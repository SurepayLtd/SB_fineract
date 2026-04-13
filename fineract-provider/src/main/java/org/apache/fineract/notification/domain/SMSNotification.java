package org.apache.fineract.notification.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import org.apache.fineract.infrastructure.core.domain.AbstractPersistableCustom;

@Getter
@Entity
@Table(name = "m_sms_credentials")
public class SMSNotification extends AbstractPersistableCustom<Long> {

    @Column(name = "sms_vendor_code")
    private String vendorCode;


    @Column(name = "sms_vendor_password")
    private String vendorPassword;

    protected SMSNotification(){}

    public SMSNotification(String vendorCode, String vendorPassword) {
        this.vendorCode = vendorCode;
        this.vendorPassword = vendorPassword;
    }
}
