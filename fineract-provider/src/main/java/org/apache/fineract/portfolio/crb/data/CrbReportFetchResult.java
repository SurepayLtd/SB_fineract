package org.apache.fineract.portfolio.crb.data;

import lombok.Getter;

import java.io.InputStream;

@Getter
public final class CrbReportFetchResult {

    private final InputStream stream;
    private final long length;
    private final int statusCode;
    private final String error;

    private CrbReportFetchResult(InputStream stream, long length, int statusCode, String error) {
        this.stream = stream;
        this.length = length;
        this.statusCode = statusCode;
        this.error = error;
    }

    public static CrbReportFetchResult success(InputStream stream, long length) {
        return new CrbReportFetchResult(stream, length, 0, null);
    }

    public static CrbReportFetchResult upstreamError(int statusCode) {
        return new CrbReportFetchResult(null, 0, statusCode, null);
    }

    public static CrbReportFetchResult ioError(String message) {
        return new CrbReportFetchResult(null, 0, 0, message);
    }

    public boolean isSuccess() {
        return stream != null;
    }
    public boolean isUpstreamError() {
        return statusCode > 0;
    }
    public boolean isIoError() {
        return error != null;
    }

}
