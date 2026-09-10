package org.apache.fineract.infrastructure.campaigns.sms.domain;

public enum SmsProvider {

    SUREPAY(1, "SurePay"),
    MAMBO(2, "Mambo");

    private final Integer value;
    private final String description;

    SmsProvider(Integer value, String description) {
        this.value = value;
        this.description = description;
    }

    public static SmsProvider fromString(final String value){
        for (SmsProvider provider: SmsProvider.values()){
            if (provider.description.equals(value)){
                return provider;
            }
        }

        throw new IllegalArgumentException("Unknown SmsProvider: "+ value);
    }

    public Integer getValue() {
        return value;
    }

    public String getDescription() {
        return description;
    }
}
