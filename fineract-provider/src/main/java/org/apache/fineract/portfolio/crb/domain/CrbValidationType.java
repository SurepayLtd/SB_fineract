package org.apache.fineract.portfolio.crb.domain;

public enum CrbValidationType {
    DETAILS(1, "crbValidationType.details", "Validation Details"),
    NIN(2, "crbValidationType.nin", "NIN Number"),
    PHONE(3, "crbValidationType.details", "Phone Number");

    private final Integer value;
    private final String code;
    private final String description;


    CrbValidationType(Integer value, String code, String description) {
        this.value = value;
        this.code = code;
        this.description = description;
    }

    public static CrbValidationType fromInt(Integer value){
        for (CrbValidationType status: values()){
            if (status.value.equals(value)){
                return status;
            }
        }
        throw new IllegalArgumentException("Invalid Crb Validation value: " + value);
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
