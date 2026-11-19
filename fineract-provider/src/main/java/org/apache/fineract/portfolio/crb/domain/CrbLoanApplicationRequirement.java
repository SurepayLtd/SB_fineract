package org.apache.fineract.portfolio.crb.domain;

import org.apache.fineract.infrastructure.core.exception.GeneralPlatformDomainRuleException;

public enum CrbLoanApplicationRequirement {

    CREDIT_ENQUIRY_AND_REPORT(1, "Credit Enquiry and Report"),
    CREDIT_SCORE(2, "Credit Score"),
    BOTH(3, "Credit Enquiry, Report and Credit Score");

    private final Integer value;
    private final String description;

    CrbLoanApplicationRequirement(Integer value, String description) {
        this.value = value;
        this.description = description;
    }

    public static CrbLoanApplicationRequirement fromInt(Integer value){
        for (CrbLoanApplicationRequirement type: values()){
            if (type.value.equals(value)){
                return type;
            }
        }
        throw new GeneralPlatformDomainRuleException("error.msg.invalid.crb.loan.value", "Invalid Loan Application value: " + value);
    }

    public Integer getValue() {
        return value;
    }

    public String getDescription() {
        return description;
    }
}
