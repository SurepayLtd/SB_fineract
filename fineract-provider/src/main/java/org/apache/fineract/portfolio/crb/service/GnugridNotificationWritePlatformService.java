package org.apache.fineract.portfolio.crb.service;

import org.apache.fineract.infrastructure.creditbureau.service.CreditBureauIntegrationWritePlatformService;
import org.apache.fineract.portfolio.crb.data.*;

public interface GnugridNotificationWritePlatformService extends CreditBureauIntegrationWritePlatformService {

    ValidatePersonalInformationResponseData validatePersonalInfo(ValidatePersonalInformationRequestData requestData);

    ValidatePersonalInformationResponseData reValidatePersonalInfo(String validationId);

    ValidateNINResponseData validateNIN(String ninNumber);

    ValidateNINResponseData reValidateNIN(String validationId);

    ValidatePhoneResponseData validatePhone(String phone);

    ValidatePhoneResponseData reValidatePhone(String validationId);

    KycEnquiryResponseData validateKyc(KycEnquiryRequestData kycEnquiryRequestData);

    KycReportResponseJsonData kycReport(KycReportRequestData kycReportRequestData);

    CreditEnquiryResponseData creditEnquiry(CreditEnquiryRequestData creditEnquiryRequestData);

    CreditReportJsonResponseData creditReport(CreditReportRequestData creditReportRequestData);

    CreditReportPdfResponseData creditPdfReport(CreditReportRequestData creditReportRequestData);

    CreditScoreResponseData creditScore(CreditScoreRequestData creditScoreRequestData);
}
