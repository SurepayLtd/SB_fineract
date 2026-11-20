package org.apache.fineract.portfolio.crb.domain;

import org.apache.fineract.infrastructure.core.exception.GeneralPlatformDomainRuleException;

public enum CreditReports {

    CREDIT_REPORT(500, "creditReports.creditReport", "Credit Report"),
    KYC_REPORT(600, "crbValidationType.kycReport", "KYC Report");


    private final Integer value;
    private final String code;
    private final String description;


    CreditReports(Integer value, String code, String description) {
        this.value = value;
        this.code = code;
        this.description = description;
    }

    public static CreditReports fromInt(Integer value){
        for (CreditReports status: values()){
            if (status.value.equals(value)){
                return status;
            }
        }
        throw new GeneralPlatformDomainRuleException("error.msg.invalid.credit.report.value",
                "Invalid Credit Report value: " + value
        );
    }

    public Integer getValue() {
        return value;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }
}
