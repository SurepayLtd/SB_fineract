package org.apache.fineract.portfolio.loanaccount.jobs.accrualperiodicposting;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.accounting.common.AccountingRuleType;
import org.apache.fineract.infrastructure.configuration.domain.ConfigurationDomainService;
import org.apache.fineract.infrastructure.core.service.DateUtils;
import org.apache.fineract.portfolio.loanaccount.data.LoanAccrualData;
import org.apache.fineract.portfolio.loanaccount.service.LoanAccrualBatchReadService;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.NonTransientResourceException;
import org.springframework.batch.item.ParseException;
import org.springframework.batch.item.UnexpectedInputException;
import org.apache.fineract.portfolio.loanaccount.jobs.paymentdueremider.ExecuteBatchJobConstant;

import java.time.LocalDate;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class PeriodicAccrualItemReader implements ItemReader<LoanAccrualData> {

    private final int pageSize;
    private static final String ACCRUAL_ON_CHARGE_SUBMITTED_ON_DATE = "submitted-date";

    private final ConfigurationDomainService configurationDomainService;
    private final LoanAccrualBatchReadService readService;


    private final Deque<LoanAccrualData> page = new ArrayDeque<>();

    private Long minAccountKey;
    private Long maxAccountKey;
    private Long afterLoanId;

    private Integer accountingType;
    private LocalDate tillDate;
    private boolean futureCharges;

    private boolean exhausted;

    @BeforeStep
    public void beforeStep(final StepExecution stepExecution) {
        this.minAccountKey = stepExecution.getExecutionContext().getLong(ExecuteBatchJobConstant.MIN_ACCOUNT_KEY);
        this.maxAccountKey = stepExecution.getExecutionContext().getLong(ExecuteBatchJobConstant.MAX_ACCOUNT_KEY);
        this.accountingType = AccountingRuleType.ACCRUAL_PERIODIC.getValue();
        this.tillDate = DateUtils.getBusinessLocalDate();
        this.futureCharges = !isChargeOnDueDate();
        this.exhausted = minAccountKey == 0L && maxAccountKey == 0L;
        log.info("Worker {} processing account range [{} - {}]", stepExecution.getExecutionContext().getString(ExecuteBatchJobConstant.PARTITION_KEY), minAccountKey, maxAccountKey);
    }

    @Override
    public LoanAccrualData read() throws Exception, UnexpectedInputException, ParseException, NonTransientResourceException {
        if (page.isEmpty() && !exhausted) {
            fetchNextPage();
        }

        return page.pollFirst();
    }


    private void fetchNextPage() {


        final List<LoanAccrualData> results = readService.retrieveLoanAccrualPage(minAccountKey, maxAccountKey, afterLoanId, pageSize, accountingType, tillDate, futureCharges);

        if (results.isEmpty()) {
            exhausted = true;
            return;
        }

        page.addAll(results);

        final LoanAccrualData last = results.get(results.size() - 1);

        afterLoanId = last.loanId();

        if (results.size() < pageSize) {
            exhausted = true;
        }
    }

    private boolean isChargeOnDueDate() {
        final String chargeAccrualDateType = configurationDomainService.getAccrualDateConfigForCharge();
        return !ACCRUAL_ON_CHARGE_SUBMITTED_ON_DATE.equalsIgnoreCase(chargeAccrualDateType);
    }
}
