package org.apache.fineract.portfolio.crb.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.apache.fineract.infrastructure.configuration.domain.ConfigurationDomainService;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResultBuilder;
import org.apache.fineract.infrastructure.core.exception.GeneralPlatformDomainRuleException;
import org.apache.fineract.infrastructure.core.service.DateUtils;
import org.apache.fineract.infrastructure.creditbureau.domain.CRBProviders;
import org.apache.fineract.infrastructure.creditbureau.domain.CreditBureauLoanProductMapping;
import org.apache.fineract.infrastructure.creditbureau.domain.CreditBureauLoanProductMappingRepository;
import org.apache.fineract.portfolio.client.domain.CRBStatus;
import org.apache.fineract.portfolio.client.domain.Client;
import org.apache.fineract.portfolio.client.domain.LegalForm;
import org.apache.fineract.portfolio.crb.data.CrbApiConstants;
import org.apache.fineract.portfolio.crb.data.CreditReportRequestData;
import org.apache.fineract.portfolio.crb.data.CreditReportGenerationData;
import org.apache.fineract.portfolio.crb.data.CreditReportJsonResponseData;
import org.apache.fineract.portfolio.crb.data.CreditReportPdfResponseData;
import org.apache.fineract.portfolio.crb.data.CrbProviderResult;
import org.apache.fineract.portfolio.crb.data.CreditReportUrlBuilder;
import org.apache.fineract.portfolio.crb.domain.CrbCheckRepository;
import org.apache.fineract.portfolio.crb.domain.CreditReports;
import org.apache.fineract.portfolio.crb.domain.CrbReportAction;
import org.apache.fineract.portfolio.crb.domain.ConsentStatus;
import org.apache.fineract.portfolio.crb.domain.CreditValidationType;
import org.apache.fineract.portfolio.crb.domain.CrbReportFormat;
import org.apache.fineract.portfolio.crb.domain.CrbRequestType;
import org.apache.fineract.portfolio.crb.domain.CrbCheck;
import org.apache.fineract.portfolio.crb.domain.CrbConsent;
import org.apache.fineract.portfolio.crb.domain.CrbConsentRepository;

import org.apache.fineract.portfolio.crb.serializer.CrbDataValidator;
import org.apache.fineract.portfolio.loanaccount.domain.Loan;
import org.apache.fineract.portfolio.loanaccount.domain.LoanRepositoryWrapper;
import org.apache.fineract.portfolio.loanproduct.domain.LoanProduct;
import org.springframework.core.env.Environment;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GnugridCreditReportIntegrationWritePlatformServiceImpl implements GnugridCreditReportIntegrationWritePlatformService{

    private final GnugridNotificationWritePlatformService gnugridService;
    private final CrbCheckRepository crbCheckRepository;
    private final ObjectMapper objectMapper;
    private final Environment environment;
    private static final ZoneId TIME_ZONE = ZoneId.of("Africa/Kampala");
    private final CrbDataValidator fromApiJsonDeserializer;
    private final LoanRepositoryWrapper loanRepositoryWrapper;
    private final CrbConsentRepository crbConsentRepository;
    private final CreditBureauLoanProductMappingRepository creditBureauLoanProductMappingRepository;
    private final ConfigurationDomainService configurationDomainService;


    @Transactional
    @Override
    public CommandProcessingResult creditReports(Long loanId, JsonCommand command) {

        boolean isCrbEnabled = this.configurationDomainService.isCRBEnabled();

        if (!isCrbEnabled){
            throw new GeneralPlatformDomainRuleException("error.msg.crb.not.enabled",
                    "CRB not enabled for this Tenant."
            );
        }

        final Loan loan = this.loanRepositoryWrapper.findOneWithNotFoundDetection(loanId);

        if (!loan.isSubmittedAndPendingApproval()){
            throw new GeneralPlatformDomainRuleException("error.msg.crb.loan.not.pending", "Kyc Check Score enquiry cannot be performed because the loan is not pending.");
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

        Integer report = command.integerValueOfParameterNamed(CrbApiConstants.crbReportTypeParamName);
        Integer reportFormat = command.integerValueOfParameterNamed(CrbApiConstants.crbReportFormatParamName);
        Integer reportAction = command.integerValueOfParameterNamed(CrbApiConstants.crbReportActionFormatParamName);

        final CrbConsent crbConsent = this.crbConsentRepository.findVerifiedClient(client.getId(), ConsentStatus.VERIFIED.getValue());

        final CreditReports validationType = CreditReports.fromInt(report);

        // Reuse a previous successful credit check when still valid.
        if (validationType.equals(CreditReports.CREDIT_REPORT)) {

            final CrbCheck reusableCheck = findReusableCheck(client.getId(), report, mapping.getStalePeriod());

            if (reusableCheck != null) {
                return new CommandProcessingResultBuilder()
                        .withEntityId(reusableCheck.getId())
                        .withClientId(client.getId())
                        .build();
            }
        }

        final CrbCheck crbCheck = CrbCheck.create(client, loan, crbConsent, report, CrbRequestType.INITIAL.getValue());

        this.crbCheckRepository.saveAndFlush(crbCheck);

        try{

            var check = creditReport(client, reportFormat, reportAction);

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

            if (mapping.isSkipCreditCheckInFailure() && isProviderFailure(e)) {
                return new CommandProcessingResultBuilder()
                        .withEntityId(crbCheck.getId())
                        .withClientId(client.getId())
                        .build();
            }

            throw e;
        }
    }


    private CrbProviderResult creditReport(Client client, Integer reportFormat, Integer reportAction) {

        final CrbReportFormat crbReportFormat = CrbReportFormat.fromInt(reportFormat);
        final CrbReportAction crbReportAction = CrbReportAction.fromInt(reportAction);

        final LegalForm legalForm = LegalForm.fromInt(client.getLegalForm());

        if (legalForm == null) {
            throw new GeneralPlatformDomainRuleException("error.msg.crb.client.legal.form.invalid", "Invalid client legal form.");
        }

        final CrbCheck crbCheck = findCreditEnquiryCheck(client, CreditValidationType.CREDIT_ENQUIRY.getValue());

        if (!CRBStatus.VERIFIED.getValue().equals(crbCheck.getStatus())) {
            throw new GeneralPlatformDomainRuleException("error.msg.crb.validation.not.verified",
                    "Credit report cannot be generated because the client's CRB validation has not been verified."
            );
        }

        final CreditReportRequestData request = creditClientTypeReport(client, crbReportFormat, crbCheck);

        if (crbReportFormat.equals(CrbReportFormat.JSON)) {

            final CreditReportJsonResponseData jsonResponseData = gnugridService.creditReport(request);

            //Todo: What Makes the Report verified really???
            //Todo: I assume that if the first enquiry was successful then we don't need a verification here
            final boolean verified = true;

            return CrbProviderResult.builder()
                    .verified(verified)
                    .responsePayload(serializeResponse(jsonResponseData))
                    .build();
        }

        final CreditReportPdfResponseData pdfResponseData = gnugridService.creditPdfReport(request);

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

    @Override
    @Transactional(readOnly = true)
    public String getCreditReportPayload(Long loanId) {

        final Loan loan = loanRepositoryWrapper.findOneWithNotFoundDetection(loanId);

        final Client client = loan.getClient();

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

    private String serializeResponse(final Object response) {
        try {
            return this.objectMapper.writeValueAsString(response);
        } catch (final JsonProcessingException exception) {
            throw new GeneralPlatformDomainRuleException("error.msg.crb.validation.response.serialization.failed",
                    "Unable to serialize the CRB validation response.");
        }
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

    private CrbCheck findReusableCheck(final Long clientId, final Integer validationType, final int stalePeriod) {

        if (stalePeriod <= 0) {
            return null;
        }

        final List<CrbCheck> checks = this.crbCheckRepository.findLatestVerifiedCheck(clientId, validationType,
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
