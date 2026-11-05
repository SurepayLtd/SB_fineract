package org.apache.fineract.portfolio.crb.data;

import org.apache.fineract.infrastructure.core.exception.GeneralPlatformDomainRuleException;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public final class CreditReportUrlBuilder {

    private static final String SOURCE = "api";

    private static final ZoneId TIME_ZONE = ZoneId.of("Africa/Kampala");

    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                    .withZone(TIME_ZONE);

    private CreditReportUrlBuilder() {}

    public static String buildReportUrl(final String baseUrl, final CreditReportGenerationData data) {

        if (baseUrl == null || baseUrl.isBlank()) {
            throw new GeneralPlatformDomainRuleException("error.msg.crb.credit.report.base.url.missing", "CRB credit report base URL is not configured.");
        }

        if (data == null || data.getEnquiry_id() == null || data.getAccount_id() == null || data.getReference() == null || data.getTimestamp() == null || data.getToken() == null) {
            throw new GeneralPlatformDomainRuleException("error.msg.crb.credit.report.data.invalid", "Required credit report generation details are missing.");
        }

        final String profileType;
        final Integer profileId;

        if (data.getIndividual_id() != null && data.getNonindividual_id() == null) {

            profileType = "individual";
            profileId = data.getIndividual_id();

        } else if (data.getNonindividual_id() != null && data.getIndividual_id() == null) {

            profileType = "non-individual";
            profileId = data.getNonindividual_id();

        } else {

            throw new GeneralPlatformDomainRuleException("error.msg.crb.credit.report.invalid.profile", "Exactly one of individualId or nonIndividualId must be provided.");
        }

        if (data.getAction() == null) {
            throw new GeneralPlatformDomainRuleException("error.msg.crb.credit.report.invalid.action", "Report action is required.");
        }

        final String action = data.getAction().toLowerCase(Locale.ROOT);

        if (!action.equals("view") && !action.equals("download")) {
            throw new GeneralPlatformDomainRuleException("error.msg.crb.credit.report.invalid.action", "Action must be either view or download.");
        }

        return stripTrailingSlash(baseUrl)
                + "/credit-report/"
                + profileType + "/"
                + data.getEnquiry_id() + "/"
                + profileId + "/"
                + data.getAccount_id() + "/"
                + data.getReference() + "/"
                + formatTimestamp(data.getTimestamp()) + "/"
                + SOURCE + "/print/"
                + data.getToken()
                + "?action=" + action;
    }

    public static String formatTimestamp(final Long timestamp) {

        if (timestamp == null) {
            throw new GeneralPlatformDomainRuleException("error.msg.crb.credit.report.timestamp.missing", "Credit report timestamp is required.");
        }

        return TIMESTAMP_FORMAT.format(Instant.ofEpochSecond(timestamp));
    }

    private static String stripTrailingSlash(final String url) {
        return url.replaceAll("/+$", "");
    }
}