package org.apache.fineract.portfolio.crb.domain;

import org.apache.fineract.infrastructure.core.exception.GeneralPlatformDomainRuleException;

public enum CreditScore {
    ALL(1, "", "All Scores"),
    CRB(2, "CRB", "Crb Score"),
    MNO(3, "MNO", "Mobile Number Score"),
//    SACCO(4, "SACCO", "Sacco Score"),
    CRB_MNO(4, "CRB.MNO", "Crb and Mobile Number Score");
//    CRB_SACCO(6, "CRB.SACCO", "Crb and Sacco Score")// Not our package,
//    MNO_SACCO(7, "MNO.SACCO", "Mobile Number and Sacco Score");

    private final Integer value;
    private final String code;
    private final String description;

    CreditScore(Integer value, String code, String description) {
        this.value = value;
        this.code = code;
        this.description = description;
    }

    public static CreditScore fromInt(Integer value){
        for (CreditScore type: values()){
            if (type.value.equals(value)){
                return type;
            }
        }
        throw new GeneralPlatformDomainRuleException("error.msg.invalid.credit.score.value", "Invalid Credit Score value: " + value);
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
