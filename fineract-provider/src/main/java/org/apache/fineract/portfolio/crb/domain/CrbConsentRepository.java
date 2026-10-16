package org.apache.fineract.portfolio.crb.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface CrbConsentRepository extends JpaRepository<CrbConsent, Long>, JpaSpecificationExecutor<CrbConsent> {

}
