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
import org.apache.fineract.portfolio.client.domain.*;
import org.apache.fineract.portfolio.crb.data.*;
import org.apache.fineract.portfolio.crb.domain.*;
import org.apache.fineract.portfolio.crb.serializer.CrbCheckReasons;
import org.apache.fineract.portfolio.crb.serializer.CrbDataValidator;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;


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
    private final Environment environment;
    private static final ZoneId TIME_ZONE = ZoneId.of("Africa/Kampala");


    @Transactional
    @Override
    public CommandProcessingResult crbCredit(Long clientId, JsonCommand command) {
        final Client client = this.clientRepositoryWrapper.findOneWithNotFoundDetection(clientId);

        this.fromApiJsonDeserializer.validateCrbCredit(command);

        Integer type = command.integerValueOfParameterNamed(CrbApiConstants.crbCreditFormatParamName);
        Integer report = command.integerValueOfParameterNamed(CrbApiConstants.crbReportTypeParamName);
        Integer reportFormat = command.integerValueOfParameterNamed(CrbApiConstants.crbReportFormatParamName);
        Integer reportAction = command.integerValueOfParameterNamed(CrbApiConstants.crbReportActionFormatParamName);



        final CrbConsent crbConsent = this.crbConsentRepository.findVerifiedClient(client.getId(), ConsentStatus.VERIFIED.getValue());

        String clientConsented = null;

        if (crbConsent == null){
            throw new GeneralPlatformDomainRuleException("error.msg.client.crb.consent", "Client has not consented to CRB verification.");
        }else {
            clientConsented = CONSENTED;
        }

        if (report != null || reportFormat != null || reportAction != null){
            return creditReports(client, crbConsent, report, reportFormat, reportAction);
        }
        return creditChecks(client, crbConsent, type, clientConsented);
    }


    private CommandProcessingResult creditChecks(Client client, CrbConsent consent, Integer type, String clientConsented) {

        final CrbCheck crbCheck = CrbCheck.create(client, consent, type, CrbRequestType.INITIAL.getValue());

        this.crbCheckRepository.saveAndFlush(crbCheck);

        try{

            CreditValidationType creditValidationType = CreditValidationType.fromInt(type);

            var check = checks(creditValidationType, client, clientConsented);

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

            throw e;
        }
    }

    private CommandProcessingResult creditReports(Client client, CrbConsent consent, Integer reportType, Integer reportFormat, Integer reportAction) {

        final CrbCheck crbCheck = CrbCheck.create(client, consent, reportType, CrbRequestType.INITIAL.getValue());

        this.crbCheckRepository.saveAndFlush(crbCheck);

        try{

            CreditReports creditReports = CreditReports.fromInt(reportType);

            var check = report(creditReports, client, reportFormat, reportAction);

            var reportToken = check != null ? check.getReportToken() : null;

            var reportAccountId = check != null ? check.getReportAccountId() : null;


            assert check != null;

            int status = check.isVerified() ? CRBStatus.VERIFIED.getValue() : CRBStatus.FAILED.getValue();

            LocalDateTime completedAt = DateUtils.getLocalDateTimeOfTenant();

            crbCheck.creditCheck(status, null, null, null, check.getResponsePayload(), completedAt, null, null, reportToken, reportAccountId);

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

            throw e;
        }
    }



    private CrbProviderResult checks(CreditValidationType type, Client client, String consented) {

        return switch (type) {
            case KYC_ENQUIRY -> validateKyc(client, consented);
            case CREDIT_ENQUIRY -> validateCreditEnquiry(client, consented);
            case CREDIT_SCORE -> validateKyc(client, consented);
        };
    }

    private CrbProviderResult report(CreditReports reports, Client client, Integer reportFormat, Integer reportAction) {

        return switch (reports) {
            case KYC_REPORT -> kycReport(client, reportFormat);
            case CREDIT_REPORT -> creditReport(client, reportFormat, reportAction);
        };
    }

    private CrbProviderResult validateCreditEnquiry(Client client, String consented){

        Integer entity = entityType(client);
        String identificationType = identificationType(client);
        String identifier = identifier(client);

        String format = CrbCheckReasons.CREDIT_ENQUIRY_FORMAT;

        var reason = CrbCheckReasons.CREDIT_ENQUIRY_REASON;

        var request = new CreditEnquiryRequestData(identifier, identificationType, format, reason, consented, null, entity);

        var response = gnugridNotificationWritePlatformService.creditEnquiry(request);

        final boolean verified = "SUCCESSFUL".equalsIgnoreCase(response.getEnquiry().getStatus())
                && "1".equalsIgnoreCase(response.getEnquiry().getFormal().getMatches().toString());

        var enquiry = response.getEnquiry();


        return CrbProviderResult.builder()
                .verified(verified)
                .validationId(enquiry.getId())
                .cost(enquiry.getCost())
                .reference(enquiry.getReference())
                .status(enquiry.getStatus())
                .responsePayload(serializeResponse(response))
                .individualId(enquiry.getFormal().getIndividuals().getFirst().getIndividual_id())
                .nonIndividualId(enquiry.getFormal().getNonindividuals().getFirst().getNon_individual_id())
                .build();
    }

    private CrbProviderResult validateKyc(Client client, String consented){

        Integer entity = entityType(client);
        String identificationType = identificationType(client);
        String identifier = identifier(client);

        var reason = CrbCheckReasons.KYC_ENQUIRY_REASON;

        var request = new KycEnquiryRequestData(identifier, identificationType, reason, consented, null, entity);

        var response = gnugridNotificationWritePlatformService.validateKyc(request);

        final boolean verified = "SUCCESSFUL".equalsIgnoreCase(response.getEnquiry().getStatus())
                && "1".equalsIgnoreCase(response.getEnquiry().getFormal().getMatches().toString());

        var enquiry = response.getEnquiry();


        return CrbProviderResult.builder()
                .verified(verified)
                .validationId(enquiry.getId())
                .cost(enquiry.getCost())
                .reference(enquiry.getReference())
                .status(enquiry.getStatus())
                .responsePayload(serializeResponse(response))
                .individualId(enquiry.getFormal().getIndividuals().getFirst().getIndividual_id())
                .nonIndividualId(enquiry.getFormal().getNonindividuals().getFirst().getNon_individual_id())
                .build();
    }

    private CrbProviderResult kycReport(Client client, Integer reportFormat){

        CrbReportFormat crbReportFormat = CrbReportFormat.fromInt(reportFormat);

        var request = kycClientTypeReport(client, crbReportFormat);

        var response = gnugridNotificationWritePlatformService.kycReport(request);

        //Todo: What Makes the Report verified really???
        final boolean verified = true;

        return CrbProviderResult.builder()
                .verified(verified)
                .responsePayload(serializeResponse(response))
                .build();
    }

    private CrbProviderResult creditReport(final Client client, final Integer reportFormat, final Integer reportAction) {

        final CrbReportFormat crbReportFormat = CrbReportFormat.fromInt(reportFormat);
        final CrbReportAction crbReportAction = CrbReportAction.fromInt(reportAction);

        final LegalForm legalForm = LegalForm.fromInt(client.getLegalForm());

        if (legalForm == null) {
            throw new GeneralPlatformDomainRuleException("error.msg.crb.client.legal.form.invalid", "Invalid client legal form.");
        }

        final CrbCheck crbCheck = findCreditEnquiryCheck(client, CreditValidationType.CREDIT_ENQUIRY.getValue());

        final CreditReportRequestData request = creditClientTypeReport(client, crbReportFormat, crbCheck);

        if (crbReportFormat.equals(CrbReportFormat.JSON)) {

            final CreditReportJsonResponseData jsonResponseData = gnugridNotificationWritePlatformService.creditReport(request);

            //Todo: What Makes the Report verified really???
            final boolean verified = true;

            return CrbProviderResult.builder()
                    .verified(verified)
                    .responsePayload(serializeResponse(jsonResponseData))
                    .build();
        }

        final CreditReportPdfResponseData pdfResponseData = gnugridNotificationWritePlatformService.creditPdfReport(request);

        if (pdfResponseData == null || pdfResponseData.getToken() == null || pdfResponseData.getAccount_id() == null) {
            throw new GeneralPlatformDomainRuleException("error.msg.crb.credit.report.pdf.invalid.response",
                    "CRB returned an incomplete PDF report response."
            );
        }

        final String baseUrl = getProperty("crb.ui.url");

        final long timestamp = crbCheck.getCreatedAt()
                .atZone(TIME_ZONE)
                .toEpochSecond();

        final CreditReportGenerationData genData;

        if (legalForm.isEntity()) {

            genData = new CreditReportGenerationData(request.getEnquiryId(), null, request.getNon_individual_id(),
                    pdfResponseData.getAccount_id(), crbCheck.getProviderReference(), timestamp, pdfResponseData.getToken(), crbReportAction.getDescription());

        } else {

            genData = new CreditReportGenerationData(request.getEnquiryId(), request.getIndividual_id(), null,
                    pdfResponseData.getAccount_id(), crbCheck.getProviderReference(), timestamp, pdfResponseData.getToken(), crbReportAction.getDescription());
        }

        final String reportUrl = CreditReportUrlBuilder.buildReportUrl(baseUrl, genData);

        //Todo: What Makes the Report verified really???
        final boolean verified = true;

        return CrbProviderResult.builder()
                .verified(verified)
                .responsePayload(serializeResponse(pdfResponseData))
                .reportToken(pdfResponseData.getToken())
                .reportAccountId(pdfResponseData.getAccount_id())
                .reportUrl(reportUrl)
                .build();
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

    private KycReportRequestData kycClientTypeReport(Client client, CrbReportFormat crbReportFormat){
        final LegalForm legalForm = LegalForm.fromInt(client.getLegalForm());

        if (legalForm == null) {
            throw new GeneralPlatformDomainRuleException("error.msg.crb.client.legal.form.invalid", "Invalid client legal form.");
        }

        CrbCheck crbCheck = findCreditEnquiryCheck(client, CreditValidationType.KYC_ENQUIRY.getValue());

        var providerValidationId = crbCheck.getProviderValidationId();


        if (providerValidationId == null) {
            throw new GeneralPlatformDomainRuleException("error.msg.crb.validation.id.not.found", "CRB provider validation ID not found.");
        }

        if (legalForm.isEntity()){
            Integer nonIndividualId = crbCheck.getNonIndividualId();

            if (nonIndividualId == null) {
                throw new GeneralPlatformDomainRuleException("error.msg.crb.non.individual.id.not.found", "CRB non-individual ID not found.");
            }
            return new KycReportRequestData(providerValidationId, crbReportFormat.getDescription(), null, nonIndividualId);
        }

        final Integer individualId = crbCheck.getIndividualId();

        if (individualId == null) {
            throw new GeneralPlatformDomainRuleException("error.msg.crb.individual.id.not.found", "CRB individual ID not found.");
        }

        return new KycReportRequestData(providerValidationId, crbReportFormat.getDescription(),individualId, null);
    }

    private CreditReportRequestData creditClientTypeReport(final Client client, final CrbReportFormat crbReportFormat, final CrbCheck crbCheck) {

        final LegalForm legalForm = LegalForm.fromInt(client.getLegalForm());

        if (legalForm == null) {
            throw new GeneralPlatformDomainRuleException("error.msg.crb.client.legal.form.invalid", "Invalid client legal form.");
        }

        final String providerValidationId = crbCheck.getProviderValidationId();

        if (providerValidationId == null) {
            throw new GeneralPlatformDomainRuleException("error.msg.crb.validation.id.not.found", "CRB provider validation ID not found.");
        }

        if (legalForm.isEntity()) {

            final Integer nonIndividualId = crbCheck.getNonIndividualId();

            if (nonIndividualId == null) {
                throw new GeneralPlatformDomainRuleException("error.msg.crb.non.individual.id.not.found", "CRB non-individual ID not found.");
            }

            return new CreditReportRequestData(providerValidationId, null, nonIndividualId, crbReportFormat.getDescription());
        }

        final Integer individualId = crbCheck.getIndividualId();

        if (individualId == null) {
            throw new GeneralPlatformDomainRuleException("error.msg.crb.individual.id.not.found", "CRB individual ID not found.");
        }

        return new CreditReportRequestData(providerValidationId, individualId, null, crbReportFormat.getDescription());
    }

    private CrbCheck findCreditEnquiryCheck(final Client client, final Integer creditValidationType) {

        return crbCheckRepository.findHistoryWithProviderId(client.getId(), creditValidationType, CrbRequestType.INITIAL.getValue())
                .stream()
                .findFirst()
                .orElseThrow(() -> new GeneralPlatformDomainRuleException("error.msg.crb.check.not.found", "Previous CRB credit enquiry validation not found."));
    }

    private String getProperty(String property){
        return environment.getProperty(property);
    }
}
