package org.apache.fineract.portfolio.loanaccount.jobs.applychargetooverdueloaninstallment;

import org.apache.fineract.infrastructure.configuration.domain.ConfigurationDomainService;
import org.apache.fineract.portfolio.loanaccount.data.JobPartition;
import org.apache.fineract.portfolio.loanaccount.jobs.paymentdueremider.ExecuteBatchJobConstant;
import org.apache.fineract.portfolio.loanaccount.service.LoanReadPlatformService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.item.ExecutionContext;
import static org.assertj.core.api.Assertions.assertThat;


import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyBoolean;

public class OverdueLoanPartitionerTest {

    private LoanReadPlatformService loanReadPlatformService;
    private ConfigurationDomainService configurationDomainService;

    private OverdueLoanPartitioner partitioner;

    @BeforeEach
    void setUp() {
        loanReadPlatformService = mock(LoanReadPlatformService.class);
        configurationDomainService = mock(ConfigurationDomainService.class);

        partitioner = new OverdueLoanPartitioner(loanReadPlatformService, configurationDomainService);
    }

    @Test
    void eachPartitionCarriesItsOwnAccountRange() {

        when(configurationDomainService.retrievePenaltyWaitPeriod()).thenReturn(1L);
        when(configurationDomainService.isBackdatePenaltiesEnabled()).thenReturn(false);

        when(loanReadPlatformService.retrieveOverdueLoanPartitions(100, 1L, false))
                .thenReturn(List.of(
                        new JobPartition(1L, 100L, 1L, 100L),
                        new JobPartition(101L, 150L, 2L, 50L)));

        Map<String, ExecutionContext> partitions = partitioner.partition(4);

        assertEquals(2, partitions.size());
        assertThat(partitions).containsOnlyKeys("partition_1", "partition_2");
        assertThat(partitions.get("partition_1").getLong(ExecuteBatchJobConstant.MIN_ACCOUNT_KEY)).isEqualTo(1L);
        assertThat(partitions.get("partition_1").getLong(ExecuteBatchJobConstant.MAX_ACCOUNT_KEY)).isEqualTo(100L);
        assertThat(partitions.get("partition_2").getLong(ExecuteBatchJobConstant.MIN_ACCOUNT_KEY)).isEqualTo(101L);
        assertThat(partitions.get("partition_2").getLong(ExecuteBatchJobConstant.MAX_ACCOUNT_KEY)).isEqualTo(150L);

        verify(loanReadPlatformService).retrieveOverdueLoanPartitions(100, 1L, false);
    }

    @Test
    void shouldCreateDummyPartitionWhenNoLoansFound() {


        when(configurationDomainService.retrievePenaltyWaitPeriod()).thenReturn(1L);
        when(configurationDomainService.isBackdatePenaltiesEnabled()).thenReturn(false);

        when(loanReadPlatformService.retrieveOverdueLoanPartitions(anyInt(), anyLong(), anyBoolean()))
                .thenReturn(List.of());

        final Map<String, ExecutionContext> partitions = partitioner.partition(1);

        assertEquals(1, partitions.size());

        assertThat(partitions).containsOnlyKeys("partition_0");
        assertThat(partitions.get("partition_0").getLong(ExecuteBatchJobConstant.MIN_ACCOUNT_KEY)).isZero();
        assertThat(partitions.get("partition_0").getLong(ExecuteBatchJobConstant.MAX_ACCOUNT_KEY)).isZero();
    }
}
