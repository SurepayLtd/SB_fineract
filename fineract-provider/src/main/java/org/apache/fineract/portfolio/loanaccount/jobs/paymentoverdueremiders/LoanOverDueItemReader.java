package org.apache.fineract.portfolio.loanaccount.jobs.paymentoverdueremiders;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.portfolio.loanaccount.data.LoanInstallmentOverdueReminderData;
import org.apache.fineract.portfolio.loanaccount.data.LoanInstallmentReminderData;
import org.apache.fineract.portfolio.loanaccount.jobs.paymentdueremider.ExecuteBatchJobConstant;
import org.apache.fineract.portfolio.loanaccount.loanschedule.service.BatchSmSReadService;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.NonTransientResourceException;
import org.springframework.batch.item.ParseException;
import org.springframework.batch.item.UnexpectedInputException;

import java.time.LocalDate;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

@RequiredArgsConstructor
@Slf4j
public class LoanOverDueItemReader implements ItemReader<LoanInstallmentOverdueReminderData> {

    private final Deque<LoanInstallmentOverdueReminderData> page = new ArrayDeque<>();
    private final BatchSmSReadService readService;

    private final int pageSize;


    private Long minAccountKey;
    private Long maxAccountKey;
    private LocalDate afterDueDate;
    private Long afterId;
    private boolean exhausted;

    @BeforeStep
    public void beforeStep(final StepExecution stepExecution) {
        this.minAccountKey = stepExecution.getExecutionContext().getLong(ExecuteBatchJobConstant.MIN_ACCOUNT_KEY);
        this.maxAccountKey = stepExecution.getExecutionContext().getLong(ExecuteBatchJobConstant.MAX_ACCOUNT_KEY);
        this.exhausted = minAccountKey == 0L && maxAccountKey == 0L;

        log.info("Worker {} processing account range [{} - {}]",
                stepExecution.getExecutionContext().getString(ExecuteBatchJobConstant.PARTITION_KEY), minAccountKey, maxAccountKey);
    }

    @Override
    public LoanInstallmentOverdueReminderData read() throws Exception, UnexpectedInputException, ParseException, NonTransientResourceException {
        if (page.isEmpty() && !exhausted) {
            fetchNextPage();
        }
        return page.poll();
    }

    private void fetchNextPage() {

        final List<LoanInstallmentOverdueReminderData> results = readService.retrieveLoanOverDuePage(minAccountKey, maxAccountKey, afterDueDate, afterId, pageSize);

        log.info("Result Overdues: {}", results);

        if (results.isEmpty()) {
            exhausted = true;
            return;
        }

        page.addAll(results);

        final LoanInstallmentOverdueReminderData last = results.get(results.size() - 1); //getlast();

        afterDueDate = last.dueDate();
        afterId = last.installmentId();

        if (results.size() < pageSize) {
            exhausted = true;
        }

        log.debug("Loaded {} loan overdues reminders for account range {} - {}", results.size(), minAccountKey, maxAccountKey);
    }
}
