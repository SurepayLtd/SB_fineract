package org.apache.fineract.portfolio.account.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface StandingInstructionHistoryRepository extends JpaRepository<StandingInstructionHistory, Long>, JpaSpecificationExecutor<StandingInstructionHistory> {
}
