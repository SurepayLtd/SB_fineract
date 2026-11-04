package org.apache.fineract.portfolio.crb.domain;




import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.fineract.infrastructure.core.domain.AbstractPersistableCustom;
import org.apache.fineract.portfolio.client.domain.CRBStatus;
import org.apache.fineract.portfolio.client.domain.Client;

@Entity
@Table(name = "m_crb_check")
@Getter
@Setter
public class CrbCheck extends AbstractPersistableCustom<Long> {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "consent_id", nullable = false)
    private CrbConsent consent;

    @Column(name = "validation_type", nullable = false)
    private Integer validationType;

    @Column(name = "request_type", nullable = false)
    private Integer requestType;

    @Column(name = "check_status", nullable = false)
    private Integer status;

    @Column(name = "provider_validation_id", length = 100, unique = true)
    private String providerValidationId;

    @Column(name = "provider_reference", length = 150, unique = true)
    private String providerReference;

    @Column(name = "cost", precision = 19, scale = 4)
    private BigDecimal cost;

    @Column(name = "currency", length = 10)
    private String currency;

    @Column(name = "request_payload", columnDefinition = "LONGTEXT")
    private String requestPayload;

    @Column(name = "response_payload", columnDefinition = "LONGTEXT")
    private String responsePayload;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "message", columnDefinition = "TEXT")
    private String message;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;


    @PrePersist()
    public void onCreate(){
        this.createdAt = LocalDateTime.now();
    }

    protected CrbCheck(){}

    public CrbCheck(Client client, CrbConsent consent, Integer validationType, Integer requestType, String providerValidationId, String providerReference, BigDecimal cost, String currency, String requestPayload, String responsePayload, String errorMessage, String message,
                    LocalDateTime completedAt) {
        this.client = client;
        this.consent = consent;
        this.validationType = validationType;
        this.requestType = requestType;
        this.status = CRBStatus.PENDING.getValue();
        this.providerValidationId = providerValidationId;
        this.providerReference = providerReference;
        this.cost = cost;
        this.currency = currency;
        this.requestPayload = requestPayload;
        this.responsePayload = responsePayload;
        this.errorMessage = errorMessage;
        this.message = message;
        this.completedAt = completedAt;
    }

    public static CrbCheck create(final Client client, final CrbConsent consent, final Integer type, final Integer requestType) {
        return new CrbCheck(client, consent, type, requestType, null, null, null, null, null, null, null, null, null);
    }

    public void complete(final Integer status, final String validationId, final String reference,
                         final BigDecimal cost, final String currency, final String responsePayload,
                         final String errorMessage, String message, final LocalDateTime completedAt) {
        this.status = status;
        this.providerValidationId = validationId;
        this.providerReference = reference;
        this.cost = cost;
        this.currency = currency;
        this.responsePayload = responsePayload;
        this.message = message;
        this.errorMessage = errorMessage;
        this.completedAt = completedAt;
    }
}
