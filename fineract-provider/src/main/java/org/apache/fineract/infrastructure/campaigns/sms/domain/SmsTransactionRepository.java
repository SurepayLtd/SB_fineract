package org.apache.fineract.infrastructure.campaigns.sms.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface SmsTransactionRepository extends JpaRepository<SmsTransaction, Long>, JpaSpecificationExecutor<SmsTransaction> {

    @Query("""
        SELECT COUNT(s) FROM SmsTransaction s
        WHERE s.mobileNumber = :mobileNumber
          AND s.smsEvent = :smsEvent
          AND s.sentAt >= :startDate
          AND s.sentAt < :endDate
        """)
    Long existsBirthdaySms(@Param("mobileNumber") String mobileNumber, @Param("smsEvent") String smsEvent,
                              @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate
    );
}
