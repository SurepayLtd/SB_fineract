package org.apache.fineract.notification.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface SMSNotificationRepository extends JpaRepository<SMSNotification, Long>, JpaSpecificationExecutor<SMSNotification> {
}
