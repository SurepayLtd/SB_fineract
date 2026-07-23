package org.apache.fineract.portfolio.account.service;

import org.apache.fineract.portfolio.account.data.AccountTransferDTO;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Executes a single standing instruction with per-instruction transactional isolation.
 * Each method runs in its own {@code REQUIRES_NEW} transaction so that a failing instruction can never mark a sibling's
 * transaction rollback-only. A successful execution (transfer + {@code last_run_date} stamp + success history) is one
 * atomic boundary; a failure is recorded in a separate committed boundary, so a rolled-back transfer still leaves a
 * durable {@code failed} history record.
 */
public interface StandingInstructionHistoryWriteService {
    void transferFunds(final AccountTransferDTO accountTransferDTO, final Long standingInstructionId, final LocalDate transactionDate, final BigDecimal transferredAmount);
    void recordFailure(final Long standingInstructionId, final String errorLog, final LocalDate transactionDate);
}
