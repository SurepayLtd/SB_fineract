package org.apache.fineract.portfolio.crb.serializer;

import com.google.gson.JsonElement;
import com.google.gson.reflect.TypeToken;
import org.apache.commons.lang3.StringUtils;
import org.apache.fineract.infrastructure.configuration.service.ConfigurationReadPlatformService;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.ApiParameterError;
import org.apache.fineract.infrastructure.core.data.DataValidatorBuilder;
import org.apache.fineract.infrastructure.core.exception.InvalidJsonException;
import org.apache.fineract.infrastructure.core.exception.PlatformApiDataValidationException;
import org.apache.fineract.infrastructure.core.serialization.FromJsonHelper;
import org.apache.fineract.portfolio.crb.data.CrbApiConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public final class CrbDataValidator {

    private final FromJsonHelper fromApiJsonHelper;
    private final ConfigurationReadPlatformService configurationReadPlatformService;

    @Autowired
    public CrbDataValidator(FromJsonHelper fromApiJsonHelper, ConfigurationReadPlatformService configurationReadPlatformService) {
        this.fromApiJsonHelper = fromApiJsonHelper;
        this.configurationReadPlatformService = configurationReadPlatformService;
    }

    public void validateCrbConsent(final JsonCommand command){
        final String json = command.json();

        if (StringUtils.isBlank(json)) {
            throw new InvalidJsonException();
        }

        final Type typeOfMap = new TypeToken<Map<String, Object>>() {}.getType();
        this.fromApiJsonHelper.checkForUnsupportedParameters(typeOfMap, json, CrbApiConstants.CRB_CREATE_CONSENT_REQUEST_DATA_PARAMETERS);

        final List<ApiParameterError> dataValidationErrors = new ArrayList<>();
        final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors)
                .resource(CrbApiConstants.CRB_RESOURCE_NAME);

        final JsonElement element = command.parsedJson();

        final String consentText = this.fromApiJsonHelper.extractStringNamed(CrbApiConstants.consentTextParamName, element);
        baseDataValidator.reset().parameter(CrbApiConstants.consentTextParamName).value(consentText).notExceedingLengthOf(500);

        final Long consentChannel = this.fromApiJsonHelper.extractLongNamed(CrbApiConstants.consentChannelParamName, element);
        baseDataValidator.reset().parameter(CrbApiConstants.consentChannelParamName).value(consentChannel).ignoreIfNull().notExceedingLengthOf(5);

        throwExceptionIfValidationWarningsExist(dataValidationErrors);
    }

    public void validateOtpCode(final JsonCommand command) {

        final String json = command.json();

        if (StringUtils.isBlank(json)) {
            throw new InvalidJsonException();
        }

        final Type typeOfMap = new TypeToken<Map<String, Object>>() {}.getType();
        this.fromApiJsonHelper.checkForUnsupportedParameters(typeOfMap, json,
                CrbApiConstants.VALIDATE_OTP_REQUEST_DATA_PARAMETERS);

        final List<ApiParameterError> dataValidationErrors = new ArrayList<>();
        final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors)
                .resource(CrbApiConstants.CRB_RESOURCE_NAME);

        final JsonElement element = command.parsedJson();

        final String otpCode = this.fromApiJsonHelper.extractStringNamed(CrbApiConstants.otpCodeParamName, element);
        baseDataValidator.reset().parameter(CrbApiConstants.otpCodeParamName).value(otpCode).notExceedingLengthOf(5);


        throwExceptionIfValidationWarningsExist(dataValidationErrors);

    }

    public void validateCrbCheck(final JsonCommand command){
        final String json = command.json();

        if (StringUtils.isBlank(json)) {
            throw new InvalidJsonException();
        }

        final Type typeOfMap = new TypeToken<Map<String, Object>>() {}.getType();
        this.fromApiJsonHelper.checkForUnsupportedParameters(typeOfMap, json, CrbApiConstants.CRB_CREATE_CHECK_REQUEST_DATA_PARAMETERS);

        final List<ApiParameterError> dataValidationErrors = new ArrayList<>();
        final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors)
                .resource(CrbApiConstants.CRB_RESOURCE_NAME);

        final JsonElement element = command.parsedJson();

        final Long crbValidationType = this.fromApiJsonHelper.extractLongNamed(CrbApiConstants.crbValidationTypeParamName, element);
        baseDataValidator.reset().parameter(CrbApiConstants.crbValidationTypeParamName).value(crbValidationType).ignoreIfNull().notExceedingLengthOf(5);

        final Long crbRequestType = this.fromApiJsonHelper.extractLongNamed(CrbApiConstants.crbRequestTypeParamName, element);
        baseDataValidator.reset().parameter(CrbApiConstants.crbRequestTypeParamName).value(crbRequestType).ignoreIfNull().notExceedingLengthOf(5);

        throwExceptionIfValidationWarningsExist(dataValidationErrors);
    }

    private void throwExceptionIfValidationWarningsExist(final List<ApiParameterError> dataValidationErrors) {
        if (!dataValidationErrors.isEmpty()) {
            //
            throw new PlatformApiDataValidationException(dataValidationErrors);
        }
    }
}
