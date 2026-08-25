package org.apache.fineract.notification.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface MamboSmsRepository extends JpaRepository<MamboSms, Long>, JpaSpecificationExecutor<MamboSms> {
}
