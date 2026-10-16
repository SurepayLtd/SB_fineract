package org.apache.fineract.portfolio.client.domain;

public enum CRBStatus {

    NOT_STARTED(1, "crb.notStarted"),
    PENDING(2, "crb.Pending"),
    VERIFIED(3, "crb.Pending"),
    FAILED(4, "crb.Pending"),
    MANUAL_REVIEW(5, "crb.Pending"),
    OVERRIDDEN(6, "crb.Pending");

    private final Integer value;
    private final String code;

    CRBStatus(Integer value, String code) {
        this.value = value;
        this.code = code;
    }

    public static CRBStatus fromInt(Integer value){
        for (CRBStatus status: values()){
            if (status.value.equals(value)){
                return status;
            }
        }
        throw new IllegalArgumentException("Invalid CRB status value: " + value);
    }

    public Integer getValue() {
        return value;
    }

    public String getCode() {
        return code;
    }
}
