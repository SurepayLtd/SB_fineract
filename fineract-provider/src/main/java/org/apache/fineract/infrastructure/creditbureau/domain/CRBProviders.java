package org.apache.fineract.infrastructure.creditbureau.domain;

import org.apache.fineract.infrastructure.core.exception.GeneralPlatformDomainRuleException;

public enum CRBProviders {

    GNUGRID(1, "CRBProviders.gnugrid", "GNUGRID"),
    THITSAWORKS(2, "CRBProviders.thisWorks", "THITSAWORKS");


    private final Integer value;
    private final String code;
    private final String description;


    CRBProviders(Integer value, String code, String description) {
        this.value = value;
        this.code = code;
        this.description = description;
    }

    public static CRBProviders fromInt(Integer value){
        for (CRBProviders status: values()){
            if (status.value.equals(value)){
                return status;
            }
        }
        throw new GeneralPlatformDomainRuleException("error.msg.invalid.crb.bureau.value",
                "Invalid CRB Bureau value: " + value
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
