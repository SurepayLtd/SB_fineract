package org.apache.fineract.portfolio.loanaccount.jobs.addperiodicaccrualentries;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.accounting.common.AccountingRuleType;
import org.apache.fineract.infrastructure.configuration.domain.ConfigurationDomainService;
import org.apache.fineract.infrastructure.core.service.DateUtils;
import org.apache.fineract.portfolio.loanaccount.data.LoanAccrualPartition;
import org.apache.fineract.portfolio.loanaccount.jobs.paymentdueremider.ExecuteBatchJobConstant;
import org.apache.fineract.portfolio.loanaccount.service.LoanAccrualBatchReadService;
import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.item.ExecutionContext;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Slf4j
public class PeriodicAccrualItemPartitioner implements Partitioner {

    private static final int PARTITION_SIZE = 100;
    private static final String ACCRUAL_ON_CHARGE_SUBMITTED_ON_DATE = "submitted-date";


    private final LoanAccrualBatchReadService loanAccrualBatchReadService;
    private final ConfigurationDomainService configurationDomainService;

    @Override
    public Map<String, ExecutionContext> partition(int gridSize) {

        final Integer accountingType = AccountingRuleType.ACCRUAL_PERIODIC.getValue();

        final LocalDate tillDate = DateUtils.getBusinessLocalDate();

        final boolean futureCharges = !isChargeOnDueDate();

        List<LoanAccrualPartition> partitions = new ArrayList<>(
                loanAccrualBatchReadService.retrieveLoanAccrualPartitions(PARTITION_SIZE, accountingType, tillDate, futureCharges)
        );

        if (partitions.isEmpty()){
            log.info("No accrual periodic today");
            partitions.add(new LoanAccrualPartition(0L, 0L, 0L, 0L));
        }

        log.info("========== {} Partitions ==========", ExecuteBatchJobConstant.PERIODIC_ACCRUAL_JOB_NAME);

        log.info("{} periodic accruals are due, split into {} partitions of at most {} source accounts",
                partitions.stream().mapToLong(LoanAccrualPartition::partitionCount).sum(), partitions.size(), PARTITION_SIZE);

        partitions.forEach(partition ->
                log.info("Partition {} -> Account Range [{} - {}], Records: {}", partition.pageNumber(), partition.minAccountKey(), partition.maxAccountKey(), partition.partitionCount()
                ));

        return partitions.stream().collect(Collectors.toMap(partition -> ExecuteBatchJobConstant.PARTITION_PREFIX + partition.pageNumber(), this::executionContextOf));

    }

    private ExecutionContext executionContextOf(final LoanAccrualPartition partition) {
        final ExecutionContext executionContext = new ExecutionContext();
        executionContext.putLong(ExecuteBatchJobConstant.MIN_ACCOUNT_KEY, partition.minAccountKey());
        executionContext.putLong(ExecuteBatchJobConstant.MAX_ACCOUNT_KEY, partition.maxAccountKey());
        executionContext.putString(ExecuteBatchJobConstant.PARTITION_KEY, ExecuteBatchJobConstant.PARTITION_PREFIX + partition.pageNumber());
        return executionContext;
    }

    private boolean isChargeOnDueDate() {
        final String chargeAccrualDateType = configurationDomainService.getAccrualDateConfigForCharge();
        return !ACCRUAL_ON_CHARGE_SUBMITTED_ON_DATE.equalsIgnoreCase(chargeAccrualDateType);
    }
}
