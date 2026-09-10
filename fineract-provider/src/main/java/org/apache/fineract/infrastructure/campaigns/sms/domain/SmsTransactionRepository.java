package org.apache.fineract.infrastructure.campaigns.sms.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface SmsTransactionRepository extends JpaRepository<SmsTransaction, Long>, JpaSpecificationExecutor<SmsTransaction> {
}
