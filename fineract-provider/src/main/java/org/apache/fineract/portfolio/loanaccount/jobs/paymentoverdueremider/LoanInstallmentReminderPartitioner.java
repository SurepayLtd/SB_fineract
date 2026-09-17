package org.apache.fineract.portfolio.loanaccount.jobs.paymentoverdueremider;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.infrastructure.springbatch.PropertyService;
import org.apache.fineract.portfolio.loanaccount.data.LoanInstallmentReminderPartition;
import org.apache.fineract.portfolio.loanaccount.loanschedule.service.LoanInstallmentReminderReadService;
import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.item.ExecutionContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Splits the instructions due today into partitions that workers process in parallel.
 *
 * <p>
 * Partitions are cut over distinct source accounts rather than over instructions, so every instruction debiting a given
 * account stays in one partition and runs sequentially there. Two partitions therefore never contend on the same
 * source-account row. Instructions crediting the same destination can still land in different partitions; that
 * contention is left to the step's retry, being far less common and much shorter-lived than the debit-side check.
 * </p>
 */
@Slf4j
@RequiredArgsConstructor
public class LoanInstallmentReminderPartitioner implements Partitioner {

    private final PropertyService propertyService;
    private final LoanInstallmentReminderReadService readService;


    @Override
    public Map<String, ExecutionContext> partition(int gridSize) {

        final int partitionSize = propertyService.getPartitionSize(ExecuteInstallmentReminderConstant.JOB_NAME);

        final List<LoanInstallmentReminderPartition> partitions = new ArrayList<>(
                readService.retrieveDuePartitions(partitionSize)
        );

        if (partitions.isEmpty()) {
            // A step needs at least one partition to run at all, so hand it an account range that matches nothing
            // rather than an empty map: the run then completes normally having done no work.
            log.info("No payment installments is due today");
            partitions.add(new LoanInstallmentReminderPartition(0L, 0L, 0L, 0L));
        }
        log.info("{} payment installments are due, split into {} partitions of at most {} source accounts",
                partitions.stream().mapToLong(LoanInstallmentReminderPartition::installmentCount).sum(), partitions.size(), partitionSize);

        return partitions.stream().collect(Collectors.toMap(partition -> ExecuteInstallmentReminderConstant.PARTITION_PREFIX + partition.pageNumber(), this::executionContextOf));

    }

    private ExecutionContext executionContextOf(final LoanInstallmentReminderPartition partition) {
        final ExecutionContext executionContext = new ExecutionContext();
        executionContext.putLong(ExecuteInstallmentReminderConstant.MIN_ACCOUNT_KEY, partition.minAccountKey());
        executionContext.putLong(ExecuteInstallmentReminderConstant.MAX_ACCOUNT_KEY, partition.maxAccountKey());
        executionContext.putString(ExecuteInstallmentReminderConstant.PARTITION_KEY, ExecuteInstallmentReminderConstant.PARTITION_PREFIX + partition.pageNumber());
        return executionContext;
    }
}
