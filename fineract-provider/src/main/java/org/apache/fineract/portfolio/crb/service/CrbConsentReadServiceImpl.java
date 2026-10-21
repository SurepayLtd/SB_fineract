package org.apache.fineract.portfolio.crb.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.infrastructure.codes.data.CodeValueData;
import org.apache.fineract.infrastructure.codes.service.CodeValueReadPlatformService;
import org.apache.fineract.infrastructure.core.data.EnumOptionData;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.fineract.portfolio.crb.data.CrbConsentData;
import org.apache.fineract.portfolio.crb.domain.ConsentChannel;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CrbConsentReadServiceImpl implements CrbConsentReadService{
    private final PlatformSecurityContext context;
    private final JdbcTemplate jdbcTemplate;
    private final CodeValueReadPlatformService codeValueReadPlatformService;


    @Override
    public CrbConsentData retrieveConsent(Long consentId) {
        this.context.authenticatedUser();
        ConsentMapper mp = new ConsentMapper();


        StringBuilder sql = new StringBuilder();
        sql.append("SELECT ");
        sql.append(mp.schema());
        sql.append(" WHERE cc.id = ? ");


        return this.jdbcTemplate.queryForObject(sql.toString(), mp, consentId);
    }

    @Override
    public CrbConsentData retrieveChannelTemplate() {
        this.context.authenticatedUser();
        return CrbConsentData.channelTemplate(getChannels(), null);
    }

    @Override
    public CrbConsentData retrieveAllNarrations(String consentNarrations) {
        final List<CodeValueData> narrations = new ArrayList<>(this.codeValueReadPlatformService.retrieveCodeValuesByCode(consentNarrations));
        return CrbConsentData.channelTemplate(null, narrations);
    }

    protected static class ConsentMapper implements RowMapper<CrbConsentData> {

        @Override
        public CrbConsentData mapRow(ResultSet rs, int rowNum) throws SQLException {
            final Long consentedBy = rs.getObject("consented_by", Long.class);
            final Long verifiedBy = rs.getObject("verified_by", Long.class);
            final Long revokedBy = rs.getObject("revoked_by", Long.class);
            final Long reactivatedBy = rs.getObject("reactivated_by", Long.class);

            return CrbConsentData.instance(
                    rs.getLong("id"),
                    rs.getLong("client_id"),
                    rs.getString("client_name"),
                    rs.getInt("consent_channel"),
                    rs.getInt("consent_status"),
                    rs.getString("phone_number"),
                    rs.getString("consent_version"),
                    rs.getTimestamp("consented_at") != null
                            ? rs.getTimestamp("consented_at").toLocalDateTime() : null,
                    rs.getTimestamp("revoked_at") != null
                            ? rs.getTimestamp("revoked_at").toLocalDateTime() : null,
                    rs.getTimestamp("reactivated_at") != null
                            ? rs.getTimestamp("reactivated_at").toLocalDateTime() : null,
                    consentedBy,
                    rs.getString("consentedByName"),
                    verifiedBy,
                    rs.getString("verifiedByName"),
                    revokedBy,
                    rs.getString("revokedByName"),
                    reactivatedBy,
                    rs.getString("reactivatedByName")

                    );
        }

        public String schema() {
            final StringBuilder builder = new StringBuilder(500);

            builder.append("cc.id AS id, ");
            builder.append("cc.client_id AS client_id, ");
            builder.append("c.display_name AS client_name, ");
            builder.append("cc.consent_channel AS consent_channel, ");
            builder.append("cc.consent_status AS consent_status, ");
            builder.append("cc.phone_number AS phone_number, ");
            builder.append("cc.consent_version AS consent_version, ");
            builder.append("cc.consented_at AS consented_at, ");
            builder.append("cc.revoked_at AS revoked_at, ");
            builder.append("cc.reactivated_at AS reactivated_at, ");
            builder.append("cc.consented_by AS consented_by, ");
            builder.append("consentedUser.username AS consentedByName, ");
            builder.append("cc.verified_by AS verified_by, ");
            builder.append("verifiedUser.username AS verifiedByName, ");
            builder.append("cc.revoked_by AS revoked_by, ");
            builder.append("revokedUser.username AS revokedByName, ");
            builder.append("cc.reactivated_by AS reactivated_by, ");
            builder.append("reactivatedUser.username AS reactivatedByName ");
            builder.append("FROM m_crb_consent cc ");
            builder.append("JOIN m_client c ON c.id = cc.client_id ");
            builder.append("LEFT JOIN m_appuser consentedUser ");
            builder.append("ON consentedUser.id = cc.consented_by ");
            builder.append("LEFT JOIN m_appuser verifiedUser ");
            builder.append("ON verifiedUser.id = cc.verified_by ");
            builder.append("LEFT JOIN m_appuser revokedUser ");
            builder.append("ON revokedUser.id = cc.revoked_by ");
            builder.append("LEFT JOIN m_appuser reactivatedUser ");
            builder.append("ON reactivatedUser.id = cc.reactivated_by ");

            return builder.toString();
        }
    }

    public Collection<EnumOptionData> getChannels(){
        Collection<EnumOptionData> data = new ArrayList<>();
        for (ConsentChannel channel: ConsentChannel.values()){
            data.add(new EnumOptionData(channel.getValue().longValue(), channel.getCode(), channel.getDescription()));
        }
        return data;
    }
}
