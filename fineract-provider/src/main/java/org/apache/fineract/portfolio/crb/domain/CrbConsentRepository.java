package org.apache.fineract.portfolio.crb.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CrbConsentRepository extends JpaRepository<CrbConsent, Long>, JpaSpecificationExecutor<CrbConsent> {

    String FIND_CONSENT = "Select c from CrbConsent c where c.client.id = :clientId and c.consentStatus = :consentStatus ";

    @Query(FIND_CONSENT)
    CrbConsent findVerifiedClient(@Param("clientId") Long clientId, @Param("consentStatus") Integer consentStatus);

    boolean existsByClientId(Long clientId);

}
