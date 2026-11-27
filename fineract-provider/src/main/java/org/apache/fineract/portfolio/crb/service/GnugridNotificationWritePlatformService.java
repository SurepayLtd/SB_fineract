package org.apache.fineract.portfolio.crb.service;

import org.apache.fineract.portfolio.crb.data.ValidatePersonalInformationResponseData;
import org.apache.fineract.portfolio.crb.data.CreditReportPdfResponseData;
import org.apache.fineract.portfolio.crb.data.CreditReportJsonResponseData;
import org.apache.fineract.portfolio.crb.data.CreditScoreResponseData;
import org.apache.fineract.portfolio.crb.data.CreditScoreRequestData;
import org.apache.fineract.portfolio.crb.data.CreditReportRequestData;
import org.apache.fineract.portfolio.crb.data.CreditEnquiryRequestData;
import org.apache.fineract.portfolio.crb.data.CreditEnquiryResponseData;
import org.apache.fineract.portfolio.crb.data.KycReportResponseJsonData;
import org.apache.fineract.portfolio.crb.data.KycReportRequestData;
import org.apache.fineract.portfolio.crb.data.KycEnquiryRequestData;
import org.apache.fineract.portfolio.crb.data.KycEnquiryResponseData;
import org.apache.fineract.portfolio.crb.data.ValidatePhoneResponseData;
import org.apache.fineract.portfolio.crb.data.ValidateNINResponseData;
import org.apache.fineract.portfolio.crb.data.ValidatePersonalInformationRequestData;


public interface GnugridNotificationWritePlatformService {

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
