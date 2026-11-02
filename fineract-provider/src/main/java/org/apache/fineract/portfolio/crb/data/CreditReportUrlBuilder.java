package org.apache.fineract.portfolio.crb.data;

import org.apache.fineract.infrastructure.core.exception.GeneralPlatformDomainRuleException;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public final class CreditReportUrlBuilder {

    private static final String SOURCE = "api";

    private static final ZoneId TIME_ZONE = ZoneId.of("Africa/Kampala");

    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                    .withZone(TIME_ZONE);

    private CreditReportUrlBuilder() {
    }

    public static String buildReportUrl(String baseUrl, CreditReportGenerationData data) {

        String profileType;
        Long profileId;

        if (data.getIndividualId() != null) {
            profileType = "individual";
            profileId = data.getIndividualId();
        } else if (data.getNonIndividualId() != null) {
            profileType = "non-individual";
            profileId = data.getNonIndividualId();
        } else {
            throw new GeneralPlatformDomainRuleException("error.msg.crb.credit.report.invalid.profile", "Either individualId or nonIndividualId is required.");
        }

        String action = data.getAction().toLowerCase();

        if (!action.equals("view") && !action.equals("download")) {
            throw new GeneralPlatformDomainRuleException("error.msg.crb.credit.report.invalid.action", "Action must be either view or download.");
        }

        return stripTrailingSlash(baseUrl)
                + "/credit-report/"
                + profileType + "/"
                + data.getEnquiryId() + "/"
                + profileId + "/"
                + data.getAccountId() + "/"
                + data.getReference() + "/"
                + data.getTimestamp() + "/"
                + SOURCE + "/print/"
                + data.getToken()
                + "?action=" + action;
    }

    public static String formatTimestamp(Long timestamp) {
        return TIMESTAMP_FORMAT.format(Instant.ofEpochSecond(timestamp));
    }

    private static String stripTrailingSlash(String url) {
        return url.replaceAll("/+$", "");
    }
}
