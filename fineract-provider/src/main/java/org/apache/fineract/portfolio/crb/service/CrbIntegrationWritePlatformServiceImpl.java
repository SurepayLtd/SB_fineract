package org.apache.fineract.portfolio.crb.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.infrastructure.core.exception.GeneralPlatformDomainRuleException;
import org.apache.fineract.infrastructure.core.service.DateUtils;
import org.apache.fineract.portfolio.client.domain.CRBStatus;
import org.apache.fineract.portfolio.client.domain.Client;
import org.apache.fineract.portfolio.client.domain.ClientRepositoryWrapper;
import org.apache.fineract.portfolio.crb.data.*;
import org.apache.fineract.portfolio.crb.domain.CrbCheck;
import org.apache.fineract.portfolio.crb.domain.CrbCheckRepository;
import org.apache.fineract.portfolio.crb.domain.CrbRequestType;
import org.apache.fineract.portfolio.crb.domain.CrbValidationType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;


@Service
@RequiredArgsConstructor
@Slf4j
public class CrbIntegrationWritePlatformServiceImpl implements CrbIntegrationWritePlatformService{

    private final ClientRepositoryWrapper clientRepositoryWrapper;
    private final GnugridNotificationWritePlatformService gnugridNotificationWritePlatformService;
    private final CrbCheckRepository crbCheckRepository;
    private final ObjectMapper objectMapper;


    @Transactional
    @Override
    public void createInitialCheck(CrbValidationType type, Long clientId) {
        final Client client = this.clientRepositoryWrapper.findOneWithNotFoundDetection(clientId);

        final CrbCheck crbCheck = CrbCheck.create(client, type.getValue(), CrbRequestType.INITIAL.getValue());

        this.crbCheckRepository.saveAndFlush(crbCheck);

        try{

            var result = validate(type, client.getId());

            int status = result.isVerified()
                    ? CRBStatus.VERIFIED.getValue()
                    : CRBStatus.FAILED.getValue();

            LocalDateTime completedAt = DateUtils.getLocalDateTimeOfTenant();

            crbCheck.complete(status, result.getValidationId(), result.getReference(), result.getCost(), result.getCurrency(), result.getResponsePayload(), result.getMessage(), null, completedAt);

            this.crbCheckRepository.saveAndFlush(crbCheck);

            if (result.isVerified() && client.isCrbPending() ) {
                client.markCrbVerified(DateUtils.getLocalDateTimeOfTenant());
                this.clientRepositoryWrapper.saveAndFlush(client);
            }


        } catch (Exception e) {
            crbCheck.setStatus(CRBStatus.ERROR.getValue());
            crbCheck.setErrorMessage(e.getMessage());
            crbCheck.setCompletedAt(DateUtils.getLocalDateTimeOfTenant());

            this.crbCheckRepository.saveAndFlush(crbCheck);

            throw e;
        }

    }

    @Transactional
    @Override
    public void reCheck(CrbValidationType type, Long clientId) {
        final Client client = this.clientRepositoryWrapper.findOneWithNotFoundDetection(clientId);

        var result = revalidate(type, client.getId());

    }



    private CrbProviderResult validate(CrbValidationType type, Long clientId) {

        return switch (type) {
            case DETAILS -> validateDetails(clientId);
            case NIN -> validateNin(clientId);
            case PHONE -> validatePhone(clientId);
        };
    }

    private CrbProviderResult revalidate(CrbValidationType type, Long clientId) {

        return switch (type) {
            case DETAILS -> reValidateDetails(clientId);
            case NIN -> reValidateNin(clientId);
            case PHONE -> reValidatePhone(clientId);
        };
    }

    private CrbProviderResult validateDetails(Long clientId){

        Client client = this.clientRepositoryWrapper.findOneWithNotFoundDetection(clientId);

        var requestData = new ValidatePersonalInformationRequestData(
                client.getNationalId(), client.getLastname(), client.getFirstname(), client.getMiddlename(), client.dateOfBirth().toString(), Integer.valueOf(client.getCardNumber())
        );

        var responseData = this.gnugridNotificationWritePlatformService.validatePersonalInfo(requestData);

        final ValidatePersonalInformationResponseData.ValidationData validation = responseData.getValidation();

        final boolean verified = "SUCCESSFUL".equalsIgnoreCase(validation.getStatus())
                        && "FOUND".equalsIgnoreCase(validation.getNin_status())
                        && "YES".equalsIgnoreCase(validation.getMatching_status())
                        && "VALID".equalsIgnoreCase(validation.getCard_status());

        return CrbProviderResult.builder()
                .verified(verified)
                .validationId(validation.getId())
                .reference(validation.getReference())
                .status(validation.getStatus())
                .message(resolveValidationMessage(validation.getError_message(), validation.getMessage()))
                .cost(parseCost(validation.getCost()))
                .currency(validation.getCurrency())
                .responsePayload(serializeResponse(responseData))
                .build();
    }

    private CrbProviderResult validateNin(Long clientId){

        Client client = this.clientRepositoryWrapper.findOneWithNotFoundDetection(clientId);

        var requestData = client.getNationalId();

        var responseData = this.gnugridNotificationWritePlatformService.validateNIN(requestData);

        final ValidateNINResponseData.ValidationData validation = responseData.getValidation();

        final boolean verified = "SUCCESSFUL".equalsIgnoreCase(validation.getStatus())
                && "VALID".equalsIgnoreCase(validation.getNin_status())
                && client.getNationalId().equalsIgnoreCase(validation.getNin())
                && client.dateOfBirth().toString().equalsIgnoreCase(validation.getDate_of_birth())
                && client.getFullname().equalsIgnoreCase(validation.getName());

        return CrbProviderResult.builder()
                .verified(verified)
                .validationId(validation.getId())
                .reference(validation.getReference())
                .status(validation.getStatus())
                .message(resolveValidationMessage(validation.getError_message(), validation.getMessage()))
                .cost(parseCost(validation.getCost()))
                .currency(validation.getCurrency())
                .responsePayload(serializeResponse(responseData))
                .build();

    }

    private CrbProviderResult validatePhone(Long clientId) {

        Client client = this.clientRepositoryWrapper.findOneWithNotFoundDetection(clientId);

        var requestData = client.getMobileNo();

        var responseData = this.gnugridNotificationWritePlatformService.validatePhone(requestData);

        final ValidatePhoneResponseData.ValidationData validation = responseData.getValidation();

        final boolean verified = "SUCCESSFUL".equalsIgnoreCase(validation.getStatus())
                && "FOUND".equalsIgnoreCase(validation.getPhone_status())
                && client.getLastname().equalsIgnoreCase(validation.getSurname());

        return CrbProviderResult.builder()
                .verified(verified)
                .validationId(validation.getId())
                .reference(validation.getReference())
                .status(validation.getStatus())
                .message(resolveValidationMessage(validation.getError_message(), validation.getMessage()))
                .cost(parseCost(validation.getCost()))
                .currency(validation.getCurrency())
                .responsePayload(serializeResponse(responseData))
                .build();

    }

    private CrbProviderResult reValidateDetails(Long clientId){

        CrbCheck crbCheck = this.crbCheckRepository.findHistoryWithProviderId(clientId, CrbValidationType.DETAILS.getValue())
                .stream()
                .findFirst()
                .orElseThrow(() -> new GeneralPlatformDomainRuleException("error.msg.crb.check.not.found",
                        "Previous CRB details validation not found."));

        var providerValidationId = crbCheck.getProviderValidationId();

        var responseData = this.gnugridNotificationWritePlatformService.reValidatePersonalInfo(providerValidationId);

        final ValidatePersonalInformationResponseData.ValidationData validation = responseData.getValidation();

        final boolean verified = "SUCCESSFUL".equalsIgnoreCase(validation.getStatus())
                && "FOUND".equalsIgnoreCase(validation.getNin_status())
                && "YES".equalsIgnoreCase(validation.getMatching_status())
                && "VALID".equalsIgnoreCase(validation.getCard_status());

        return CrbProviderResult.builder()
                .verified(verified)
                .validationId(validation.getId() != null ? validation.getId() : providerValidationId)
                .reference(validation.getReference())
                .status(validation.getStatus())
                .message(resolveValidationMessage(validation.getError_message(), validation.getMessage()))
                .cost(parseCost(validation.getCost()))
                .currency(validation.getCurrency())
                .responsePayload(serializeResponse(responseData))
                .build();
    }

    private CrbProviderResult reValidateNin(Long clientId){

        Client client = this.clientRepositoryWrapper.findOneWithNotFoundDetection(clientId);


        CrbCheck crbCheck = this.crbCheckRepository.findHistoryWithProviderId(clientId, CrbValidationType.NIN.getValue())
                .stream()
                .findFirst()
                .orElseThrow(() -> new GeneralPlatformDomainRuleException("error.msg.crb.check.not.found",
                        "Previous CRB details validation not found."));

        var providerValidationId = crbCheck.getProviderValidationId();

        var responseData = this.gnugridNotificationWritePlatformService.reValidateNIN(providerValidationId);

        final ValidateNINResponseData.ValidationData validation = responseData.getValidation();

        final boolean verified = "SUCCESSFUL".equalsIgnoreCase(validation.getStatus())
                && "VALID".equalsIgnoreCase(validation.getNin_status())
                && client.getNationalId().equalsIgnoreCase(validation.getNin())
                && client.dateOfBirth().toString().equalsIgnoreCase(validation.getDate_of_birth())
                && client.getFullname().equalsIgnoreCase(validation.getName());

        return CrbProviderResult.builder()
                .verified(verified)
                .validationId(validation.getId() != null ? validation.getId() : providerValidationId)
                .reference(validation.getReference())
                .status(validation.getStatus())
                .message(resolveValidationMessage(validation.getError_message(), validation.getMessage()))
                .cost(parseCost(validation.getCost()))
                .currency(validation.getCurrency())
                .responsePayload(serializeResponse(responseData))
                .build();
    }

    private CrbProviderResult reValidatePhone(Long clientId){

        Client client = this.clientRepositoryWrapper.findOneWithNotFoundDetection(clientId);

        CrbCheck crbCheck = this.crbCheckRepository.findHistoryWithProviderId(clientId, CrbValidationType.PHONE.getValue())
                .stream()
                .findFirst()
                .orElseThrow(() -> new GeneralPlatformDomainRuleException("error.msg.crb.check.not.found",
                        "Previous CRB details validation not found."));

        var providerValidationId = crbCheck.getProviderValidationId();

        var responseData = this.gnugridNotificationWritePlatformService.reValidatePhone(providerValidationId);

        final ValidatePhoneResponseData.ValidationData validation = responseData.getValidation();

        final boolean verified = "SUCCESSFUL".equalsIgnoreCase(validation.getStatus())
                && "FOUND".equalsIgnoreCase(validation.getPhone_status())
                && client.getLastname().equalsIgnoreCase(validation.getSurname());

        return CrbProviderResult.builder()
                .verified(verified)
                .validationId(validation.getId() != null ? validation.getId() : providerValidationId)
                .reference(validation.getReference())
                .status(validation.getStatus())
                .message(resolveValidationMessage(validation.getError_message(), validation.getMessage()))
                .cost(parseCost(validation.getCost()))
                .currency(validation.getCurrency())
                .responsePayload(serializeResponse(responseData))
                .build();
    }

    private String resolveValidationMessage(final String errorMessage, final String message) {

        if (errorMessage != null && !errorMessage.isBlank()) {
            return errorMessage;
        }

        return message;
    }

    private BigDecimal parseCost(final String cost) {

        if (cost == null || cost.isBlank()) {
            return null;
        }

        try {
            return new BigDecimal(cost);
        } catch (final NumberFormatException exception) {
            throw new GeneralPlatformDomainRuleException("error.msg.crb.validation.invalid.cost", "CRB returned an invalid validation cost.");
        }
    }

    private String serializeResponse(final Object response) {
        try {
            return this.objectMapper.writeValueAsString(response);
        } catch (final JsonProcessingException exception) {
            throw new GeneralPlatformDomainRuleException("error.msg.crb.validation.response.serialization.failed",
                    "Unable to serialize the CRB validation response.");
        }
    }


}
