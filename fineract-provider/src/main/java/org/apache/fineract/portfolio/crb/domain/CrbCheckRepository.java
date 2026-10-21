package org.apache.fineract.portfolio.crb.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CrbCheckRepository extends JpaRepository<CrbCheck, Long>, JpaSpecificationExecutor<CrbCheck> {

    String FIND_HISTORY = "SELECT c FROM CrbCheck c WHERE c.client.id = :clientId AND c.validationType = :validationType ORDER BY c.id DESC ";

    String FIND_REVALIDATION_ID = " SELECT c FROM CrbCheck c WHERE c.client.id = :clientId AND c.validationType = :validationType AND c.requestType = :requestType AND c.providerValidationId IS NOT NULL ORDER BY c.id DESC ";

    @Query(FIND_HISTORY)
    List<CrbCheck> findHistory(@Param("clientId") Long clientId, @Param("validationType") Integer validationType);

    @Query(FIND_REVALIDATION_ID)
    List<CrbCheck> findHistoryWithProviderId(@Param("clientId") Long clientId, @Param("validationType") Integer validationType, @Param("requestType") Integer requestType);

}
