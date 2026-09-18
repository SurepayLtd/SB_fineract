package org.apache.fineract.portfolio.loanaccount.jobs.paymentdueremider;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.infrastructure.springbatch.PropertyService;
import org.apache.fineract.portfolio.loanaccount.data.JobPartition;
import org.apache.fineract.portfolio.loanaccount.loanschedule.service.BatchSmSReadService;
import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.item.ExecutionContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Splits the loan installments into partitions that workers process in parallel.
 *
 * <p>
 * Partitions are cut over distinct source accounts rather than over installments, so every installment in a given loan
 * account stays in one partition and runs sequentially there. Two partitions therefore never contend on the same
 * source-account row*
 * </p>
 */
@Slf4j
@RequiredArgsConstructor
public class LoanInstallmentReminderPartitioner implements Partitioner {

    private final PropertyService propertyService;
    private final BatchSmSReadService readService;


    @Override
    public Map<String, ExecutionContext> partition(int gridSize) {

        final int partitionSize = propertyService.getPartitionSize(ExecuteBatchJobConstant.LOAN_INSTALLMENT_JOB_NAME);

        final List<JobPartition> partitions = new ArrayList<>(
                readService.retrieveLoanDuePartitions(partitionSize)
        );

        if (partitions.isEmpty()) {
            log.info("No payment installments is due today");
            partitions.add(new JobPartition(0L, 0L, 0L, 0L));
        }

        log.info("========== {} Partitions ==========", ExecuteBatchJobConstant.LOAN_INSTALLMENT_JOB_NAME);

        log.info("{} payment installments are due, split into {} partitions of at most {} source accounts",
                partitions.stream().mapToLong(JobPartition::partitionCount).sum(), partitions.size(), partitionSize);

        partitions.forEach(partition ->
                log.info("Partition {} -> Account Range [{} - {}], Records: {}", partition.pageNumber(), partition.minAccountKey(), partition.maxAccountKey(), partition.partitionCount()
                ));

        return partitions.stream().collect(Collectors.toMap(partition -> ExecuteBatchJobConstant.PARTITION_PREFIX + partition.pageNumber(), this::executionContextOf));

    }

    private ExecutionContext executionContextOf(final JobPartition partition) {
        final ExecutionContext executionContext = new ExecutionContext();
        executionContext.putLong(ExecuteBatchJobConstant.MIN_ACCOUNT_KEY, partition.minAccountKey());
        executionContext.putLong(ExecuteBatchJobConstant.MAX_ACCOUNT_KEY, partition.maxAccountKey());
        executionContext.putString(ExecuteBatchJobConstant.PARTITION_KEY, ExecuteBatchJobConstant.PARTITION_PREFIX + partition.pageNumber());
        return executionContext;
    }
}
