package org.apache.fineract.portfolio.crb.data;


import lombok.Getter;
import org.apache.fineract.infrastructure.codes.data.CodeValueData;
import org.apache.fineract.infrastructure.core.data.EnumOptionData;
import org.apache.fineract.portfolio.crb.domain.ConsentChannel;
import org.apache.fineract.portfolio.crb.domain.ConsentStatus;

import java.time.LocalDateTime;
import java.util.Collection;

@Getter
public final class CrbConsentData {

    private final Long id;
    private final Long clientId;
    private final String clientName;
    private final EnumOptionData consentChannel;
    private final EnumOptionData consentStatus;
    private final String phoneNumber;
    private final String consentVersion;
    private final LocalDateTime consentedAt;
    private final LocalDateTime revokedAt;
    private final Long consentedBy;
    private final Long verifiedBy;
    private final Long revokedBy;
    private final Collection<EnumOptionData> consentChannels;
    private final Collection<CodeValueData> narrations;


    public CrbConsentData(Long id, Long clientId, String clientName, EnumOptionData consentChannel, EnumOptionData consentStatus, String phoneNumber, String consentVersion, LocalDateTime consentedAt,
                          LocalDateTime revokedAt, Long consentedBy, Long verifiedBy, Long revokedBy) {
        this.id = id;
        this.clientId = clientId;
        this.clientName = clientName;
        this.consentChannel = consentChannel;
        this.consentStatus = consentStatus;
        this.phoneNumber = phoneNumber;
        this.consentVersion = consentVersion;
        this.consentedAt = consentedAt;
        this.revokedAt = revokedAt;
        this.consentedBy = consentedBy;
        this.verifiedBy = verifiedBy;
        this.revokedBy = revokedBy;
        this.consentChannels = null;
        this.narrations = null;
    }

    public CrbConsentData(Collection<EnumOptionData> consentChannels, Collection<CodeValueData> narrations) {
        this.consentChannels = consentChannels;
        this.id = null;
        this.clientId = null;
        this.clientName = null;
        this.consentChannel = null;
        this.consentStatus = null;
        this.phoneNumber = null;
        this.consentVersion = null;
        this.consentedAt = null;
        this.revokedAt = null;
        this.consentedBy = null;
        this.verifiedBy = null;
        this.revokedBy = null;
        this.narrations = narrations;
    }

    public static CrbConsentData instance(final Long id, final Long clientId, final String clientName, final Integer consentChannel, final Integer consentStatus,
                                          final String phoneNumber, final String consentVersion, final LocalDateTime consentedAt, final LocalDateTime revokedAt,
                                          final Long consentedBy, final Long verifiedBy, final Long revokedBy) {
        return new CrbConsentData(id, clientId, clientName,  getChannel(consentChannel), getStatus(consentStatus), phoneNumber, consentVersion, consentedAt,
                revokedAt, consentedBy, verifiedBy, revokedBy
        );
    }

    public static CrbConsentData channelTemplate(Collection<EnumOptionData> consentChannels, Collection<CodeValueData> narrations){
        return new CrbConsentData(consentChannels, narrations);
    }

    public static EnumOptionData getChannel(Integer channel){
        ConsentChannel channel1 = ConsentChannel.fromInt(channel);
        return new EnumOptionData(channel1.getValue().longValue(), channel1.getCode(), channel1.getDescription());
    }

    public static EnumOptionData getStatus(Integer status){
        ConsentStatus status1 = ConsentStatus.fromInt(status);
        return new EnumOptionData(status1.getValue().longValue(), status1.getCode(), status1.getDescription());
    }



}
