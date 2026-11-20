package org.apache.fineract.portfolio.crb.domain;

import org.apache.fineract.infrastructure.core.exception.GeneralPlatformDomainRuleException;

public enum CreditEntityType {

    INDIVIDUAL(0, "entityType.Individual", "Individual"),
    NON_INDIVIDUAL(1, "entityType.nonIndividual", "Non-Individual");

    private final Integer value;
    private final String code;
    private final String description;

    CreditEntityType(Integer value, String code, String description) {
        this.value = value;
        this.code = code;
        this.description = description;
    }

    public static CreditEntityType fromInt(Integer value){
        for (CreditEntityType type: values()){
            if (type.value.equals(value)){
                return type;
            }
        }
        throw new GeneralPlatformDomainRuleException("error.msg.invalid.crb.credit.value", "Invalid CRB Credit value: " + value);
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
