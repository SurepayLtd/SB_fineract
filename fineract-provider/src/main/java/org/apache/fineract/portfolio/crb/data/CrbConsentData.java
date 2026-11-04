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
    private final LocalDateTime reactivatedAt;
    private final Long consentedBy;
    private final String consentedByName;
    private final Long verifiedBy;
    private final String verifiedByName;
    private final Long revokedBy;
    private final String revokedByName;
    private final Long reactivatedBy;
    private final String reactivatedByName;
    private final Collection<EnumOptionData> consentChannels;
    private final Collection<CodeValueData> narrations;


    private CrbConsentData(Long id, Long clientId, String clientName, EnumOptionData consentChannel, EnumOptionData consentStatus, String phoneNumber, String consentVersion, LocalDateTime consentedAt, LocalDateTime revokedAt,
                           LocalDateTime reactivatedAt, Long consentedBy, String consentedByName, Long verifiedBy, String verifiedByName, Long revokedBy, String revokedByName, Long reactivatedBy, String reactivatedByName) {
        this.id = id;
        this.clientId = clientId;
        this.clientName = clientName;
        this.consentChannel = consentChannel;
        this.consentStatus = consentStatus;
        this.phoneNumber = phoneNumber;
        this.consentVersion = consentVersion;
        this.consentedAt = consentedAt;
        this.revokedAt = revokedAt;
        this.reactivatedAt = reactivatedAt;
        this.consentedBy = consentedBy;
        this.consentedByName = consentedByName;
        this.verifiedBy = verifiedBy;
        this.verifiedByName = verifiedByName;
        this.revokedBy = revokedBy;
        this.revokedByName = revokedByName;
        this.reactivatedBy = reactivatedBy;
        this.reactivatedByName = reactivatedByName;
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
        this.reactivatedBy= null;
        this.reactivatedAt = null;
        this.consentedByName = null;
        this.revokedByName = null;
        this.reactivatedByName = null;
        this.verifiedByName = null;
        this.narrations = narrations;
    }

    public static CrbConsentData instance(Long id, Long clientId, String clientName, Integer consentChannel, Integer consentStatus, String phoneNumber, String consentVersion, LocalDateTime consentedAt, LocalDateTime revokedAt,
                                          LocalDateTime reactivatedAt, Long consentedBy, String consentedByName, Long verifiedBy, String verifiedByName, Long revokedBy, String revokedByName, Long reactivatedBy, String reactivatedByName) {
        return new CrbConsentData(id, clientId, clientName,  getChannel(consentChannel), getStatus(consentStatus), phoneNumber, consentVersion, consentedAt,
                revokedAt, reactivatedAt, consentedBy, consentedByName, verifiedBy, verifiedByName, revokedBy, revokedByName, reactivatedBy, reactivatedByName
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
