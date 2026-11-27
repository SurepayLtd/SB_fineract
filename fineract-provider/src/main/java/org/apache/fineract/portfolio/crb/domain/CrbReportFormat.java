package org.apache.fineract.portfolio.crb.domain;

import org.apache.fineract.infrastructure.core.exception.GeneralPlatformDomainRuleException;

public enum CrbReportFormat {
    JSON(1, "crbReportFormat.Json", "json"),
    PDF(2, "crbReportFormat.pdf", "pdf");

    private final Integer value;
    private final String code;
    private final String description;

    CrbReportFormat(Integer value, String code, String description) {
        this.value = value;
        this.code = code;
        this.description = description;
    }

    public static CrbReportFormat fromInt(Integer value){
        for (CrbReportFormat status: values()){
            if (status.value.equals(value)){
                return status;
            }
        }
        throw new GeneralPlatformDomainRuleException("error.msg.invalid.crb.report.format.value", "Invalid Crb Report Value: " + value);
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
