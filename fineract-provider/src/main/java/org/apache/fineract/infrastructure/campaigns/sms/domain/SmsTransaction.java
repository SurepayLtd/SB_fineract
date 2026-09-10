package org.apache.fineract.infrastructure.campaigns.sms.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import org.apache.fineract.infrastructure.core.domain.AbstractPersistableCustom;

import java.time.LocalDateTime;

@Entity
@Table(name = "sms_transactions")
@Getter
public class SmsTransaction extends AbstractPersistableCustom<Long> {

    @Column(name = "sms_event", nullable = false)
    private String smsEvent;

    @Column(name = "mobile_number", nullable = false)
    private String mobileNumber;

    @Column(name = "sms_id")
    private String smsId;

    @Column(name = "sms_status", nullable = false)
    private String smsStatus;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "message_requirement")
    private String requirement;

    @Column(name = "message_length")
    private Integer messageLength;

    @Column(name = "sms_parts")
    private Integer smsParts;

    @Column(name = "sms_provider", nullable = false)
    private String smsProvider;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "failed_at")
    private LocalDateTime failedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    private void onCreate(){
        this.createdAt = LocalDateTime.now();
    }

    protected SmsTransaction(){}

    private SmsTransaction(String smsEvent, String mobileNumber, String smsId, String smsStatus, String errorMessage,
                          Integer messageLength, String smsProvider, String requirement, LocalDateTime sentAt, LocalDateTime failedAt) {
        this.smsEvent = smsEvent;
        this.mobileNumber = mobileNumber;
        this.smsId = smsId;
        this.smsStatus = smsStatus;
        this.errorMessage = errorMessage;
        this.messageLength = messageLength;
        this.smsParts = determineParts(messageLength);
        this.smsProvider = smsProvider;
        this.requirement = requirement;
        this.sentAt = sentAt;
        this.failedAt = failedAt;
    }

    public static SmsTransaction createSmsTransaction(String smsEvent, String mobileNumber, String smsId, Integer messageLength, String smsProvider, String requirement){
        String status = SmsTransactionStatus.SENT.getDescription();
        return new SmsTransaction(smsEvent, mobileNumber, smsId, status, null, messageLength, smsProvider, requirement, LocalDateTime.now(), null);
    }

    public static SmsTransaction recordSmsFailure(String smsEvent, String mobileNumber, String smsId, String errorMessage, Integer messageLength, String smsProvider, String requirement){
        String status = SmsTransactionStatus.FAILED.getDescription();
        return new SmsTransaction(smsEvent, mobileNumber, smsId, status, errorMessage, messageLength, smsProvider, requirement, null, LocalDateTime.now());
    }

    private Integer determineParts(final Integer messageLength) {
        if (messageLength == null || messageLength <= 0) {
            return 0;
        }

        return (int) Math.ceil(messageLength / 160.0);
    }
}
