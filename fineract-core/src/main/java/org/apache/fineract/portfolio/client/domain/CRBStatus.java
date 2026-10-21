package org.apache.fineract.portfolio.client.domain;

import org.apache.fineract.infrastructure.core.exception.GeneralPlatformDomainRuleException;

public enum CRBStatus {

    NOT_STARTED(1, "crb.notStarted", "Not Started"),
    PENDING(2, "crb.Pending", "Pending"),
    VERIFIED(3, "crb.Verified", "Verified"),
    FAILED(4, "crb.Failed", "Failed"),
    MANUAL_REVIEW(5, "crb.Manual", "Manual"),
    OVERRIDDEN(6, "crb.Overriden", "Overridden"),
    ERROR(7, "crb.Error", "Error");

    private final Integer value;
    private final String code;
    private final String description;

    CRBStatus(Integer value, String code, String description) {
        this.value = value;
        this.code = code;
        this.description = description;
    }

    public static CRBStatus fromInt(Integer value){
        for (CRBStatus status: values()){
            if (status.value.equals(value)){
                return status;
            }
        }
        throw new GeneralPlatformDomainRuleException("error.msg.invalid.crb.status.value", "Invalid CRB Status value: " + value);
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
