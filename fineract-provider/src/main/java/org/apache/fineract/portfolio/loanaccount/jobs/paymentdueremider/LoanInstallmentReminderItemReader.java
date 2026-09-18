package org.apache.fineract.portfolio.loanaccount.jobs.paymentdueremider;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.portfolio.loanaccount.data.LoanInstallmentReminderData;
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

/**
 * Reads the due installment of one partition, a page at a time.
 *
 * <p>
 * The page is a keyset over the {@code (priority, id)} sort key rather than an offset, because executing an instruction
 * stamps its {@code last_run_date} and so removes it from the due set: an offset would step over unprocessed rows every
 * time a page committed. Only one page is held in memory at a time, whatever the size of the due set.
 * </p>
 */
@Slf4j
@RequiredArgsConstructor
public class LoanInstallmentReminderItemReader implements ItemReader<LoanInstallmentReminderData> {

    private final Deque<LoanInstallmentReminderData> page = new ArrayDeque<>();
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
    public LoanInstallmentReminderData read() throws Exception, UnexpectedInputException, ParseException, NonTransientResourceException {
        if (page.isEmpty() && !exhausted) {
            fetchNextPage();
        }
        return page.poll();
    }

    private void fetchNextPage() {

        final List<LoanInstallmentReminderData> results = readService.retrieveLoanDuePage(minAccountKey, maxAccountKey, afterDueDate, afterId, pageSize);

        log.info("Result Installments: {}", results);

        if (results.isEmpty()) {
            exhausted = true;
            return;
        }

        page.addAll(results);

        final LoanInstallmentReminderData last = results.getLast(); //get(results.size() - 1);

        afterDueDate = last.dueDate();
        afterId = last.installmentId();

        if (results.size() < pageSize) {
            exhausted = true;
        }

        log.debug("Loaded {} loan installment reminders for account range {} - {}", results.size(), minAccountKey, maxAccountKey);
    }

}
