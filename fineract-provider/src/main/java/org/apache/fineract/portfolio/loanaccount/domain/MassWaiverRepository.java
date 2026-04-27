package org.apache.fineract.portfolio.loanaccount.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface MassWaiverRepository extends JpaRepository<MassWaiver, Long>, JpaSpecificationExecutor<MassWaiver> {
}
