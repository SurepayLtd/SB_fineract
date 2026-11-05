package org.apache.fineract.portfolio.crb.domain;

import org.apache.fineract.infrastructure.core.exception.GeneralPlatformDomainRuleException;

public enum CrbReportAction {

    VIEW(1, "crbReportAction.View", "view"),
    DOWNLOAD(2, "crbReportAction.Download", "download");

    private final Integer value;
    private final String code;
    private final String description;


    CrbReportAction(Integer value, String code, String description) {
        this.value = value;
        this.code = code;
        this.description = description;
    }

    public static CrbReportAction fromInt(Integer value){
        for (CrbReportAction status: values()){
            if (status.value.equals(value)){
                return status;
            }
        }
        throw new GeneralPlatformDomainRuleException("error.msg.invalid.crb.report.action.value",
                "Invalid Crb Report action value: " + value
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
