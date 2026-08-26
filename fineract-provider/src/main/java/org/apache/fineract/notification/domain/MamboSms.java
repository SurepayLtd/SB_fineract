package org.apache.fineract.notification.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import org.apache.fineract.infrastructure.core.domain.AbstractPersistableCustom;

@Entity
@Table(name = "m_mambo_sms_credentials")
@Getter
public class MamboSms extends AbstractPersistableCustom<Long> {

    @Column(name="tenant_id", nullable = false)
    private String tenantIdentifier;

    @Column(name="api_key", nullable = false)
    private String apiKey;

    protected MamboSms(){};

    public MamboSms(String tenantIdentifier, String apiKey) {
        this.tenantIdentifier = tenantIdentifier;
        this.apiKey = apiKey;
    }
}
