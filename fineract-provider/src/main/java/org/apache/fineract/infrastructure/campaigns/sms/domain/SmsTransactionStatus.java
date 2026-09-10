package org.apache.fineract.infrastructure.campaigns.sms.domain;

public enum SmsTransactionStatus {
    PENDING(1, "Pending"),
    SENT(2, "Sent"),
    FAILED(3, "Failed");

    private final Integer value;
    private final String description;

    SmsTransactionStatus(Integer value, String description) {
        this.value = value;
        this.description = description;
    }

    public static SmsTransactionStatus fromString(final String value){
        for (SmsTransactionStatus transaction: SmsTransactionStatus.values()){
            if (transaction.description.equals(value)){
                return transaction;
            }
        }

        throw new IllegalArgumentException("Unknown SmsTransaction: "+ value);
    }

    public Integer getValue() {
        return value;
    }

    public String getDescription() {
        return description;
    }
}
