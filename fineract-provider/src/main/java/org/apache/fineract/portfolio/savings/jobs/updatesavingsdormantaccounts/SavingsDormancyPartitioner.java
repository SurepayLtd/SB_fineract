package org.apache.fineract.portfolio.savings.jobs.updatesavingsdormantaccounts;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.portfolio.loanaccount.data.JobPartition;
import org.apache.fineract.portfolio.loanaccount.jobs.paymentdueremider.ExecuteBatchJobConstant;
import org.apache.fineract.portfolio.loanaccount.loanschedule.service.BatchSmSReadService;
import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.item.ExecutionContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
public class SavingsDormancyPartitioner implements Partitioner {

    private static final int PARTITION_SIZE = 100;

    private final BatchSmSReadService readService;

    @Override
    public Map<String, ExecutionContext> partition(int gridSize) {

        final List<JobPartition> partitions = new ArrayList<>(readService.retrieveDormantPartitions(PARTITION_SIZE));

        if (partitions.isEmpty()) {
            log.info("No dormant savings accounts  today");
            partitions.add(new JobPartition(0L, 0L, 0L, 0L));
        }
        log.info("========== {} Partitions ==========", ExecuteBatchJobConstant.SAVINGS_DORMANCY_JOB_NAME);

        log.info("{} dormant savings account, split into {} partitions of at most {} source accounts",
                partitions.stream().mapToLong(JobPartition::partitionCount).sum(), partitions.size(), PARTITION_SIZE);

        partitions.forEach(partition -> log.info("Partition {} -> Account Range [{} - {}], Records: {}", partition.pageNumber(), partition.minAccountKey(), partition.maxAccountKey(), partition.partitionCount()));

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
