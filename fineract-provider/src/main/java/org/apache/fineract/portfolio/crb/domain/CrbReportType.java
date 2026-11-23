package org.apache.fineract.portfolio.crb.domain;

public enum CrbReportType {
    CREDIT("credit-report"),
    KYC("kyc-report");

    private final String path;

    CrbReportType(String path) {
        this.path = path;
    }

    public String getPath() {
        return path;
    }
}
