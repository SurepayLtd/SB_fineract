package org.apache.fineract.portfolio.crb.service;

import org.apache.fineract.portfolio.crb.data.ValidateNINResponseData;
import org.apache.fineract.portfolio.crb.data.ValidatePersonalInformationRequestData;
import org.apache.fineract.portfolio.crb.data.ValidatePersonalInformationResponseData;
import org.apache.fineract.portfolio.crb.data.ValidatePhoneResponseData;

public interface GnugridNotificationWritePlatformService {

    ValidatePersonalInformationResponseData validatePersonalInfo(ValidatePersonalInformationRequestData requestData);

    ValidatePersonalInformationResponseData reValidatePersonalInfo(String validationId);

    ValidateNINResponseData validateNIN(String ninNumber);

    ValidateNINResponseData reValidateNIN(String validationId);

    ValidatePhoneResponseData validatePhone(String phone);

    ValidatePhoneResponseData reValidatePhone(String validationId);
}
