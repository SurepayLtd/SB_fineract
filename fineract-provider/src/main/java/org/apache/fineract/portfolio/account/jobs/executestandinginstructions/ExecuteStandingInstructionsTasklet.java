/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.fineract.portfolio.account.jobs.executestandinginstructions;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.infrastructure.core.domain.ExternalId;
import org.apache.fineract.infrastructure.core.exception.AbstractPlatformServiceUnavailableException;
import org.apache.fineract.infrastructure.core.exception.PlatformApiDataValidationException;
import org.apache.fineract.infrastructure.core.service.DateUtils;
import org.apache.fineract.portfolio.account.data.AccountTransferDTO;
import org.apache.fineract.portfolio.account.data.StandingInstructionData;
import org.apache.fineract.portfolio.account.data.StandingInstructionDuesData;
import org.apache.fineract.portfolio.account.domain.AccountTransferRecurrenceType;
import org.apache.fineract.portfolio.account.domain.StandingInstructionStatus;
import org.apache.fineract.portfolio.account.domain.StandingInstructionType;
import org.apache.fineract.portfolio.account.service.StandingInstructionHistoryWriteService;
import org.apache.fineract.portfolio.account.service.StandingInstructionReadPlatformService;
import org.apache.fineract.portfolio.common.domain.PeriodFrequencyType;
import org.apache.fineract.portfolio.loanaccount.loanschedule.domain.DefaultScheduledDateGenerator;
import org.apache.fineract.portfolio.loanaccount.loanschedule.domain.ScheduledDateGenerator;
import org.apache.fineract.portfolio.savings.domain.SavingsAccount;
import org.apache.fineract.portfolio.savings.exception.InsufficientAccountBalanceException;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@Slf4j
@RequiredArgsConstructor
public class ExecuteStandingInstructionsTasklet implements Tasklet {

    private final StandingInstructionReadPlatformService standingInstructionReadPlatformService;
    private final StandingInstructionHistoryWriteService standingInstructionHistoryWriteService;

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {

        int page = 0;
        while (true) {
            Pageable pageable = PageRequest.of(page, 100);

            Collection<StandingInstructionData> instructionData = standingInstructionReadPlatformService
                    .retrieveAll(pageable, StandingInstructionStatus.ACTIVE.getValue());

            if(instructionData.isEmpty()){
                break;
            }
            int executed = 0;
            int successful = 0;
            int failed = 0;
            int errors = 0;
            // Track errors per instruction, but do not stop processing others
            for (StandingInstructionData data : instructionData) {
                try {
                    boolean isDueForTransfer = false;
                    AccountTransferRecurrenceType recurrenceType = data.recurrenceType();
                    StandingInstructionType instructionType = data.instructionType();
                    LocalDate transactionDate = DateUtils.getBusinessLocalDate();
                    if (recurrenceType.isPeriodicRecurrence()) {
                        final ScheduledDateGenerator scheduledDateGenerator = new DefaultScheduledDateGenerator();
                        PeriodFrequencyType frequencyType = data.recurrenceFrequency();
                        LocalDate startDate = data.validFrom();
                        if (frequencyType.isMonthly()) {
                            startDate = startDate.withDayOfMonth(data.recurrenceOnDay());
                            if (DateUtils.isBefore(startDate, data.validFrom())) {
                                startDate = startDate.plusMonths(1);
                            }
                        } else if (frequencyType.isYearly()) {
                            startDate = startDate.withDayOfMonth(data.recurrenceOnDay()).withMonth(data.recurrenceOnMonth());
                            if (DateUtils.isBefore(startDate, data.validFrom())) {
                                startDate = startDate.plusYears(1);
                            }
                        }
                        isDueForTransfer = scheduledDateGenerator.isDateFallsInSchedule(frequencyType, data.recurrenceInterval(), startDate,
                                transactionDate);

                    }
                    BigDecimal transactionAmount = data.amount();
                    if (data.toAccountType().isLoanAccount() && (recurrenceType.isDuesRecurrence() || (isDueForTransfer && instructionType.isDuesAmoutTransfer()))) {
                        StandingInstructionDuesData standingInstructionDuesData = standingInstructionReadPlatformService
                                .retriveLoanDuesData(data.toAccount().getId());
                        if (data.instructionType().isDuesAmoutTransfer()) {
                            transactionAmount = standingInstructionDuesData.totalDueAmount();
                        }
                        if (recurrenceType.isDuesRecurrence()) {
                            isDueForTransfer = isDueForTransfer(standingInstructionDuesData);
                        }
                    }

                    if (isDueForTransfer && transactionAmount != null && transactionAmount.compareTo(BigDecimal.ZERO) > 0) {
                        executed++;
                        if (executeInstruction(data, transactionAmount, transactionDate)) {
                            successful++;
                        } else {
                            failed++;
                        }
                    }
                } catch (Exception ex) {
                    // Log and collect error, but continue with next instruction
                    log.error("Error processing standing instruction id {}: {}", data.getId(), ex.getMessage(), ex);
                    errors ++;
                }
            }
            log.info("Standing instruction execution finished: executed={}, succeeded={}, failed={}, errors={}", executed, successful, failed, errors);
            page ++;
        }
        return RepeatStatus.FINISHED;
    }

    /**
     * Executes one instruction with full isolation. The transfer runs in its own transaction (rolls back only itself on
     * failure) and the outcome is recorded in a separate committed transaction, so a failing instruction never reverts
     * a sibling and always leaves a durable history row. Returns {@code true} on success.
     */
    private boolean executeInstruction(final StandingInstructionData data, final BigDecimal transactionAmount, final LocalDate transactionDate) {
        final SavingsAccount fromSavingsAccount = null;
        final boolean isRegularTransaction = true;
        final boolean isExceptionForBalanceCheck = false;
        AccountTransferDTO accountTransferDTO = new AccountTransferDTO(transactionDate, transactionAmount,
                data.fromAccountType(), data.toAccountType(), data.fromAccount().getId(), data.toAccount().getId(),
                data.name() + " Standing instruction transfer ", null, null, null, null, data.toTransferType(), null, null,
                data.transferType().getValue(), null, null, ExternalId.empty(), null, null, fromSavingsAccount,
                isRegularTransaction, isExceptionForBalanceCheck);
        try {
            standingInstructionHistoryWriteService.transferFunds(accountTransferDTO, data.getId(), transactionDate, transactionAmount);
            return true;
        } catch (final InsufficientAccountBalanceException e) {
            recordFailure(data, "InsufficientAccountBalance Exception ", transactionDate, e);
        } catch (final PlatformApiDataValidationException e) {
            recordFailure(data, "Validation exception while transfer of funds " + e.getDefaultUserMessage(), transactionDate, e);
        } catch (final AbstractPlatformServiceUnavailableException e) {
            recordFailure(data, "Platform exception while transfer of funds " + e.getDefaultUserMessage(), transactionDate, e);
        } catch (final RuntimeException e) {
            recordFailure(data, "Exception while transfer of funds " + e.getMessage(), transactionDate, e);
        }
        return false;
    }

    private void recordFailure(final StandingInstructionData data, final String errorLog, final LocalDate transactionDate, final RuntimeException cause) {
        log.error("Standing instruction {} (from {} to {}) failed: {}", data.getId(), data.fromAccount().getId(),
                data.toAccount().getId(), errorLog, cause);
        try {
            standingInstructionHistoryWriteService.recordFailure(data.getId(), errorLog, transactionDate);
        } catch (final RuntimeException e) {
            // A history-write failure must never abort the remaining instructions; log and continue.
            log.error("Failed to record failure history for standing instruction {}", data.getId(), e);
        }
    }

    public boolean isDueForTransfer(StandingInstructionDuesData standingInstructionDuesData) {
        return standingInstructionDuesData.dueDate() != null
                && !standingInstructionDuesData.dueDate().isAfter(LocalDate.now(DateUtils.getDateTimeZoneOfTenant()));
    }
}
