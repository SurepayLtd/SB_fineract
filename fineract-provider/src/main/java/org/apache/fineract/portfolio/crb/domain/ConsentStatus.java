package org.apache.fineract.portfolio.crb.domain;

import org.apache.fineract.infrastructure.core.exception.GeneralPlatformDomainRuleException;

public enum ConsentStatus {

    PENDING(1, "status.Pending", "Pending"),
    VERIFIED(2, "status.Veried", "Verified"),
    REVOKED(3, "status.Revoked", "Revoked");

    private final Integer value;
    private final String code;
    private final String description;

    ConsentStatus(Integer value, String code, String description) {
        this.value = value;
        this.code = code;
        this.description = description;
    }

    public static ConsentStatus fromInt(Integer value){
        for (ConsentStatus status: values()){
            if (status.value.equals(value)){
                return status;
            }
        }
        throw new GeneralPlatformDomainRuleException("error.msg.invalid.crb.consent.value", "Invalid Consent Status value: " + value);
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
