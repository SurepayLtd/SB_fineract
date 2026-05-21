package org.apache.fineract.portfolio.accountdetails.domain;

import org.apache.fineract.infrastructure.core.exception.GeneralPlatformDomainRuleException;

public enum LoanChannel {

    WEB(1, "loanChannel.web", "WEB"), //
    USSD(2, "loanChannel.ussd", "USSD"), //
    APP(3, "loanChannel.app", "Mobile App");


    private final Integer value;
    private final String code;
    private final String description;

    LoanChannel(Integer value, String code, String description) {
        this.value = value;
        this.code = code;
        this.description = description;
    }

    public static LoanChannel fromInt(final Integer channelValue) {

        for (LoanChannel channel: values()){
            if (channel.getValue().equals(channelValue)){
                return channel;
            }
        }
        throw new GeneralPlatformDomainRuleException("error.msg.ussd.loan.channel.not.allowed",
                "Invalid Channel");
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
