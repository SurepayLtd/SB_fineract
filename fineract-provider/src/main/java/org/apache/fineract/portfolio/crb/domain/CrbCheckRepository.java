package org.apache.fineract.portfolio.crb.domain;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CrbCheckRepository extends JpaRepository<CrbCheck, Long>, JpaSpecificationExecutor<CrbCheck> {

    String FIND_HISTORY = "SELECT c FROM CrbCheck c WHERE c.client.id = :clientId AND c.validationType = :validationType ORDER BY c.id DESC ";

    String FIND_REVALIDATION_ID = " SELECT c FROM CrbCheck c WHERE c.client.id = :clientId AND c.validationType = :validationType AND c.requestType = :requestType AND c.providerValidationId IS NOT NULL ORDER BY c.id DESC ";

    String FIND_REPORTS = " SELECT c FROM CrbCheck c WHERE c.client.id = :clientId AND c.validationType = :validationType AND c.requestType = :requestType AND c.status = :status ORDER BY c.createdAt DESC ";


    @Query(FIND_HISTORY)
    List<CrbCheck> findHistory(@Param("clientId") Long clientId, @Param("validationType") Integer validationType);

    @Query(FIND_REVALIDATION_ID)
    List<CrbCheck> findHistoryWithProviderId(@Param("clientId") Long clientId, @Param("validationType") Integer validationType, @Param("requestType") Integer requestType);

    @Query("""
    SELECT cc
    FROM CrbCheck cc
    WHERE cc.client.id = :clientId
      AND cc.validationType IN :validationTypes
      AND cc.requestType = :requestType
    """)
    List<CrbCheck> findInitialCreditEnquiryOrScore(@Param("clientId") Long clientId, @Param("validationTypes") List<Integer> validationTypes, @Param("requestType") Integer requestType);

    @Query("""
    SELECT cc
    FROM CrbCheck cc
    WHERE cc.client.id = :clientId
      AND cc.validationType = :validationType
      AND cc.status = :status
    ORDER BY cc.createdAt DESC
    """)
    List<CrbCheck> findLatestVerifiedCheck(@Param("clientId") Long clientId, @Param("validationType") Integer validationType, @Param("status") Integer status,
            Pageable pageable);

    @Query("""
        SELECT c
        FROM CrbCheck c
        WHERE c.client.id = :clientId
          AND c.validationType = :validationType
          AND c.status = :status
          AND (
              (:hasReportUrl = true AND c.reportUrl IS NOT NULL AND c.reportUrl <> '')
              OR
              (:hasReportUrl = false AND (c.reportUrl IS NULL OR c.reportUrl = ''))
          )
        ORDER BY c.createdAt DESC
        """)
    List<CrbCheck> findLatestVerifiedReportCheck(@Param("clientId") Long clientId, @Param("validationType") Integer validationType,
            @Param("status") Integer status, @Param("hasReportUrl") boolean hasReportUrl, Pageable pageable
    );

    @Query(FIND_REPORTS)
    List<CrbCheck> findReportsHistory(@Param("clientId") Long clientId, @Param("validationType") Integer validationType, @Param("requestType") Integer requestType, @Param("status") Integer status, Pageable pageable);


}
