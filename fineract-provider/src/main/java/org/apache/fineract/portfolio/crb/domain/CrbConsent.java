package org.apache.fineract.portfolio.crb.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;
import org.apache.fineract.infrastructure.codes.domain.CodeValue;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.domain.AbstractPersistableCustom;
import org.apache.fineract.portfolio.client.domain.Client;
import org.apache.fineract.useradministration.domain.AppUser;

@Entity
@Table(name = "m_crb_consent")
@Getter
@Setter
public class CrbConsent extends AbstractPersistableCustom<Long> {


    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "consented_by", nullable = false)
    private AppUser consentedBy;

    @Column(name = "consent_version", nullable = false, length = 50)
    private String consentVersion;

    @Column(name = "consent_text", nullable = false)
    private String consentText;

    @Column(name = "consent_channel", nullable = false)
    private Integer consentChannel;

    @Column(name = "consent_status", nullable = false)
    private Integer consentStatus;

    @Column(name = "consented_at", nullable = false)
    private LocalDateTime consentedAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "revoked_by")
    private AppUser revokedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "revoke_reason_cv_id")
    private CodeValue revokeReason;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    @Column(name = "phone_number", length = 30)
    private String phoneNumber;

    @Column(name = "otp_hash")
    private String otpHash;

    @Column(name = "otp_expires_at")
    private LocalDateTime otpExpiresAt;

    @Column(name = "otp_verified_at")
    private LocalDateTime otpVerifiedAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "verified_by")
    private AppUser verifiedBy;

    @Column(name = "otp_used", nullable = false)
    private boolean otpUsed;


    protected CrbConsent() {}

    public CrbConsent(Client client, AppUser consentedBy, String consentVersion, String consentText, Integer consentChannel, Integer consentStatus, String otpHash, LocalDateTime otpExpiresAt, LocalDateTime consentedAt) {
        this.client = client;
        this.consentedBy = consentedBy;
        this.consentVersion = consentVersion;
        this.consentText = consentText;
        this.consentChannel = consentChannel;
        this.consentStatus = consentStatus;
        this.phoneNumber = client != null ? client.mobileNo(): null ;
        this.otpHash = otpHash;
        this.otpExpiresAt = otpExpiresAt;
        this.otpUsed = false;
        this.consentedAt = consentedAt;
    }

    public static CrbConsent fromJson(JsonCommand command, Client client, AppUser appUser, Integer consentStatus, String otpHash, LocalDateTime otpExpiresAt, LocalDateTime consentedAt){
        String consentTextHash = command.stringValueOfParameterNamed("consentText");
        Integer consentChannel = command.integerValueOfParameterNamed("consentChannel");
        return new CrbConsent(client, appUser, "CRB_CONSENT_V1", consentTextHash, consentChannel, consentStatus, otpHash, otpExpiresAt, consentedAt);
    }


    public boolean isRevoked() {
        return revokedAt != null;
    }

    public void revoke(final AppUser user, final LocalDateTime revokedAt, CodeValue revokeReason) {
        this.revokedAt = revokedAt;
        this.revokedBy = user;
        this.consentedAt = null;
        this.consentStatus = ConsentStatus.REVOKED.getValue();
        this.revokeReason = revokeReason;
    }
}
