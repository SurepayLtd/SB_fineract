package org.apache.fineract.portfolio.crb.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.apache.fineract.infrastructure.core.data.EnumOptionData;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.fineract.portfolio.crb.data.CrbCheckData;
import org.apache.fineract.portfolio.crb.domain.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class CrbCheckReadServicePlatformImpl implements CrbCheckReadServicePlatform{

    private final JdbcTemplate jdbcTemplate;
    private final PlatformSecurityContext context;
    private final ObjectMapper objectMapper;


    @Override
    public List<CrbCheckData> retrieveCreditChecks(Long loanId, Integer type) {
        this.context.authenticatedUser();
        CheckMapper mp = new CheckMapper(objectMapper);
        StringBuilder sb = new StringBuilder();
        sb.append(" Select ");
        sb.append(mp.schema());
        sb.append(" WHERE cc.loan_id = ? ");
        sb.append(" AND cc.validation_type = ? ");
        sb.append(" ORDER BY cc.created_at DESC, cc.id DESC ");
        return this.jdbcTemplate.query(sb.toString(), mp, loanId, type);
    }

    @Override
    public List<CrbCheckData> retrieveClientChecks(Long clientId) {
        this.context.authenticatedUser();
        CheckMapper mp = new CheckMapper(objectMapper);
        StringBuilder sb = new StringBuilder();
        sb.append(" Select ");
        sb.append(mp.schema());
        sb.append(" WHERE cc.client_id = ? ");
        sb.append(" ORDER BY cc.created_at DESC, cc.id DESC ");
        return this.jdbcTemplate.query(sb.toString(), mp, clientId);
    }

    @Override
    public List<CrbCheckData> retrieveKycChecks(Long clientId) {
        this.context.authenticatedUser();
        CheckMapper mp = new CheckMapper(objectMapper);
        StringBuilder sb = new StringBuilder();
        sb.append(" Select ");
        sb.append(mp.schema());
        sb.append(" WHERE cc.client_id = ? AND cc.validation_type = 100");
        sb.append(" ORDER BY cc.created_at DESC, cc.id DESC ");
        return this.jdbcTemplate.query(sb.toString(), mp, clientId);
    }

    @Override
    public CrbCheckData retrieveTypes() {
        return CrbCheckData.dropdown(getRequestTypes(), getValidationTypes(), getCreditReportActionTypes(), getCreditReportFormatTypes(), getCreditReportTypes(), getCreditValidationTypes(), getCreditScoreTypes());
    }

    protected static class CheckMapper implements RowMapper<CrbCheckData>{

        private final ObjectMapper objectMapper;

        public CheckMapper(ObjectMapper objectMapper) {
            this.objectMapper = objectMapper;
        }

        @Override
        public CrbCheckData mapRow(ResultSet rs, int rowNum) throws SQLException {

            final Long id = rs.getLong("id");
            final Long clientId = rs.getLong("clientId");
            final String clientName = rs.getString("clientName");
            final String clientAccount = rs.getString("clientAccount");
            final Long loanId = rs.getLong("loanId");
            final String loanProductName = rs.getString("loanProductName");
            final String loanAccount = rs.getString("loanAccount");
            final BigDecimal proposedPrincipal = rs.getBigDecimal("proposedPrincipal");
            final Integer validationType = rs.getInt("validationType");
            final Integer requestType = rs.getInt("requestType");
            final Integer status = rs.getInt("status");
            final Integer consentStatus = rs.getInt("consentStatus");
            final String providerValidationId = rs.getString("providerValidationId");
            final String providerReference = rs.getString("providerReference");
            final BigDecimal cost = rs.getBigDecimal("cost");
            final String currency = rs.getString("currency");
            final String message  = rs.getString("message");
            final String errorMessage = rs.getString("errorMessage");
            final String responsePayload = rs.getString("responsePayload");
            final LocalDateTime createdAt = rs.getTimestamp("createdAt") != null
                            ? rs.getTimestamp("createdAt").toLocalDateTime()
                            : null;
            final LocalDateTime completedAt = rs.getTimestamp("completedAt") != null
                            ? rs.getTimestamp("completedAt").toLocalDateTime()
                            : null;
            final String reportUrl = rs.getString("reportUrl");
            final Map<String, Object> responseDetails = extractResponseDetails(validationType, responsePayload);
            return CrbCheckData.display(id, clientId, clientName, clientAccount, loanId, loanProductName, loanAccount, proposedPrincipal, validationType, requestType, status, consentStatus,
                    providerValidationId, providerReference, cost, currency,
                    message, errorMessage, responsePayload, responseDetails, createdAt, completedAt, reportUrl);
        }

        public String schema(){

            final StringBuilder builder = new StringBuilder(300);

            builder.append("cc.id AS id, ");
            builder.append("cc.client_id AS clientId, ");
            builder.append("c.display_name AS clientName, ");
            builder.append("c.account_no AS clientAccount, ");
            builder.append("l.account_no AS loanAccount, ");
            builder.append("l.id AS loanId, ");
            builder.append("lp.name AS loanProductName, ");
            builder.append("l.principal_amount_proposed AS proposedPrincipal, ");
            builder.append("cc.validation_type AS validationType, ");
            builder.append("cc.request_type AS requestType, ");
            builder.append("cc.check_status AS status, ");
            builder.append("cr.consent_status AS consentStatus, ");
            builder.append("cc.provider_validation_id AS providerValidationId, ");
            builder.append("cc.provider_reference AS providerReference, ");
            builder.append("cc.cost AS cost, ");
            builder.append("cc.currency AS currency, ");
            builder.append("cc.message AS message, ");
            builder.append("cc.error_message AS errorMessage, ");
            builder.append("cc.response_payload AS responsePayload, ");
            builder.append("cc.created_at AS createdAt, ");
            builder.append("cc.report_url AS reportUrl, ");
            builder.append("cc.completed_at AS completedAt");
            builder.append(" FROM m_crb_check cc ");
            builder.append(" LEFT JOIN m_client c ON c.id = cc.client_id ");
            builder.append(" LEFT JOIN m_crb_consent cr ON cr.id = cc.consent_id ");
            builder.append(" LEFT JOIN m_loan l ON l.id = cc.loan_id ");
            builder.append(" LEFT JOIN m_product_loan lp ON lp.id = l.product_id ");

            return builder.toString();
        }

        private Map<String, Object> extractResponseDetails(final Integer validationType, final String responsePayload) {

            if (responsePayload == null || responsePayload.isBlank()) {
                return Map.of();
            }

            if (validationType == null) {
                return Map.of();
            }

            try {
                JsonNode root = objectMapper.readTree(responsePayload);

                CrbValidationType type = null;

                for (CrbValidationType validation : CrbValidationType.values()) {
                    if (validation.getValue().equals(validationType)) {
                        type = validation;
                        break;
                    }
                }

                if (type == null) {
                    return Map.of();
                }

                return switch (type) {
                    case DETAILS -> extractClientDetails(root);
                    case NIN -> extractNinDetails(root);
                    case PHONE -> extractPhoneDetails(root);
                };

            } catch (JsonProcessingException e) {
                return Map.of("message", "Unable to read provider response");
            }
        }

        private Map<String, Object> extractClientDetails(JsonNode root) {
            JsonNode validation = root.path("validation");

            Map<String, Object> details = new LinkedHashMap<>();
            details.put("status", validation.path("status").asText(""));
            details.put("ninStatus", validation.path("nin_status").asText(""));
            details.put("matchingStatus", validation.path("matching_status").asText(""));
            details.put("cardStatus", validation.path("card_status").asText(""));

            return details;
        }

        private Map<String, Object> extractNinDetails(JsonNode root) {
            JsonNode validation = root.path("validation");

            Map<String, Object> details = new LinkedHashMap<>();
            details.put("status", validation.path("status").asText(""));
            details.put("ninStatus", validation.path("nin_status").asText(""));

            return details;
        }

        private Map<String, Object> extractPhoneDetails(JsonNode root) {
            JsonNode validation = root.path("validation");

            Map<String, Object> details = new LinkedHashMap<>();
            details.put("status", validation.path("status").asText(""));
            details.put("phoneStatus", validation.path("phone_status").asText(""));

            return details;
        }
    }

    private Collection<EnumOptionData> getValidationTypes(){

        Collection<EnumOptionData> data = new ArrayList<>();
        for (CrbValidationType type : CrbValidationType.values()){
            data.add(new EnumOptionData(
                    type.getValue().longValue(), type.getCode(), type.getDescription()
            ));
        }
        return data;
    }

    private Collection<EnumOptionData> getRequestTypes(){

        Collection<EnumOptionData> data = new ArrayList<>();
        for (CrbRequestType type : CrbRequestType.values()){
            data.add(new EnumOptionData(
                    type.getValue().longValue(), type.getCode(), type.getDescription()
            ));
        }
        return data;
    }

    private Collection<EnumOptionData> getCreditReportActionTypes(){

        Collection<EnumOptionData> data = new ArrayList<>();
        for (CrbReportAction action : CrbReportAction.values()){
            data.add(new EnumOptionData(
                    action.getValue().longValue(), action.getCode(), action.getDescription()
            ));
        }
        return data;
    }

    private Collection<EnumOptionData> getCreditReportFormatTypes(){

        Collection<EnumOptionData> data = new ArrayList<>();
        for (CrbReportFormat type : CrbReportFormat.values()){
            data.add(new EnumOptionData(
                    type.getValue().longValue(), type.getCode(), type.getDescription()
            ));
        }
        return data;
    }

    private Collection<EnumOptionData> getCreditReportTypes(){

        Collection<EnumOptionData> data = new ArrayList<>();
        for (CreditReports type : CreditReports.values()){
            data.add(new EnumOptionData(
                    type.getValue().longValue(), type.getCode(), type.getDescription()
            ));
        }
        return data;
    }

    private Collection<EnumOptionData> getCreditValidationTypes(){

        Collection<EnumOptionData> data = new ArrayList<>();
        for (CreditValidationType type : CreditValidationType.values()){
            data.add(new EnumOptionData(
                    type.getValue().longValue(), type.getCode(), type.getDescription()
            ));
        }
        return data;
    }

    private Collection<EnumOptionData> getCreditScoreTypes(){

        Collection<EnumOptionData> data = new ArrayList<>();
        for (CreditScore type : CreditScore.values()){
            data.add(new EnumOptionData(
                    type.getValue().longValue(), type.getCode(), type.getDescription()
            ));
        }
        return data;
    }




}
