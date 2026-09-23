package org.apache.fineract.portfolio.loanaccount.jobs.applychargetooverdueloaninstallment;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.isNull;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.anyBoolean;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import java.math.BigDecimal;
import java.util.List;
import org.apache.fineract.infrastructure.configuration.domain.ConfigurationDomainService;
import org.apache.fineract.portfolio.loanaccount.jobs.paymentdueremider.ExecuteBatchJobConstant;
import org.apache.fineract.portfolio.loanaccount.loanschedule.data.OverdueLoanScheduleData;
import org.apache.fineract.portfolio.loanaccount.service.LoanReadPlatformService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepExecution;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
public class OverdueLoanItemReaderTest {

    private LoanReadPlatformService readService;
    private ConfigurationDomainService configurationDomainService;

    private OverdueLoanItemReader reader;

    private static final int PAGE_SIZE = 2;

    @BeforeEach
    void setUp() {
        readService = mock(LoanReadPlatformService.class);
        configurationDomainService = mock(ConfigurationDomainService.class);

        reader = new OverdueLoanItemReader(readService, configurationDomainService, PAGE_SIZE);

        StepExecution stepExecution = new StepExecution(ExecuteBatchJobConstant.APPLY_CHARGE_TO_OVERDUE_LOAN_INSTALLMENT_WORKER_STEP, new JobExecution(1L));

        stepExecution.getExecutionContext().putLong(ExecuteBatchJobConstant.MIN_ACCOUNT_KEY, 1L);
        stepExecution.getExecutionContext().putLong(ExecuteBatchJobConstant.MAX_ACCOUNT_KEY, 100L);
        stepExecution.getExecutionContext().putString(ExecuteBatchJobConstant.PARTITION_KEY, "partition_1");

        reader.beforeStep(stepExecution);
    }

    @Test
    void shouldReadItemsFromFirstPage() throws Exception {

        when(configurationDomainService.retrievePenaltyWaitPeriod()).thenReturn(1L);
        when(configurationDomainService.isBackdatePenaltiesEnabled()).thenReturn(false);

        OverdueLoanScheduleData item1 = overdueData(10L, 1, 21L);

        OverdueLoanScheduleData item2 = overdueData(51L, 1, 22L);


        when(readService.retrieveOverdueLoanPage(eq(1L), eq(100L), eq(0L), eq(0), eq(PAGE_SIZE), eq(1L), eq(false), eq(0L)))
                .thenReturn(List.of(item1, item2));


        assertEquals(item1, reader.read());
        assertEquals(item2, reader.read());

        verify(readService).retrieveOverdueLoanPage(eq(1L), eq(100L), eq(0L), eq(0), eq(PAGE_SIZE), eq(1L), eq(false), eq(0L));
    }

    @Test
    void shouldReturnNullWhenNoRecordsExist() throws Exception {


        when(configurationDomainService.retrievePenaltyWaitPeriod()).thenReturn(1L);
        when(configurationDomainService.isBackdatePenaltiesEnabled()).thenReturn(false);

        when(readService.retrieveOverdueLoanPage(any(), any(), any(), any(), anyInt(), anyLong(), anyBoolean(), any()))
                .thenReturn(List.of());

        assertNull(reader.read());
    }

    @Test
    void eachPageResumesFromTheLastInstructionOfThePreviousOne() throws Exception {

        when(configurationDomainService.retrievePenaltyWaitPeriod()).thenReturn(1L);
        when(configurationDomainService.isBackdatePenaltiesEnabled()).thenReturn(false);

        final OverdueLoanScheduleData first = overdueData(99L, 1, 21L);
        final OverdueLoanScheduleData second = overdueData(100L, 1, 22L);
        final OverdueLoanScheduleData third = overdueData(101L, 2, 23L);

        when(readService.retrieveOverdueLoanPage(eq(1L), eq(100L), eq(0L), eq(0), eq(PAGE_SIZE), eq(1L), eq(false), eq(0L)))
                .thenReturn(List.of(first, second));

        when(readService.retrieveOverdueLoanPage(eq(1L), eq(100L), eq(100L), eq(1),
                eq(PAGE_SIZE), eq(1L), eq(false), eq(22L)))
                .thenReturn(List.of(third));

        assertThat(reader.read()).isSameAs(first);
        assertThat(reader.read()).isSameAs(second);
        assertThat(reader.read()).isSameAs(third);
        assertThat(reader.read()).isNull();

        verify(readService).retrieveOverdueLoanPage(eq(1L), eq(100L), eq(0L), eq(0), eq(PAGE_SIZE), eq(1L), eq(false), eq(0L));

        verify(readService).retrieveOverdueLoanPage(eq(1L), eq(100L), eq(100L), eq(1), eq(PAGE_SIZE), eq(1L), eq(false), eq(22L));
    }

    @Test
    void aShortPageEndsThePartitionWithoutAFurtherQuery() throws Exception {

        when(configurationDomainService.retrievePenaltyWaitPeriod()).thenReturn(1L);
        when(configurationDomainService.isBackdatePenaltiesEnabled()).thenReturn(false);

        final OverdueLoanScheduleData only = overdueData(100L, 1, 21L);

        when(readService.retrieveOverdueLoanPage(eq(1L), eq(100L), eq(0L), eq(0), eq(PAGE_SIZE), eq(1L), eq(false), eq(0L)))
                .thenReturn(List.of(only));

        assertEquals(only, reader.read());
        assertNull(reader.read());
        assertNull(reader.read());

        verify(readService, times(1)).retrieveOverdueLoanPage(eq(1L), eq(100L), eq(0L), eq(0),
                eq(PAGE_SIZE), eq(1L), eq(false), eq(0L));
    }

    private OverdueLoanScheduleData overdueData(Long loanId, Integer period, Long chargeId) {

        return new OverdueLoanScheduleData(loanId, chargeId, "2026-09-20", BigDecimal.valueOf(0.3),
                "yyyy-MM-dd", "en", BigDecimal.TEN, BigDecimal.ONE, period);
    }
}
