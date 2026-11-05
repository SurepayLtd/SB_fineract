package org.apache.fineract.portfolio.crb.domain;

import org.apache.fineract.infrastructure.core.exception.GeneralPlatformDomainRuleException;

public enum CreditValidationType {

    KYC_ENQUIRY(100, "creditValidationType.kycEnquiry", "KYC Enquiry"),
    CREDIT_ENQUIRY(200, "creditValidationType.creditEnquiry", "Credit Enquiry"),
    CREDIT_SCORE(300, "creditValidationType.creditReport", "Credit Score");

    private final Integer value;
    private final String code;
    private final String description;


    CreditValidationType(Integer value, String code, String description) {
        this.value = value;
        this.code = code;
        this.description = description;
    }

    public static CreditValidationType fromInt(Integer value){
        for (CreditValidationType status: values()){
            if (status.value.equals(value)){
                return status;
            }
        }
        throw new GeneralPlatformDomainRuleException("error.msg.invalid.credit.validation.value",
                "Invalid Credit validation value: " + value
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
