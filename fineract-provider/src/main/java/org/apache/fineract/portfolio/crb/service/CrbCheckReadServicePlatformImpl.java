package org.apache.fineract.portfolio.crb.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.fineract.portfolio.crb.data.CrbCheckData;
import org.apache.fineract.portfolio.crb.domain.CrbValidationType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CrbCheckReadServicePlatformImpl implements CrbCheckReadServicePlatform{

    private final JdbcTemplate jdbcTemplate;
    private final PlatformSecurityContext context;
    private final ObjectMapper objectMapper;



    @Override
    public List<CrbCheckData> retrieveChecks(Long clientId) {
        this.context.authenticatedUser();
        CheckMapper mp = new CheckMapper(objectMapper);
        StringBuilder sb = new StringBuilder();
        sb.append(" Select ");
        sb.append(mp.schema());
        sb.append(" WHERE cc.client_id = ? ");
        sb.append(" ORDER BY cc.created_at DESC, cc.id DESC ");
        return this.jdbcTemplate.query(sb.toString(), mp, clientId);
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
            final Integer validationType = rs.getInt("validationType");
            final Integer requestType = rs.getInt("requestType");
            final Integer status = rs.getInt("status");
            final Integer consentStatus = rs.getInt("consentStatus");
            final String providerValidationId = rs.getString("providerValidationId");
            final String providerReference = rs.getString("providerReference");
            final BigDecimal cost = rs.getBigDecimal("cost");
            final String currency = rs.getString("currency");
            final String message  = rs.getString("message");
            final String errorMessage = rs.getString("error_message");
            final String responsePayload = rs.getString("response_payload");
            final LocalDateTime createdAt = rs.getTimestamp("created_at") != null
                            ? rs.getTimestamp("created_at").toLocalDateTime()
                            : null;
            final LocalDateTime completedAt = rs.getTimestamp("completed_at") != null
                            ? rs.getTimestamp("completed_at").toLocalDateTime()
                            : null;
            final Map<String, Object> responseDetails = extractResponseDetails(validationType, responsePayload);
            return CrbCheckData.display(id, clientId, clientName, validationType, requestType, status, consentStatus,
                    providerValidationId, providerReference, cost, currency,
                    message, errorMessage, responsePayload, responseDetails, createdAt, completedAt);
        }

        public String schema(){

            final StringBuilder builder = new StringBuilder(300);

            builder.append("cc.id AS id, ");
            builder.append("cc.client_id AS clientId, ");
            builder.append("c.display_name AS clientName, ");
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
            builder.append("cc.completed_at AS completedAt");
            builder.append(" FROM m_crb_check cc ");
            builder.append(" JOIN m_client c ON c.id = cc.client_id ");
            builder.append(" JOIN m_crb_consent cr ON cr.id = cc.consent_id ");

            return builder.toString();
        }

        private Map<String, Object> extractResponseDetails(Integer validationType, String responsePayload) {

            if (responsePayload == null || responsePayload.isBlank()) {
                return Map.of();
            }

            try {
                JsonNode root = objectMapper.readTree(responsePayload);

                CrbValidationType type = CrbValidationType.fromInt(validationType);

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


}
