package org.apache.fineract.portfolio.crb.domain;

public enum ConsentChannel {

    OTP(1, "channel.Otp", "OTP Channel"),
    PAPER(2, "channel.Paper", "Paper");

    private final Integer value;
    private final String code;
    private final String description;

    ConsentChannel(Integer value, String code, String description) {
        this.value = value;
        this.code = code;
        this.description = description;
    }

    public static ConsentChannel fromInt(Integer value){
        for (ConsentChannel status: values()){
            if (status.value.equals(value)){
                return status;
            }
        }
        throw new IllegalArgumentException("Invalid Consent Channel value: " + value);
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
