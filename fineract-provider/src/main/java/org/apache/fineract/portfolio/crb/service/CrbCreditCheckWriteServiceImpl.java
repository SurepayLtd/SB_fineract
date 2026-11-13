package org.apache.fineract.portfolio.crb.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.infrastructure.codes.domain.CodeValue;
import org.apache.fineract.infrastructure.codes.domain.CodeValueRepositoryWrapper;
import org.apache.fineract.infrastructure.configuration.domain.ConfigurationDomainService;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResultBuilder;
import org.apache.fineract.infrastructure.core.exception.GeneralPlatformDomainRuleException;
import org.apache.fineract.infrastructure.core.service.DateUtils;
import org.apache.fineract.infrastructure.creditbureau.domain.CreditBureauLoanProductMapping;
import org.apache.fineract.infrastructure.creditbureau.domain.CreditBureauLoanProductMappingRepository;
import org.apache.fineract.portfolio.client.domain.*;
import org.apache.fineract.portfolio.crb.data.*;
import org.apache.fineract.portfolio.crb.domain.*;
import org.apache.fineract.portfolio.crb.serializer.CrbCheckReasons;
import org.apache.fineract.portfolio.crb.serializer.CrbDataValidator;
import org.apache.fineract.portfolio.loanaccount.domain.Loan;
import org.apache.fineract.portfolio.loanaccount.domain.LoanRepositoryWrapper;
import org.apache.fineract.portfolio.loanproduct.domain.LoanProduct;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;

import java.time.LocalDateTime;
import java.util.List;


@Service
@RequiredArgsConstructor
@Slf4j
public class CrbCreditCheckWriteServiceImpl implements CrbCreditCheckWriteService{

    private final ClientRepositoryWrapper clientRepositoryWrapper;
    private final ClientNonPersonRepositoryWrapper clientNonPersonRepositoryWrapper;
    private final GnugridNotificationWritePlatformService gnugridNotificationWritePlatformService;
    private final CrbCheckRepository crbCheckRepository;
    private final ObjectMapper objectMapper;
    private final CrbConsentRepository crbConsentRepository;
    private final CrbDataValidator fromApiJsonDeserializer;
    private final static String CONSENTED = "Yes";
    private final CodeValueRepositoryWrapper codeValueRepository;
    private final LoanRepositoryWrapper loanRepositoryWrapper;
    private final CreditBureauLoanProductMappingRepository creditBureauLoanProductMappingRepository;
    private final ConfigurationDomainService configurationDomainService;



    @Transactional
    @Override
    public CommandProcessingResult kycCheck(Long clientId, JsonCommand command) {

        boolean isCrbEnabled = this.configurationDomainService.isCRBEnabled();

        if (!isCrbEnabled){
            throw new GeneralPlatformDomainRuleException("error.msg.crb.not.enabled", "CRB not enabled for this Tenant.");
        }
        Client client = this.clientRepositoryWrapper.findOneWithNotFoundDetection(clientId);

        if (!client.isActive() && !client.isPending()) {
            throw new GeneralPlatformDomainRuleException("error.msg.crb.client.inactive", "Kyc Check Score enquiry cannot be performed because the client is inactive.");
        }

        this.fromApiJsonDeserializer.validateCrbCredit(command);

        final Long reasonId = command.longValueOfParameterNamed(CrbApiConstants.enquiryReasonIdParamName);
        Integer type = command.integerValueOfParameterNamed(CrbApiConstants.crbCreditFormatParamName);


        final CrbConsent crbConsent = this.crbConsentRepository.findVerifiedClient(client.getId(), ConsentStatus.VERIFIED.getValue());

        CodeValue value = this.codeValueRepository.findOneByCodeNameAndIdWithNotFoundDetection(CrbApiConstants.CRB_ENQUIRY_REASON, reasonId);
        String reason = value.getLabel();

        String clientConsented;

        if (crbConsent == null){
            throw new GeneralPlatformDomainRuleException("error.msg.client.crb.consent", "Client has not consented to CRB verification.");
        }else {
            clientConsented = CONSENTED;
        }
        return creditChecks(client, null, crbConsent, type, clientConsented, reason, null, null);
    }

    @Transactional
    @Override
    public CommandProcessingResult crbCredit(Long loanId, JsonCommand command) {

        boolean isCrbEnabled = this.configurationDomainService.isCRBEnabled();

        if (!isCrbEnabled){
            throw new GeneralPlatformDomainRuleException("error.msg.crb.not.enabled",
                    "CRB not enabled for this Tenant."
            );
        }

        final Loan loan = this.loanRepositoryWrapper.findOneWithNotFoundDetection(loanId);

        if (!loan.isSubmittedAndPendingApproval()){
            throw new GeneralPlatformDomainRuleException("error.msg.crb.loan.not.pending", "Credit Enquiry, Score  cannot be performed because the loan is not pending.");
        }

        final LoanProduct loanProduct = loan.loanProduct();

        final CreditBureauLoanProductMapping mapping = this.creditBureauLoanProductMappingRepository.findOneByLoanProduct(loanProduct);

        if (mapping == null || !mapping.isActive()) {
            throw new GeneralPlatformDomainRuleException("error.msg.credit.bureau.loan.product.mapping.not.found",
                    "An active credit bureau mapping is required for this loan product."
            );
        }

        final Client client = loan.getClient();

        this.fromApiJsonDeserializer.validateCrbCredit(command);

        Integer type = command.integerValueOfParameterNamed(CrbApiConstants.crbCreditFormatParamName);
        Integer scoreType = command.integerValueOfParameterNamed(CrbApiConstants.crbCreditScoreTypeActionFormatParamName);
        final Long reasonId = command.longValueOfParameterNamed(CrbApiConstants.enquiryReasonIdParamName);

        String reason = null;
        CodeValue value;
        if (reasonId != null) {
           value = this.codeValueRepository.findOneByCodeNameAndIdWithNotFoundDetection(CrbApiConstants.CRB_ENQUIRY_REASON, reasonId);
           reason = value.getLabel();
        }


        final CrbConsent crbConsent = this.crbConsentRepository.findVerifiedClient(client.getId(), ConsentStatus.VERIFIED.getValue());

        String clientConsented;

        if (crbConsent == null){
            throw new GeneralPlatformDomainRuleException("error.msg.client.crb.consent", "Client has not consented to CRB verification.");
        }else {
            clientConsented = CONSENTED;
        }

        return creditChecks(client, loan, crbConsent, type, clientConsented, reason, scoreType, mapping);
    }


    private CommandProcessingResult creditChecks(Client client, Loan loan, CrbConsent consent, Integer type, String clientConsented, String reason, Integer scoreType, CreditBureauLoanProductMapping mapping) {

        final CreditValidationType validationType = CreditValidationType.fromInt(type);

        // Reuse a previous successful credit check when still valid.
        if (validationType.equals(CreditValidationType.CREDIT_ENQUIRY) || validationType.equals(CreditValidationType.CREDIT_SCORE)) {

            final CrbCheck reusableCheck = findReusableCheck(client.getId(), type, mapping.getStalePeriod());

            if (reusableCheck != null) {
                return new CommandProcessingResultBuilder()
                        .withEntityId(reusableCheck.getId())
                        .withClientId(client.getId())
                        .build();
            }
        }

        final CrbCheck crbCheck = CrbCheck.create(client, loan, consent, type, CrbRequestType.INITIAL.getValue());

        this.crbCheckRepository.saveAndFlush(crbCheck);

        try{

            CreditValidationType creditValidationType = CreditValidationType.fromInt(type);

            var check = checks(creditValidationType, client, clientConsented, reason, scoreType);

            var individualId = check != null ? check.getIndividualId() : null;
            var nonIndividualId = check != null ? check.getNonIndividualId() : null;

            assert check != null;

            int status = check.isVerified() ? CRBStatus.VERIFIED.getValue() : CRBStatus.FAILED.getValue();

            LocalDateTime completedAt = DateUtils.getLocalDateTimeOfTenant();

            crbCheck.creditCheck(status, check.getValidationId(), check.getReference(), check.getCost(), check.getResponsePayload(), completedAt, individualId, nonIndividualId, null, null);

            this.crbCheckRepository.saveAndFlush(crbCheck);

            return new CommandProcessingResultBuilder()
                    .withEntityId(crbCheck.getId())
                    .withClientId(client.getId())
                    .build();

        } catch (Exception e) {
            crbCheck.setStatus(CRBStatus.ERROR.getValue());
            crbCheck.setErrorMessage(e.getMessage());
            crbCheck.setCompletedAt(DateUtils.getLocalDateTimeOfTenant());

            this.crbCheckRepository.saveAndFlush(crbCheck);

            if (isProviderFailure(e) && mapping.isSkipCreditCheckInFailure()) {
                return new CommandProcessingResultBuilder()
                        .withEntityId(crbCheck.getId())
                        .withClientId(client.getId())
                        .build();
            }

            throw e;
        }
    }



    private CrbProviderResult checks(CreditValidationType type, Client client, String consented, String reason, Integer scoreType) {

        return switch (type) {
            case KYC_ENQUIRY -> validateKyc(client, consented, reason);
            case CREDIT_ENQUIRY -> validateCreditEnquiry(client, consented, reason);
            case CREDIT_SCORE -> validateCreditScoreEnquiry(client, consented, scoreType);
        };
    }

    private CrbProviderResult validateCreditScoreEnquiry(final Client client, final String consented, final Integer scoreType) {

        final Integer entity = entityType(client);
        final String identificationType = identificationType(client);
        final String identifier = identifier(client);

        if (identifier == null || identifier.isBlank()) {
            throw new GeneralPlatformDomainRuleException("error.msg.crb.identifier.not.found",
                    "Client identification number is required for the credit score enquiry."
            );
        }

        if (!client.isActive()) {
            throw new GeneralPlatformDomainRuleException("error.msg.crb.client.inactive", "CRB Credit Score enquiry cannot be performed because the client is inactive.");
        }

        final LegalForm legalForm = LegalForm.fromInt(client.getLegalForm());

        if (legalForm == null) {
            throw new GeneralPlatformDomainRuleException("error.msg.crb.legal.form.invalid", "Invalid client legal form.");
        }

        String entityCategory = null;
        String phone = null;

        if (legalForm.isEntity()) {
            ClientNonPerson clientNonPerson = clientNonPersonRepositoryWrapper.findOneByClientIdWithNotFoundDetection(client.getId());

            CodeValue mainBusinessLine = clientNonPerson.getMainBusinessLine();

            if (mainBusinessLine == null || mainBusinessLine.getLabel() == null || mainBusinessLine.getLabel().isBlank()) {
                throw new GeneralPlatformDomainRuleException("error.msg.crb.entity.category.required",
                        "Main business line is required for a non-individual credit score enquiry."
                );
            }

            entityCategory = mainBusinessLine.getLabel();
        }

        final CreditScore creditScore = CreditScore.fromInt(scoreType);

        if (CreditScore.MNO.getValue().equals(scoreType) || CreditScore.CRB_MNO.getValue().equals(scoreType) || CreditScore.CRB.getValue().equals(scoreType)) {

            phone = client.mobileNo();

            if (phone == null || phone.isBlank()) {
                throw new GeneralPlatformDomainRuleException("error.msg.crb.phone.not.found",
                        "Client phone number is required for an MNO credit score enquiry."
                );
            }
        }

        final String type = creditScore.getCode();

        var request = new CreditScoreRequestData(identifier, entity, entityCategory, identificationType, phone, consented, type, null);

        final CreditScoreResponseData response = gnugridNotificationWritePlatformService.creditScore(request);

        if (response == null || response.getData() == null || response.getData().getEnquiry() == null) {

            throw new GeneralPlatformDomainRuleException("error.msg.crb.credit.score.invalid.response",
                    "Invalid credit score response received from CRB."
            );
        }

        final var enquiry = response.getData().getEnquiry();

        final boolean verified = identifier.equals(enquiry.getIdentifier());

        return CrbProviderResult.builder()
                .verified(verified)
                .validationId(enquiry.getId())
                .cost(response.getData().getCost().getAmount())
                .reference(enquiry.getReference())
                .responsePayload(serializeResponse(response))
                .build();
    }

    private CrbProviderResult validateCreditEnquiry(Client client, String consented, String reason){

        Integer entity = entityType(client);
        String identificationType = identificationType(client);
        String identifier = identifier(client);

        if (!client.isActive()) {
            throw new GeneralPlatformDomainRuleException("error.msg.crb.client.inactive", "CRB enquiry cannot be performed because the client is inactive.");
        }

        String format = CrbCheckReasons.CREDIT_ENQUIRY_FORMAT;

        var request = new CreditEnquiryRequestData(identifier, identificationType, reason, format, consented, null, entity);

        var response = gnugridNotificationWritePlatformService.creditEnquiry(request);

        final boolean verified = "SUCCESSFUL".equalsIgnoreCase(response.getEnquiry().getStatus())
                && "1".equalsIgnoreCase(response.getEnquiry().getFormal().getMatches().toString());

        var enquiry = response.getEnquiry();
        var formal = enquiry.getFormal();



        var builder = CrbProviderResult.builder()
                .verified(verified)
                .validationId(enquiry.getId())
                .cost(enquiry.getCost())
                .reference(enquiry.getReference())
                .status(enquiry.getStatus())
                .responsePayload(serializeResponse(response));

        if (formal != null) {
            if (formal.getIndividuals() != null && !formal.getIndividuals().isEmpty()) {
                builder.individualId(formal.getIndividuals().getFirst().getIndividual_id());
            } else if (formal.getNonindividuals() != null && !formal.getNonindividuals().isEmpty()) {
                builder.nonIndividualId(formal.getNonindividuals().getFirst().getNon_individual_id());
            }
        }
        return builder.build();
    }

    private CrbProviderResult validateKyc(Client client, String consented, String reason){

        Integer entity = entityType(client);
        String identificationType = identificationType(client);
        String identifier = identifier(client);

        var request = new KycEnquiryRequestData(identifier, identificationType, reason, consented, null, entity);

        var response = gnugridNotificationWritePlatformService.validateKyc(request);

        final boolean verified = "SUCCESSFUL".equalsIgnoreCase(response.getEnquiry().getStatus())
                && "1".equalsIgnoreCase(response.getEnquiry().getFormal().getMatches().toString());

        var enquiry = response.getEnquiry();

        var formal = enquiry.getFormal();


        var builder = CrbProviderResult.builder()
                .verified(verified)
                .validationId(enquiry.getId())
                .cost(enquiry.getCost())
                .reference(enquiry.getReference())
                .status(enquiry.getStatus())
                .responsePayload(serializeResponse(response));
        if (formal != null) {
            if (formal.getIndividuals() != null && !formal.getIndividuals().isEmpty()) {
                builder.individualId(formal.getIndividuals().getFirst().getIndividual_id());
            } else if (formal.getNonindividuals() != null && !formal.getNonindividuals().isEmpty()) {
                builder.nonIndividualId(formal.getNonindividuals().getFirst().getNon_individual_id());
            }
        }

        return  builder.build();
    }


    private Integer entityType(Client client) {
        LegalForm legalForm = LegalForm.fromInt(client.getLegalForm());

        if (legalForm == null) {
            return null;
        }

        return switch (legalForm) {
            case PERSON -> CreditEntityType.INDIVIDUAL.getValue();
            case ENTITY -> CreditEntityType.NON_INDIVIDUAL.getValue();
        };
    }

    private String identificationType(Client client){
        LegalForm legalForm = LegalForm.fromInt(client.getLegalForm());

        if (legalForm == null) {
            return null;
        }

        return switch (legalForm){
            case ENTITY -> CrbIdentificationType.Registration_Certificate_Number.getRequestValue();
            case PERSON -> CrbIdentificationType.Country_ID_Number.getRequestValue();
        };
    }

    private String identifier(final Client client) {
        final LegalForm legalForm = LegalForm.fromInt(client.getLegalForm());

        if (legalForm == null) {
            return null;
        }

        if (legalForm.isEntity()) {
            final ClientNonPerson clientNonPerson = clientNonPersonRepositoryWrapper.findOneByClientId(client.getId());

            return clientNonPerson != null ? clientNonPerson.getIncorpNumber() : null;
        }

        return client.getNationalId();
    }

    private String serializeResponse(final Object response) {
        try {
            return this.objectMapper.writeValueAsString(response);
        } catch (final JsonProcessingException exception) {
            throw new GeneralPlatformDomainRuleException("error.msg.crb.validation.response.serialization.failed",
                    "Unable to serialize the CRB validation response.");
        }
    }

    private CrbCheck findReusableCheck(final Long clientId, final Integer validationType, final int stalePeriod) {

        if (stalePeriod <= 0) {
            return null;
        }

        final List<CrbCheck> checks = this.crbCheckRepository.findLatestVerifiedCheck(
                        clientId,
                        validationType,
                        CRBStatus.VERIFIED.getValue(),
                        PageRequest.of(0, 1));

        if (checks.isEmpty()) {
            return null;
        }

        final CrbCheck latestCheck = checks.get(0);

        final LocalDateTime expiryDate = latestCheck.getCreatedAt().plusDays(stalePeriod);

        if (expiryDate.isBefore(LocalDateTime.now())) {
            return null;
        }

        return latestCheck;
    }

    private boolean isProviderFailure(final Exception exception) {

        return exception instanceof RestClientException;
    }

}
