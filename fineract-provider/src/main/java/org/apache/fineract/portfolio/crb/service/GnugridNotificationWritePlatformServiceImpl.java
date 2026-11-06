package org.apache.fineract.portfolio.crb.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.infrastructure.configuration.domain.ConfigurationDomainService;
import org.apache.fineract.infrastructure.core.exception.GeneralPlatformDomainRuleException;
import org.apache.fineract.infrastructure.core.service.ThreadLocalContextUtil;
import org.apache.fineract.portfolio.crb.data.*;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

@Service
@RequiredArgsConstructor
@Slf4j
public class GnugridNotificationWritePlatformServiceImpl implements GnugridNotificationWritePlatformService{

    private final Environment env;
    private final ConfigurationDomainService configurationDomainService;

    @Override
    public CreditScoreResponseData creditScore(CreditScoreRequestData creditScoreRequestData) {

        boolean isCrbCreditScoreEnquiryEnabled = configurationDomainService.isCrbCreditScoreEnabled();

        if (!isCrbCreditScoreEnquiryEnabled) {
            throw new GeneralPlatformDomainRuleException("error.msg.crb.credit.score.disabled", "CRB Credit Score is disabled.");
        }
        final String endpoint = "credit-enquiries/credit-scores";

        final String url = baseUrl() + endpoint;

        final var responseData = executeCrbRequest(
                url,
                HttpMethod.POST,
                creditScoreRequestData,
                CreditScoreResponseData.class,
                "error.msg.crb.credit.enquiry.invalid.response",
                "Credit Enquiry returned an invalid personal information validation response..",
                response -> response.getData() != null,
                "error.msg.crb.credit.enquiry.request.failed",
                "error.msg.crb.credit.enquiry.connection.failed"
        );

        log.info("Credit Score: {}", responseData);

        return responseData;
    }

    @Override
    public CreditReportJsonResponseData creditReport(CreditReportRequestData creditReportRequestData) {

        boolean isCrbReportEnquiryEnabled = configurationDomainService.isCrbCreditReportEnabled();

        if (!isCrbReportEnquiryEnabled) {
            throw new GeneralPlatformDomainRuleException("error.msg.crb.credit.report.disabled", "CRB Credit ENQUIRY is disabled.");
        }

        final boolean hasIndividualId = creditReportRequestData.getIndividual_id() != null;

        final boolean hasNonIndividualId = creditReportRequestData.getNon_individual_id() != null;

        if (hasIndividualId == hasNonIndividualId) {
            throw new GeneralPlatformDomainRuleException("error.msg.credit.identifier.invalid", "Exactly one identifier must be provided: individual_id or non_individual_id.");
        }

        final String endpoint = hasIndividualId ? "credit-enquiries/individual/credit-report" : "credit-enquiries/non-individual/credit-report";

        final String url = baseUrl() + endpoint;

        final var responseData = executeCrbRequest(
                url,
                HttpMethod.POST,
                creditReportRequestData,
                CreditReportJsonResponseData.class,
                "error.msg.crb.credit.report.invalid.response",
                "Credit Report returned an invalid personal information validation response..",
                response -> response.getCredit_information() != null,
                "error.msg.crb.report.enquiry.request.failed",
                "error.msg.crb.report.enquiry.connection.failed"
        );

        log.info("Credit Report: {}", responseData);

        return responseData;
    }

    @Override
    public CreditReportPdfResponseData creditPdfReport(CreditReportRequestData creditReportRequestData) {

        boolean isCrbReportEnquiryEnabled = configurationDomainService.isCrbCreditReportEnabled();

        if (!isCrbReportEnquiryEnabled) {
            throw new GeneralPlatformDomainRuleException("error.msg.crb.credit.report.disabled", "CRB Credit ENQUIRY is disabled.");
        }

        final boolean hasIndividualId = creditReportRequestData.getIndividual_id() != null;

        final boolean hasNonIndividualId = creditReportRequestData.getNon_individual_id() != null;

        if (hasIndividualId == hasNonIndividualId) {
            throw new GeneralPlatformDomainRuleException("error.msg.credit.identifier.invalid", "Exactly one identifier must be provided: individual_id or non_individual_id.");
        }

        final String endpoint = hasIndividualId ? "credit-enquiries/individual/credit-report" : "credit-enquiries/non-individual/credit-report";

        final String url = baseUrl() + endpoint;

        final var responseData = executeCrbRequest(
                url,
                HttpMethod.POST,
                creditReportRequestData,
                CreditReportPdfResponseData.class,
                "error.msg.crb.credit.report.invalid.response",
                "Credit Report returned an invalid personal information validation response..",
                response -> response.getToken() != null,
                "error.msg.crb.report.enquiry.request.failed",
                "error.msg.crb.report.enquiry.connection.failed"
        );

        log.info("Credit Report: {}", responseData);

        return responseData;
    }

    @Override
    public CreditEnquiryResponseData creditEnquiry(CreditEnquiryRequestData creditEnquiryRequestData) {
        boolean isCrbCreditEnquiryEnabled = configurationDomainService.isCrbCreditEnquiryEnabled();

        if (!isCrbCreditEnquiryEnabled) {
            throw new GeneralPlatformDomainRuleException("error.msg.crb.credit.enquiry.disabled", "CRB Credit ENQUIRY is disabled.");
        }
        final String endpoint = "credit-enquiries/request/new";

        final String url = baseUrl() + endpoint;

        final var responseData = executeCrbRequest(
                url,
                HttpMethod.POST,
                creditEnquiryRequestData,
                CreditEnquiryResponseData.class,
                "error.msg.crb.credit.enquiry.invalid.response",
                "Credit Enquiry returned an invalid personal information validation response..",
                response -> response.getEnquiry() != null,
                "error.msg.crb.credit.enquiry.request.failed",
                "error.msg.crb.credit.enquiry.connection.failed"
        );

        log.info("Credit Enquiry: {}", responseData);

        return responseData;
    }

    @Override
    public KycReportResponseJsonData kycReport(final KycReportRequestData kycReportRequestData) {

        boolean isKycReportEnabled = configurationDomainService.isCrbKycReportEnabled();


        if (!isKycReportEnabled) {
            throw new GeneralPlatformDomainRuleException("error.msg.crb.kyc.report.disabled", "CRB KYC REPORT is disabled.");
        }

        final boolean hasIndividualId = kycReportRequestData.getIndividual_id() != null;

        final boolean hasNonIndividualId = kycReportRequestData.getNon_individual_id() != null;

        if (hasIndividualId == hasNonIndividualId) {
            throw new GeneralPlatformDomainRuleException("error.msg.kyc.identifier.invalid", "Exactly one identifier must be provided: individual_id or non_individual_id.");
        }

        final String endpoint = hasIndividualId ? "kyc-enquiries/individual/kyc-report" : "kyc-enquiries/non-individual/kyc-report";

        final String url = baseUrl() + endpoint;

        final var responseData = executeCrbRequest(
                url,
                HttpMethod.POST,
                kycReportRequestData,
                KycReportResponseJsonData.class,
                "error.msg.crb.kyc.report.invalid.response",
                "KYC Report returned an invalid personal information validation response.",
                response -> response.getKyc_information() != null,
                "error.msg.crb..kyc.report.request.failed",
                "error.msg.crb..kyc.report.connection.failed"
        );

        log.info("KYC Report: {}", responseData);

        return responseData;
    }


    @Override
    public KycEnquiryResponseData validateKyc(final KycEnquiryRequestData kycEnquiryRequestData) {


        boolean isKycEnquiryEnabled = configurationDomainService.isCrbKycEnquiryEnabled();

        if (!isKycEnquiryEnabled) {
            throw new GeneralPlatformDomainRuleException("error.msg.crb.kyc.enquiry.disabled", "CRB KYC ENQUIRY is disabled.");
        }

        final String endpoint = "kyc-enquiries/request/new";

        final String url = baseUrl() + endpoint;

        final var responseData = executeCrbRequest(
                url,
                HttpMethod.POST,
                kycEnquiryRequestData,
                KycEnquiryResponseData.class,
                "error.msg.crb.kyc.enquiry.invalid.response",
                "KYC Enquiry returned an invalid personal information validation response..",
                response -> response.getEnquiry() != null,
                "error.msg.crb.kyc.enquiry.request.failed",
                "error.msg.crb.kyc.enquiry.connection.failed"
        );

        log.info("KYC Enquiry: {}", responseData);

        return responseData;
    }

    @Override
    public ValidatePersonalInformationResponseData validatePersonalInfo( final ValidatePersonalInformationRequestData requestData) {


        boolean isValidateInfoEnabled = configurationDomainService.isCRBValidateDetailsEnabled();

        if (!isValidateInfoEnabled) {
            throw new GeneralPlatformDomainRuleException("error.msg.crb.validation.disabled", "CRB personal information validation is disabled.");
        }

        final String endpoint = "validate-personal-info";

        final String url = baseUrl() + endpoint;

        var responseData = executeCrbRequest(
                url,
                HttpMethod.POST,
                requestData,
                ValidatePersonalInformationResponseData.class,
                "error.msg.crb.validation.invalid.response",
                "CRB returned an invalid personal information validation response..",
                response -> response.getValidation() != null,
                "error.msg.crb.validation.request.failed",
                "error.msg.crb.validation.connection.failed"
        );

        log.info("Personal Details: {}", responseData);

        return responseData;

    }

    @Override
    public ValidatePersonalInformationResponseData reValidatePersonalInfo(String validationId) {
        ValidatePersonalInformationResponseData responseData;

        boolean isValidateInfoEnabled = configurationDomainService.isCRBValidateDetailsEnabled();

        if (!isValidateInfoEnabled) {
            throw new GeneralPlatformDomainRuleException("error.msg.crb.validation.disabled", "CRB personal information validation is disabled.");
        }

        final String endpoint = "personal-info-validation" + "/" + validationId.trim();

        final String url = baseUrl() + endpoint;

        responseData = executeCrbRequest(
                url,
                HttpMethod.GET,
                null,
                ValidatePersonalInformationResponseData.class,
                "error.msg.crb.validation.invalid.response",
                "CRB returned an invalid personal information validation response...",
                response -> response.getValidation() != null,
                "error.msg.crb.validation.request.failed",
                "error.msg.crb.validation.connection.failed"
        );

        log.info("Personal Details: {}", responseData);

        return responseData;
    }

    @Override
    public ValidateNINResponseData validateNIN(String ninNumber) {
        ValidateNINResponseData responseData;

        boolean isValidateNINEnabled = configurationDomainService.isCRBValidateNINEnabled();

        if (!isValidateNINEnabled) {
            throw new GeneralPlatformDomainRuleException("error.msg.crb.nin.validation.disabled", "CRB NIN validation is disabled.");
        }

        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("nin", ninNumber);

        final String endpoint = "validate-nin";

        final String url = baseUrl() + endpoint;

        responseData = executeCrbRequest(
                url,
                HttpMethod.POST,
                requestBody,
                ValidateNINResponseData.class,
                "error.msg.crb.nin.validation.invalid.response",
                "CRB returned an invalid NIN validation response..",
                response -> response.getValidation() != null,
                "error.msg.crb.nin.validation.request.failed",
                "error.msg.crb.nin.validation.connection.failed"
        );

        log.info("NinData: {}", responseData);

        return responseData;
    }

    @Override
    public ValidateNINResponseData reValidateNIN(String validationId) {
        ValidateNINResponseData responseData;

        boolean isValidateNINEnabled = configurationDomainService.isCRBValidateNINEnabled();

        if (!isValidateNINEnabled) {
            throw new GeneralPlatformDomainRuleException("error.msg.crb.nin.validation.disabled", "CRB NIN validation is disabled.");
        }

        final String endpoint = "nin-validation" + "/" + validationId.trim();

        final String url = baseUrl() + endpoint;

        responseData = executeCrbRequest(
                url,
                HttpMethod.GET,
                null,
                ValidateNINResponseData.class,
                "error.msg.crb.nin.validation.invalid.response",
                "CRB returned an invalid NIN validation response.",
                response -> response.getValidation() != null,
                "error.msg.crb.nin.validation.request.failed",
                "error.msg.crb.nin.validation.connection.failed"
        );

        log.info("NinData: {}", responseData);

        return responseData;
    }

    @Override
    public ValidatePhoneResponseData validatePhone(String phone) {
        ValidatePhoneResponseData responseData;

        boolean isValidatePhoneEnabled = configurationDomainService.isCRBValidatePhoneEnabled();

        if (!isValidatePhoneEnabled) {
            throw new GeneralPlatformDomainRuleException("error.msg.crb.phone.validation.disabled", "CRB Phone Number validation is disabled.");
        }

        final String endpoint = "validate-phone";

        final String url = baseUrl() + endpoint;

        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("phonenumber", phone);

        responseData = executeCrbRequest(
                url,
                HttpMethod.POST,
                requestBody,
                ValidatePhoneResponseData.class,
                "error.msg.crb.phone.validation.invalid.response",
                "CRB returned an invalid Phone Number validation response.",
                response -> response.getValidation() != null,
                "error.msg.crb.phone.validation.request.failed",
                "error.msg.crb.phone.validation.connection.failed"
        );

        log.info("Phone Data: {}", responseData);

        return responseData;
    }

    @Override
    public ValidatePhoneResponseData reValidatePhone(String validationId) {
        ValidatePhoneResponseData responseData;

        boolean isValidatePhoneEnabled = configurationDomainService.isCRBValidatePhoneEnabled();

        if (!isValidatePhoneEnabled) {
            throw new GeneralPlatformDomainRuleException("error.msg.crb.phone.validation.disabled", "CRB Phone Number validation is disabled.");
        }

        final String endpoint = "phone-validation" + "/" + validationId.trim();

        final String url = baseUrl() + endpoint;

        responseData = executeCrbRequest(
                url,
                HttpMethod.GET,
                null,
                ValidatePhoneResponseData.class,
                "error.msg.crb.phone.validation.invalid.response",
                "CRB returned an invalid Phone Number validation response.",
                response -> response.getValidation() != null,
                "error.msg.crb.phone.validation.request.failed",
                "error.msg.crb.phone.validation.connection.failed"
        );

        log.info("Phone Data: {}", responseData);

        return responseData;

    }

    private String getProperty(String property){
        return env.getProperty(property);
    }

    private String getCrbAccessToken(){

        GnugridTokenResponse tokenResponse;

        boolean isCrbEnabled = configurationDomainService.isCRBEnabled();

        if (isCrbEnabled) {

            RestTemplate restTemplate = new RestTemplate();

            String url = baseUrl() + "oauth/token";


            try {

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.setAccept(MediaType.parseMediaTypes(MediaType.APPLICATION_JSON_VALUE));
                headers.set("X-Requested-With", "XMLHttpRequest");


                Map<String, String> requestBody = new HashMap<>();
                requestBody.put("grant_type", "client_credentials");
                requestBody.put("client_id", getProperty("crb.client.id"));
                requestBody.put("client_secret", getProperty("crb.client.secret"));

                HttpEntity<Map<String, String>> entity = new HttpEntity<>(requestBody, headers);

                ResponseEntity<GnugridTokenResponse> response = restTemplate.exchange(url, HttpMethod.POST, entity, GnugridTokenResponse.class);

                int code = response.getStatusCode().value();
                tokenResponse = response.getBody();

                if (code != 200 && code != 201) {

                    throw new GeneralPlatformDomainRuleException("error.msg.crb.authentication.invalid.response",
                            "CRB authentication returned an invalid token response."
                    );

                }
                log.info("AccessToken: {}", tokenResponse.getAccess_token());
                return tokenResponse.getAccess_token();


            } catch (final HttpStatusCodeException exception) {

                final String responseBody = exception.getResponseBodyAsString();

                try {
                    final GnugridTokenResponse.ErrorResponse errorResponse =
                            new ObjectMapper().readValue(responseBody, GnugridTokenResponse.ErrorResponse.class);

                    final String errorMessage = errorResponse.getError_description() != null
                            ? errorResponse.getError_description()
                            : errorResponse.getMessage() != null
                            ? errorResponse.getMessage()
                            : "CRB authentication failed.";

                    throw new GeneralPlatformDomainRuleException("error.msg.crb.authentication.failed", errorMessage);

                } catch (final JsonProcessingException jsonException) {
                    throw new GeneralPlatformDomainRuleException("error.msg.crb.authentication.failed", "CRB authentication failed with HTTP status "
                            + exception.getStatusCode().value() + "."
                    );
                }

            } catch (Exception e) {
                throw new GeneralPlatformDomainRuleException("error.msg.crb.authentication.failed", "Unable to authenticate with the CRB service."
                );
            }
        }else {
            log.info("** CRB is disabled for this Tenant :-> " + ThreadLocalContextUtil.getTenant().getName());
            throw new GeneralPlatformDomainRuleException("error.msg.crb.authentication.invalid.response",
                    "CRB not enabled for this Tenant."
            );
        }

    }

    private String baseUrl(){
        return getProperty("crb.base.url");
    }


    /**
     * Executes an authenticated request against the Gnugrid CRB API.
     *
     * @param url CRB endpoint URL
     * @param method HTTP method
     * @param requestBody request payload (can be null for GET requests)
     * @param responseType expected response class
     * @param invalidResponseCode error code for invalid responses
     * @param invalidResponseMessage error message for invalid responses
     * @param responseValidator validates the returned response body
     * @return deserialized response
     */
    private <T> T executeCrbRequest(final String url, final HttpMethod method, final Object requestBody, final Class<T> responseType,
            final String invalidResponseCode, final String invalidResponseMessage, final Predicate<T> responseValidator, final String requestFailedCode, final String connectionFailedCode) {

        final String accessToken = getCrbAccessToken();

        final RestTemplate restTemplate = new RestTemplate();

        final HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(MediaType.parseMediaTypes(MediaType.APPLICATION_JSON_VALUE));
        headers.setBearerAuth(accessToken);

        final HttpEntity<?> entity = new HttpEntity<>(requestBody, headers);

        try {
            final ResponseEntity<T> response = restTemplate.exchange(url, method, entity, responseType);

            final T responseData = response.getBody();

            if (!response.getStatusCode().is2xxSuccessful() || responseData == null || !responseValidator.test(responseData)) {

                throw new GeneralPlatformDomainRuleException(invalidResponseCode, invalidResponseMessage);
            }

            return responseData;

        } catch (final HttpStatusCodeException exception) {

            throw new GeneralPlatformDomainRuleException(requestFailedCode, "CRB request failed with HTTP status " + exception.getStatusCode().value() + ".");
        } catch (final RestClientException exception) {

            throw new GeneralPlatformDomainRuleException(connectionFailedCode, "Unable to communicate with the CRB service.");
        }
    }

}
