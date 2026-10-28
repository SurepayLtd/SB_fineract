package org.apache.fineract.portfolio.crb.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResultBuilder;
import org.apache.fineract.infrastructure.core.exception.GeneralPlatformDomainRuleException;
import org.apache.fineract.infrastructure.core.service.DateUtils;
import org.apache.fineract.portfolio.client.domain.CRBStatus;
import org.apache.fineract.portfolio.client.domain.Client;
import org.apache.fineract.portfolio.client.domain.ClientRepositoryWrapper;
import org.apache.fineract.portfolio.crb.data.*;
import org.apache.fineract.portfolio.crb.domain.*;
import org.apache.fineract.portfolio.crb.serializer.CrbDataValidator;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;


@Service
@RequiredArgsConstructor
@Slf4j
public class CrbCheckWritePlatformServiceImpl implements CrbCheckWritePlatformService {

    private final ClientRepositoryWrapper clientRepositoryWrapper;
    private final GnugridNotificationWritePlatformService gnugridNotificationWritePlatformService;
    private final CrbCheckRepository crbCheckRepository;
    private final ObjectMapper objectMapper;
    private final CrbConsentRepository crbConsentRepository;
    private final CrbDataValidator fromApiJsonDeserializer;

    @Transactional
    @Override
    public CommandProcessingResult crbCheck(Long clientId, JsonCommand command) {
        final Client client = this.clientRepositoryWrapper.findOneWithNotFoundDetection(clientId);

        this.fromApiJsonDeserializer.validateCrbCheck(command);

        Integer type = command.integerValueOfParameterNamed("crbValidationType");

        Integer request = command.integerValueOfParameterNamed("crbRequestType");

        final CrbConsent crbConsent = this.crbConsentRepository.findVerifiedClient(client.getId(), ConsentStatus.VERIFIED.getValue());

        if (crbConsent == null){
            throw new GeneralPlatformDomainRuleException("error.msg.client.crb.consent",
                    "Client has not consented to CRB verification."
            );
        }

        if (request.equals(CrbRequestType.INITIAL.getValue())){
           return createInitialCheck(client, type, crbConsent);
        }else {
            return reCheck(client, type, crbConsent);
        }

    }


    private CommandProcessingResult createInitialCheck(Client client, Integer type, CrbConsent crbConsent) {



        final CrbCheck crbCheck = CrbCheck.create(client, crbConsent, type, CrbRequestType.INITIAL.getValue());

        this.crbCheckRepository.saveAndFlush(crbCheck);

        try{
            CrbValidationType type1 = CrbValidationType.fromInt(type);

            var result = validate(type1, client.getId());

            int status = result.isVerified()
                    ? CRBStatus.VERIFIED.getValue()
                    : CRBStatus.FAILED.getValue();

            LocalDateTime completedAt = DateUtils.getLocalDateTimeOfTenant();

            crbCheck.complete(status, result.getValidationId(), result.getReference(), result.getCost(), result.getCurrency(), result.getResponsePayload(), result.getMessage(), null, completedAt);

            this.crbCheckRepository.saveAndFlush(crbCheck);

            if (result.isVerified() && client.isCrbPending() ) {
                client.markCrbVerified(DateUtils.getLocalDateTimeOfTenant());
            }

            return new CommandProcessingResultBuilder()
                    .withEntityId(crbCheck.getId())
                    .withClientId(client.getId())
                    .build();

        } catch (Exception e) {
            crbCheck.setStatus(CRBStatus.ERROR.getValue());
            crbCheck.setErrorMessage(e.getMessage());
            crbCheck.setCompletedAt(DateUtils.getLocalDateTimeOfTenant());

            this.crbCheckRepository.saveAndFlush(crbCheck);

            throw e;
        }


    }


    private CommandProcessingResult reCheck(Client client, Integer type, CrbConsent crbConsent) {

        final CrbCheck crbCheck = CrbCheck.create(client, crbConsent, type, CrbRequestType.REVALIDATION.getValue());

        this.crbCheckRepository.saveAndFlush(crbCheck);

        try{

            CrbValidationType type1 = CrbValidationType.fromInt(type);

            var result = revalidate(type1, client.getId());

            int status = result.isVerified()
                    ? CRBStatus.VERIFIED.getValue()
                    : CRBStatus.FAILED.getValue();

            LocalDateTime completedAt = DateUtils.getLocalDateTimeOfTenant();

            crbCheck.complete(status, null, null, result.getCost(), result.getCurrency(), result.getResponsePayload(), result.getMessage(), null, completedAt);

            this.crbCheckRepository.saveAndFlush(crbCheck);

            if (result.isVerified() && client.isCrbPending() ) {
                client.markCrbVerified(DateUtils.getLocalDateTimeOfTenant());
            }

            return new CommandProcessingResultBuilder()
                    .withEntityId(crbConsent.getId())
                    .withClientId(client.getId())
                    .build();

        } catch (Exception e) {
            crbCheck.setStatus(CRBStatus.ERROR.getValue());
            crbCheck.setErrorMessage(e.getMessage());
            crbCheck.setCompletedAt(DateUtils.getLocalDateTimeOfTenant());

            this.crbCheckRepository.saveAndFlush(crbCheck);

            throw e;
        }

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

        final String nationalId = client.getNationalId();
        final String surname = client.getLastname();
        final String givenName = client.getFirstname();
        final String otherName = client.getMiddlename() != null ? client.getMiddlename() : null;
        final LocalDate dateOfBirth = client.dateOfBirth();
        final String cardNumber = getString(client, nationalId);

        final boolean hasSurname = surname != null && !surname.isBlank();
        final boolean hasGivenName = givenName != null && !givenName.isBlank();
        final boolean hasOtherName = otherName != null && !otherName.isBlank();
        final boolean hasDateOfBirth = dateOfBirth != null;

        if (!hasSurname && !hasGivenName && !hasOtherName && !hasDateOfBirth) {
            throw new GeneralPlatformDomainRuleException("error.msg.crb.personal.information.required",
                    "At least one of surname, given name, other name, or date of birth must be supplied.");
        }

        var requestData = new ValidatePersonalInformationRequestData(
                nationalId, surname, givenName, otherName, dateOfBirth != null ? dateOfBirth.toString() : null, Integer.valueOf(cardNumber)
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

    @NotNull
    private static String getString(Client client, String nationalId) {

        final String cardNumber = client.getCardNumber();

        if (nationalId == null || nationalId.isBlank()) {
            throw new GeneralPlatformDomainRuleException("error.msg.crb.nin.required",
                    "National identification number (NIN) is required for CRB validation.");
        }

        if (cardNumber == null || cardNumber.isBlank()) {
            throw new GeneralPlatformDomainRuleException("error.msg.client.card.number.required",
                    "Card number is required for CRB validation.");
        }
        return cardNumber;
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

        CrbCheck crbCheck = this.crbCheckRepository.findHistoryWithProviderId(clientId, CrbValidationType.DETAILS.getValue(), CrbRequestType.INITIAL.getValue())
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


        CrbCheck crbCheck = this.crbCheckRepository.findHistoryWithProviderId(clientId, CrbValidationType.NIN.getValue(), CrbRequestType.INITIAL.getValue())
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

        CrbCheck crbCheck = this.crbCheckRepository.findHistoryWithProviderId(clientId, CrbValidationType.PHONE.getValue(), CrbRequestType.INITIAL.getValue())
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
