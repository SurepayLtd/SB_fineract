package org.apache.fineract.portfolio.loanaccount.jobs.applychargetooverdueloaninstallment;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.infrastructure.configuration.domain.ConfigurationDomainService;
import org.apache.fineract.portfolio.loanaccount.data.JobPartition;
import org.apache.fineract.portfolio.loanaccount.jobs.paymentdueremider.ExecuteBatchJobConstant;
import org.apache.fineract.portfolio.loanaccount.service.LoanReadPlatformService;
import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.item.ExecutionContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
public class OverdueLoanPartitioner implements Partitioner {

    private static final int PARTITION_SIZE = 100;

    private final LoanReadPlatformService readService;
    private final ConfigurationDomainService configurationDomainService;

    @Override
    public Map<String, ExecutionContext> partition(int gridSize) {

        final Long penaltyWaitPeriodValue = configurationDomainService.retrievePenaltyWaitPeriod();
        final Boolean backdatePenalties = configurationDomainService.isBackdatePenaltiesEnabled();


        final List<JobPartition> partitions = new ArrayList<>(
                readService.retrieveOverdueLoanPartitions(PARTITION_SIZE, penaltyWaitPeriodValue, backdatePenalties));

        if (partitions.isEmpty()){
            log.info("No accrual periodic today");
            partitions.add(new JobPartition(0L, 0L, 0L, 0L));
        }

        log.info("========== {} Partitions ==========", ExecuteBatchJobConstant.APPLY_CHARGE_TO_OVERDUE_LOAN_INSTALLMENT_JOB_NAME);

        log.info("{} overdue loan accounts found, split into {} partitions of at most {} source accounts",
                partitions.stream().mapToLong(JobPartition::partitionCount).sum(), partitions.size(), PARTITION_SIZE);

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
