package org.apache.fineract.portfolio.crb.domain;

import org.apache.fineract.infrastructure.core.exception.GeneralPlatformDomainRuleException;

public enum CrbRequestType {
    INITIAL(1, "crbRequestType.Initial", "Initial Validation"),
    REVALIDATION(2, "crbRequestType.Revalidation", "Revalidation");

    private final Integer value;
    private final String code;
    private final String description;

    CrbRequestType(Integer value, String code, String description) {
        this.value = value;
        this.code = code;
        this.description = description;
    }

    public static CrbRequestType fromInt(Integer value){
        for (CrbRequestType status: values()){
            if (status.value.equals(value)){
                return status;
            }
        }
        throw new GeneralPlatformDomainRuleException("error.msg.invalid.crb.request.value", "Invalid CRB Request value: " + value);
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
