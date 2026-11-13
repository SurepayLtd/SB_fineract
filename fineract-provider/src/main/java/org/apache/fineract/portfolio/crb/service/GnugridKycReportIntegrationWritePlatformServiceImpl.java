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
import org.apache.fineract.infrastructure.creditbureau.domain.CRBProviders;
import org.apache.fineract.portfolio.client.domain.CRBStatus;
import org.apache.fineract.portfolio.client.domain.Client;
import org.apache.fineract.portfolio.client.domain.ClientRepositoryWrapper;
import org.apache.fineract.portfolio.client.domain.LegalForm;
import org.apache.fineract.portfolio.crb.data.CrbApiConstants;
import org.apache.fineract.portfolio.crb.data.CrbProviderResult;
import org.apache.fineract.portfolio.crb.data.KycReportRequestData;

import org.apache.fineract.portfolio.crb.domain.CrbCheckRepository;
import org.apache.fineract.portfolio.crb.domain.CrbConsentRepository;
import org.apache.fineract.portfolio.crb.domain.CrbRequestType;
import org.apache.fineract.portfolio.crb.domain.ConsentStatus;
import org.apache.fineract.portfolio.crb.domain.CrbReportFormat;
import org.apache.fineract.portfolio.crb.domain.CreditValidationType;
import org.apache.fineract.portfolio.crb.domain.CreditReports;
import org.apache.fineract.portfolio.crb.domain.CrbCheck;
import org.apache.fineract.portfolio.crb.domain.CrbConsent;

import org.apache.fineract.portfolio.crb.serializer.CrbDataValidator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class GnugridKycReportIntegrationWritePlatformServiceImpl implements GnugridKycReportIntegrationWritePlatformService{

    private final ClientRepositoryWrapper clientRepositoryWrapper;
    private final CrbDataValidator fromApiJsonDeserializer;
    private final CrbConsentRepository crbConsentRepository;
    private final CrbCheckRepository crbCheckRepository;
    private final ObjectMapper objectMapper;
    private final GnugridNotificationWritePlatformService gnugridNotificationWritePlatformService;



    @Transactional
    @Override
    public CommandProcessingResult kycReports(Long clientId, JsonCommand command) {
        Client client = this.clientRepositoryWrapper.findOneWithNotFoundDetection(clientId);

        this.fromApiJsonDeserializer.validateCrbCredit(command);

        Integer report = command.integerValueOfParameterNamed(CrbApiConstants.crbReportTypeParamName);
        Integer reportFormat = command.integerValueOfParameterNamed(CrbApiConstants.crbReportFormatParamName);

        if (!client.isActive() || !client.isPending()) {
            throw new GeneralPlatformDomainRuleException("error.msg.crb.client.inactive", "Kyc Check Score enquiry cannot be performed because the client is inactive.");
        }

        final CrbConsent crbConsent = this.crbConsentRepository.findVerifiedClient(client.getId(), ConsentStatus.VERIFIED.getValue());


        final CrbCheck crbCheck = CrbCheck.create(client, null, crbConsent, report, CrbRequestType.INITIAL.getValue());

        this.crbCheckRepository.saveAndFlush(crbCheck);

        try{

            var check = kycReport(client, reportFormat);

            var reportToken = check != null ? check.getReportToken() : null;

            var reportUrl = check != null ? check.getReportUrl() : null;

            assert check != null;

            int status = check.isVerified() ? CRBStatus.VERIFIED.getValue() : CRBStatus.FAILED.getValue();

            LocalDateTime completedAt = DateUtils.getLocalDateTimeOfTenant();

            crbCheck.creditCheck(status, null, null, null, check.getResponsePayload(), completedAt, null, null, reportToken, reportUrl);

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

    @Override
    public String getKycReportPayload(Long clientId) {
        final Client client = this.clientRepositoryWrapper.findOneWithNotFoundDetection(clientId);

        // Retrieve the latest successful credit report check for this client.
        CrbCheck crbCheck = this.crbCheckRepository.findHistoryWithProviderId(client.getId(), CreditReports.CREDIT_REPORT.getValue(), CrbRequestType.INITIAL.getValue())
                .stream()
                .findFirst()
                .orElseThrow(() -> new GeneralPlatformDomainRuleException("error.msg.crb.check.not.found",
                        "Previous CRB details validation not found."));

        // PDF reports have a generated URL; JSON reports use the response payload.
        if (crbCheck.getReportUrl() != null && !crbCheck.getReportUrl().isBlank()) {
            return crbCheck.getReportUrl();
        }

        return crbCheck.getResponsePayload();
    }

    @Override
    public String getImplementationKey() {
        return CRBProviders.GNUGRID.getDescription();
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

        if (!CRBStatus.VERIFIED.getValue().equals(crbCheck.getStatus())) {
            throw new GeneralPlatformDomainRuleException("error.msg.crb.validation.not.verified",
                    "KYC report cannot be generated because the client's Kyc validation has not been verified."
            );
        }

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


    private CrbCheck findCreditEnquiryCheck(final Client client, final Integer creditValidationType) {

        return crbCheckRepository.findHistoryWithProviderId(client.getId(), creditValidationType, CrbRequestType.INITIAL.getValue())
                .stream()
                .findFirst()
                .orElseThrow(() -> new GeneralPlatformDomainRuleException("error.msg.crb.check.not.found", "Previous CRB credit enquiry validation not found."));
    }
}
