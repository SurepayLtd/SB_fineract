package org.apache.fineract.portfolio.loanaccount.jobs.applychargetooverdueloaninstallment;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.infrastructure.configuration.domain.ConfigurationDomainService;
import org.apache.fineract.portfolio.loanaccount.jobs.paymentdueremider.ExecuteBatchJobConstant;
import org.apache.fineract.portfolio.loanaccount.loanschedule.data.OverdueLoanScheduleData;
import org.apache.fineract.portfolio.loanaccount.service.LoanReadPlatformService;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.item.ItemReader;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class OverdueLoanItemReader implements ItemReader<OverdueLoanScheduleData> {

    private final LoanReadPlatformService readService;
    private final ConfigurationDomainService configurationDomainService;
    private final int pageSize;

    private final Deque<OverdueLoanScheduleData> page = new ArrayDeque<>();

    private Long minAccountKey;
    private Long maxAccountKey;

    private Long afterLoanId;
    private Integer afterInstallmentNumber;
    private Long afterChargeId;

    private boolean exhausted;

    @BeforeStep
    public void beforeStep(StepExecution stepExecution) {

        this.minAccountKey = stepExecution.getExecutionContext().getLong(ExecuteBatchJobConstant.MIN_ACCOUNT_KEY);

        this.maxAccountKey = stepExecution.getExecutionContext().getLong(ExecuteBatchJobConstant.MAX_ACCOUNT_KEY);

        this.afterLoanId = 0L;
        this.afterInstallmentNumber = 0;
        this.afterChargeId = 0L;

        this.exhausted = minAccountKey == 0L && maxAccountKey == 0L;

        log.info("Worker {} processing account range [{} - {}]",
                stepExecution.getExecutionContext().getString(ExecuteBatchJobConstant.PARTITION_KEY), minAccountKey, maxAccountKey);
    }

    @Override
    public OverdueLoanScheduleData read() {

        if (page.isEmpty() && !exhausted) {
            fetchNextPage();
        }

        return page.poll();
    }

    private void fetchNextPage() {

        final Long penaltyWaitPeriodValue = configurationDomainService.retrievePenaltyWaitPeriod();
        final Boolean backdatePenalties = configurationDomainService.isBackdatePenaltiesEnabled();

        final List<OverdueLoanScheduleData> results = readService.retrieveOverdueLoanPage(minAccountKey, maxAccountKey, afterLoanId, afterInstallmentNumber, pageSize, penaltyWaitPeriodValue, backdatePenalties, afterChargeId);

        log.info("Partition [{} - {}] fetched {} overdue installments using cursor [loanId={}, installment={}, chargeId={}]", minAccountKey, maxAccountKey, results.size(), afterLoanId, afterInstallmentNumber, afterChargeId);

        if (results.isEmpty()) {
            exhausted = true;
            return;
        }

        page.addAll(results);

        final OverdueLoanScheduleData last = results.get(results.size() - 1);

        afterLoanId = last.getLoanId();
        afterInstallmentNumber = last.getPeriodNumber();
        afterChargeId = last.getChargeId();

        log.info("Partition [{} - {}] next cursor [loanId={}, installment={}, chargeId={}]", minAccountKey, maxAccountKey, afterLoanId, afterInstallmentNumber, afterChargeId);


        if (results.size() < pageSize) {
            exhausted = true;
        }
    }
}
