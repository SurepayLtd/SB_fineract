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

@Service
@RequiredArgsConstructor
@Slf4j
public class GnugridNotificationWritePlatformServiceImpl implements GnugridNotificationWritePlatformService{

    private final Environment env;
    private final ConfigurationDomainService configurationDomainService;

    @Override
    public ValidatePersonalInformationResponseData validatePersonalInfo(ValidatePersonalInformationRequestData requestData) {

        ValidatePersonalInformationResponseData responseData;

        boolean isValidateInfoEnabled = configurationDomainService.isCRBValidateDetailsEnabled();

        if (!isValidateInfoEnabled) {
            throw new GeneralPlatformDomainRuleException("error.msg.crb.validation.disabled", "CRB personal information validation is disabled.");
        }

        String accessToken = getCrbAccessToken();
        RestTemplate restTemplate = new RestTemplate();
        String url = getProperty("crb.details.url");

        try {

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setAccept(MediaType.parseMediaTypes(MediaType.APPLICATION_JSON_VALUE));
            headers.setBearerAuth(accessToken);

            HttpEntity<ValidatePersonalInformationRequestData> entity = new HttpEntity<>(requestData, headers);

            ResponseEntity<ValidatePersonalInformationResponseData> response = restTemplate.exchange(url, HttpMethod.POST, entity, ValidatePersonalInformationResponseData.class);

            int code = response.getStatusCode().value();

            responseData = response.getBody();

            if (code != 200 || responseData == null || responseData.getValidation() == null) {

                throw new GeneralPlatformDomainRuleException("error.msg.crb.validation.invalid.response",
                        "CRB returned an invalid personal information validation response.");
            }
            log.info("Personal Details: {}", responseData);

            return responseData;


        }catch (final HttpStatusCodeException exception) {
            throw new GeneralPlatformDomainRuleException("error.msg.crb.validation.request.failed",
                    "CRB personal information validation failed with HTTP status "
                            + exception.getStatusCode().value() + "."
            );

        } catch (final RestClientException exception) {
            throw new GeneralPlatformDomainRuleException("error.msg.crb.validation.connection.failed",
                    "Unable to communicate with the CRB validation service."
            );
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    @Override
    public ValidatePersonalInformationResponseData reValidatePersonalInfo(String validationId) {
        ValidatePersonalInformationResponseData responseData;

        boolean isValidateInfoEnabled = configurationDomainService.isCRBValidateDetailsEnabled();

        if (!isValidateInfoEnabled) {
            throw new GeneralPlatformDomainRuleException("error.msg.crb.validation.disabled", "CRB personal information validation is disabled.");
        }

        String accessToken = getCrbAccessToken();
        RestTemplate restTemplate = new RestTemplate();
        String url = getProperty("crb.details.revalidation.url") + "/" + validationId.trim();

        try {

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setAccept(MediaType.parseMediaTypes(MediaType.APPLICATION_JSON_VALUE));
            headers.setBearerAuth(accessToken);

            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<ValidatePersonalInformationResponseData> response = restTemplate.exchange(url, HttpMethod.GET, entity, ValidatePersonalInformationResponseData.class);

            int code = response.getStatusCode().value();

            responseData = response.getBody();

            if (code != 200 || responseData == null || responseData.getValidation() == null) {

                throw new GeneralPlatformDomainRuleException("error.msg.crb.validation.invalid.response",
                        "CRB returned an invalid personal information validation response.");
            }
            log.info("Personal Details: {}", responseData);

            return responseData;


        }catch (final HttpStatusCodeException exception) {
            throw new GeneralPlatformDomainRuleException("error.msg.crb.validation.request.failed",
                    "CRB personal information validation failed with HTTP status "
                            + exception.getStatusCode().value() + "."
            );

        } catch (final RestClientException exception) {
            throw new GeneralPlatformDomainRuleException("error.msg.crb.validation.connection.failed",
                    "Unable to communicate with the CRB validation service."
            );
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public ValidateNINResponseData validateNIN(String ninNumber) {
        ValidateNINResponseData responseData;

        boolean isValidateNINEnabled = configurationDomainService.isCRBValidateNINEnabled();

        if (!isValidateNINEnabled) {
            throw new GeneralPlatformDomainRuleException("error.msg.crb.nin.validation.disabled", "CRB NIN validation is disabled.");
        }

        String accessToken = getCrbAccessToken();
        RestTemplate restTemplate = new RestTemplate();
        String url = getProperty("crb.nin.url");

        try {

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setAccept(MediaType.parseMediaTypes(MediaType.APPLICATION_JSON_VALUE));
            headers.setBearerAuth(accessToken);

            Map<String, String> requestBody = new HashMap<>();
            requestBody.put("nin", ninNumber);

            HttpEntity<Map<String, String>> entity = new HttpEntity<>(requestBody,headers);

            ResponseEntity<ValidateNINResponseData> response = restTemplate.exchange(url, HttpMethod.POST, entity, ValidateNINResponseData.class);

            int code = response.getStatusCode().value();

            responseData = response.getBody();

            if (code != 200 || responseData == null || responseData.getValidation() == null) {

                throw new GeneralPlatformDomainRuleException("error.msg.crb.nin.validation.invalid.response",
                        "CRB returned an invalid NIN validation response.");
            }
            log.info("NinData: {}", responseData);
            return responseData;


        }catch (final HttpStatusCodeException exception) {
            throw new GeneralPlatformDomainRuleException("error.msg.crb.nin.validation.request.failed",
                    "CRB NIN validation failed with HTTP status "
                            + exception.getStatusCode().value() + "."
            );

        } catch (final RestClientException exception) {
            throw new GeneralPlatformDomainRuleException("error.msg.crb.nin.validation.connection.failed",
                    "Unable to communicate with the CRB NIN service."
            );
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public ValidateNINResponseData reValidateNIN(String validationId) {
        ValidateNINResponseData responseData;

        boolean isValidateNINEnabled = configurationDomainService.isCRBValidateNINEnabled();

        if (!isValidateNINEnabled) {
            throw new GeneralPlatformDomainRuleException("error.msg.crb.nin.validation.disabled", "CRB NIN validation is disabled.");
        }

        String accessToken = getCrbAccessToken();
        RestTemplate restTemplate = new RestTemplate();
        String url = getProperty("crb.nin.revalidation.url") + "/" + validationId.trim();

        try {

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setAccept(MediaType.parseMediaTypes(MediaType.APPLICATION_JSON_VALUE));
            headers.setBearerAuth(accessToken);


            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<ValidateNINResponseData> response = restTemplate.exchange(url, HttpMethod.GET, entity, ValidateNINResponseData.class);

            int code = response.getStatusCode().value();

            responseData = response.getBody();

            if (code != 200 || responseData == null || responseData.getValidation() == null) {

                throw new GeneralPlatformDomainRuleException("error.msg.crb.nin.validation.invalid.response",
                        "CRB returned an invalid NIN validation response.");
            }
            log.info("NinData: {}", responseData);
            return responseData;


        }catch (final HttpStatusCodeException exception) {
            throw new GeneralPlatformDomainRuleException("error.msg.crb.nin.validation.request.failed",
                    "CRB NIN validation failed with HTTP status "
                            + exception.getStatusCode().value() + "."
            );

        } catch (final RestClientException exception) {
            throw new GeneralPlatformDomainRuleException("error.msg.crb.nin.validation.connection.failed",
                    "Unable to communicate with the CRB NIN service."
            );
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public ValidatePhoneResponseData validatePhone(String phone) {
        ValidatePhoneResponseData responseData;

        boolean isValidatePhoneEnabled = configurationDomainService.isCRBValidatePhoneEnabled();

        if (!isValidatePhoneEnabled) {
            throw new GeneralPlatformDomainRuleException("error.msg.crb.phone.validation.disabled", "CRB Phone Number validation is disabled.");
        }

        String accessToken = getCrbAccessToken();
        RestTemplate restTemplate = new RestTemplate();
        String url = getProperty("crb.phone.url");

        try {

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setAccept(MediaType.parseMediaTypes(MediaType.APPLICATION_JSON_VALUE));
            headers.setBearerAuth(accessToken);

            Map<String, String> requestBody = new HashMap<>();
            requestBody.put("phonenumber", phone);

            HttpEntity<Map<String, String>> entity = new HttpEntity<>(requestBody,headers);

            ResponseEntity<ValidatePhoneResponseData> response = restTemplate.exchange(url, HttpMethod.POST, entity, ValidatePhoneResponseData.class);

            int code = response.getStatusCode().value();

            responseData = response.getBody();

            if (code != 200 || responseData == null || responseData.getValidation() == null) {

                throw new GeneralPlatformDomainRuleException("error.msg.crb.phone.validation.invalid.response",
                        "CRB returned an invalid Phone Number validation response.");
            }
            log.info("PhoneData: {}", responseData);
            return responseData;


        }catch (final HttpStatusCodeException exception) {
            throw new GeneralPlatformDomainRuleException("error.msg.crb.phone.validation.request.failed",
                    "CRB Phone Number validation failed with HTTP status "
                            + exception.getStatusCode().value() + "."
            );

        } catch (final RestClientException exception) {
            throw new GeneralPlatformDomainRuleException("error.msg.crb.phone.validation.connection.failed",
                    "Unable to communicate with the Phone Number service."
            );
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public ValidatePhoneResponseData reValidatePhone(String validationId) {
        ValidatePhoneResponseData responseData;

        boolean isValidatePhoneEnabled = configurationDomainService.isCRBValidatePhoneEnabled();

        if (!isValidatePhoneEnabled) {
            throw new GeneralPlatformDomainRuleException("error.msg.crb.phone.validation.disabled", "CRB Phone Number validation is disabled.");
        }

        String accessToken = getCrbAccessToken();
        RestTemplate restTemplate = new RestTemplate();
        String url = getProperty("crb.phone.revalidation.url") + "/" + validationId.trim();

        try {

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setAccept(MediaType.parseMediaTypes(MediaType.APPLICATION_JSON_VALUE));
            headers.setBearerAuth(accessToken);

            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<ValidatePhoneResponseData> response = restTemplate.exchange(url, HttpMethod.GET, entity, ValidatePhoneResponseData.class);

            int code = response.getStatusCode().value();

            responseData = response.getBody();

            if (code != 200 || responseData == null || responseData.getValidation() == null) {

                throw new GeneralPlatformDomainRuleException("error.msg.crb.phone.validation.invalid.response",
                        "CRB returned an invalid Phone Number validation response.");
            }
            log.info("PhoneData: {}", responseData);
            return responseData;


        }catch (final HttpStatusCodeException exception) {
            throw new GeneralPlatformDomainRuleException("error.msg.crb.phone.validation.request.failed",
                    "CRB Phone Number validation failed with HTTP status "
                            + exception.getStatusCode().value() + "."
            );

        } catch (final RestClientException exception) {
            throw new GeneralPlatformDomainRuleException("error.msg.crb.phone.validation.connection.failed",
                    "Unable to communicate with the Phone Number service."
            );
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String getProperty(String property){
        return env.getProperty(property);
    }

    private String getCrbAccessToken(){

        GnugridTokenResponse tokenResponse;

        boolean isCrbEnabled = configurationDomainService.isCRBEnabled();

        if (isCrbEnabled) {

            RestTemplate restTemplate = new RestTemplate();

            String url = getProperty("crb.oauth.url");


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
                return e.getMessage();
            }
        }else {
            log.info("** CRB is disabled for this Tenant :-> " + ThreadLocalContextUtil.getTenant().getName());
            throw new GeneralPlatformDomainRuleException("error.msg.crb.authentication.invalid.response",
                    "CRB not enabled for this Tenant."
            );
        }

    }
}
