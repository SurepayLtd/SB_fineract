package org.apache.fineract.portfolio.savings.jobs.updatesavingsdormantaccounts;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.portfolio.loanaccount.jobs.paymentdueremider.ExecuteBatchJobConstant;
import org.apache.fineract.portfolio.loanaccount.loanschedule.service.BatchSmSReadService;
import org.apache.fineract.portfolio.savings.data.SavingsDormancyReminderData;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.NonTransientResourceException;
import org.springframework.batch.item.ParseException;
import org.springframework.batch.item.UnexpectedInputException;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class SavingsDormancyItemReader implements ItemReader<SavingsDormancyReminderData> {


    private final BatchSmSReadService readService;
    private final int pageSize;

    private final Deque<SavingsDormancyReminderData> page = new ArrayDeque<>();

    private Long minAccountKey;
    private Long maxAccountKey;

    private Integer afterReminderDays;
    private Long afterSavingsId;

    private boolean exhausted;

    @BeforeStep
    public void beforeStep(StepExecution stepExecution) {
        this.minAccountKey = stepExecution.getExecutionContext().getLong(ExecuteBatchJobConstant.MIN_ACCOUNT_KEY);
        this.maxAccountKey = stepExecution.getExecutionContext().getLong(ExecuteBatchJobConstant.MAX_ACCOUNT_KEY);
        this.exhausted = minAccountKey == 0L && maxAccountKey == 0L;
        log.info("Worker {} processing account range [{} - {}]",
                stepExecution.getExecutionContext().getString(ExecuteBatchJobConstant.PARTITION_KEY), minAccountKey, maxAccountKey);
    }

    @Override
    public SavingsDormancyReminderData read() throws Exception, UnexpectedInputException, ParseException, NonTransientResourceException {

        if (page.isEmpty() && !exhausted) {
            fetchNextPage();
        }

        return page.poll();
    }

    private void fetchNextPage() {

        List<SavingsDormancyReminderData> results = readService.retrieveDormancyPage(minAccountKey, maxAccountKey, afterReminderDays, afterSavingsId, pageSize);

        if (results.isEmpty()) {
            exhausted = true;
            return;
        }

        page.addAll(results);

        SavingsDormancyReminderData last = results.get(results.size() - 1);
        afterReminderDays = last.reminderDays();
        afterSavingsId = last.savingsId();

        if (results.size() < pageSize) {
            exhausted = true;
        }

        log.debug("Loaded {} savings dormant reminders for account range {} - {}", results.size(), minAccountKey, maxAccountKey);

    }
}
