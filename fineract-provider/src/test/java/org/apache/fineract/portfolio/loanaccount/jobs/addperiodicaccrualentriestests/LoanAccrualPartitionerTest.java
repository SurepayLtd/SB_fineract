package org.apache.fineract.portfolio.loanaccount.jobs.addperiodicaccrualentriestests;


import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.anyBoolean;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.fineract.accounting.common.AccountingRuleType;
import org.apache.fineract.infrastructure.businessdate.domain.BusinessDateType;
import org.apache.fineract.infrastructure.configuration.domain.ConfigurationDomainService;
import org.apache.fineract.infrastructure.core.domain.ActionContext;
import org.apache.fineract.infrastructure.core.domain.FineractPlatformTenant;
import org.apache.fineract.infrastructure.core.service.ThreadLocalContextUtil;
import org.apache.fineract.portfolio.loanaccount.data.LoanAccrualPartition;
import org.apache.fineract.portfolio.loanaccount.jobs.addperiodicaccrualentries.PeriodicAccrualItemPartitioner;
import org.apache.fineract.portfolio.loanaccount.jobs.paymentdueremider.ExecuteBatchJobConstant;
import org.apache.fineract.portfolio.loanaccount.service.LoanAccrualBatchReadService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.item.ExecutionContext;

public class LoanAccrualPartitionerTest {

    private LoanAccrualBatchReadService readService;
    private PeriodicAccrualItemPartitioner partitioner;
    private ConfigurationDomainService configurationDomainService;

    private static final LocalDate BUSINESS_DATE = LocalDate.of(2026, 9, 23);
    private static final Integer accountingType = AccountingRuleType.ACCRUAL_PERIODIC.getValue();


    @BeforeEach
    void setUp() {

        ThreadLocalContextUtil.setTenant(new FineractPlatformTenant(1L, "default", "Default", "Asia/Kolkata", null));

        ThreadLocalContextUtil.setActionContext(ActionContext.DEFAULT);

        ThreadLocalContextUtil.setBusinessDates(new HashMap<>(
                Map.of(BusinessDateType.BUSINESS_DATE, BUSINESS_DATE, BusinessDateType.COB_DATE, BUSINESS_DATE.minusDays(1))));

        readService = mock(LoanAccrualBatchReadService.class);
        configurationDomainService = mock(ConfigurationDomainService.class);

        partitioner = new PeriodicAccrualItemPartitioner(readService, configurationDomainService);
    }

    @AfterEach
    void tearDown() {
        ThreadLocalContextUtil.reset();
    }

    @Test
    void eachPartitionCarriesItsOwnAccountRange() {

        when(readService.retrieveLoanAccrualPartitions(100, accountingType, BUSINESS_DATE, false))
                .thenReturn(List.of(
                        new LoanAccrualPartition(1L, 100L, 0L, 100L),
                        new LoanAccrualPartition(101L, 200L, 1L, 100L),
                        new LoanAccrualPartition(201L, 250L, 2L, 50L)
                        ));

        var partitions = partitioner.partition(4);

        assertThat(partitions).containsOnlyKeys("partition_0", "partition_1", "partition_2");

        ExecutionContext first = partitions.get("partition_0");

        assertThat(first.getLong(ExecuteBatchJobConstant.MIN_ACCOUNT_KEY)).isEqualTo(1L);
        assertThat(first.getLong(ExecuteBatchJobConstant.MAX_ACCOUNT_KEY)).isEqualTo(100L);

        ExecutionContext second = partitions.get("partition_1");

        assertThat(second.getLong(ExecuteBatchJobConstant.MIN_ACCOUNT_KEY)).isEqualTo(101L);
        assertThat(second.getLong(ExecuteBatchJobConstant.MAX_ACCOUNT_KEY)).isEqualTo(200L);

        ExecutionContext third = partitions.get("partition_2");

        assertThat(third.getLong(ExecuteBatchJobConstant.MIN_ACCOUNT_KEY)).isEqualTo(201L);
        assertThat(third.getLong(ExecuteBatchJobConstant.MAX_ACCOUNT_KEY)).isEqualTo(250L);
    }

    @Test
    void shouldCreateDummyPartitionWhenNoLoansFound() {

        when(readService.retrieveLoanAccrualPartitions(anyInt(), anyInt(), any(), anyBoolean()))
                .thenReturn(List.of());

        var partitions = partitioner.partition(4);

        assertThat(partitions).containsOnlyKeys("partition_0");

        ExecutionContext context = partitions.get("partition_0");

        assertThat(context.getLong(ExecuteBatchJobConstant.MIN_ACCOUNT_KEY)).isZero();
        assertThat(context.getLong(ExecuteBatchJobConstant.MAX_ACCOUNT_KEY)).isZero();
    }
}
