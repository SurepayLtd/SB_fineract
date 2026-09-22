package org.apache.fineract.portfolio.loanaccount.jobs.applychargetooverdueloaninstallment;


import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.fineract.infrastructure.businessdate.domain.BusinessDateType;
import org.apache.fineract.infrastructure.configuration.domain.ConfigurationDomainService;
import org.apache.fineract.infrastructure.core.domain.ActionContext;
import org.apache.fineract.infrastructure.core.domain.FineractPlatformTenant;
import org.apache.fineract.infrastructure.core.service.ThreadLocalContextUtil;
import org.apache.fineract.portfolio.loanaccount.jobs.paymentdueremider.ExecuteBatchJobConstant;
import org.apache.fineract.portfolio.loanaccount.loanschedule.data.OverdueLoanScheduleData;
import org.apache.fineract.portfolio.loanaccount.service.LoanReadPlatformService;
import org.junit.jupiter.api.AfterEach;
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

    private static final LocalDate BUSINESS_DATE = LocalDate.of(2026, 9, 22);
    private static final int PAGE_SIZE = 2;

    @BeforeEach
    void setUp() {

        ThreadLocalContextUtil.setTenant(new FineractPlatformTenant(1L, "default", "Default", "Asia/Kolkata", null));

        ThreadLocalContextUtil.setActionContext(ActionContext.DEFAULT);

        ThreadLocalContextUtil.setBusinessDates(new HashMap<>(
                Map.of(BusinessDateType.BUSINESS_DATE, BUSINESS_DATE, BusinessDateType.COB_DATE, BUSINESS_DATE.minusDays(1))));


        readService = mock(LoanReadPlatformService.class);
        configurationDomainService = mock(ConfigurationDomainService.class);

        reader = new OverdueLoanItemReader(readService, configurationDomainService, PAGE_SIZE);

        StepExecution stepExecution = new StepExecution(ExecuteBatchJobConstant.APPLY_CHARGE_TO_OVERDUE_LOAN_INSTALLMENT_WORKER_STEP, new JobExecution(1L));

        stepExecution.getExecutionContext().putLong(ExecuteBatchJobConstant.MIN_ACCOUNT_KEY, 1L);
        stepExecution.getExecutionContext().putLong(ExecuteBatchJobConstant.MAX_ACCOUNT_KEY, 100L);
        stepExecution.getExecutionContext().putString(ExecuteBatchJobConstant.PARTITION_KEY, "partition_1");

        reader.beforeStep(stepExecution);
    }

    @AfterEach
    void tearDown() {
        ThreadLocalContextUtil.reset();
    }

    @Test
    void shouldReadItemsFromFirstPage() throws Exception {

        when(configurationDomainService.retrievePenaltyWaitPeriod()).thenReturn(1L);
        when(configurationDomainService.isBackdatePenaltiesEnabled()).thenReturn(false);

        OverdueLoanScheduleData item1 = overdueData(10L, 1, 21L);

        OverdueLoanScheduleData item2 = overdueData(51L, 1, 22L);

        when(readService.retrieveOverdueLoanPage(eq(1L), eq(100L), isNull(), isNull(), eq(PAGE_SIZE), eq(1L), eq(false), isNull()))
                .thenReturn(List.of(item1, item2));

        assertEquals(item1, reader.read());
        assertEquals(item2, reader.read());
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

        when(readService.retrieveOverdueLoanPage(eq(1L), eq(100L), isNull(), isNull(), eq(PAGE_SIZE),
                eq(1L), eq(false), isNull()))
                .thenReturn(List.of(first, second));

        when(readService.retrieveOverdueLoanPage(eq(1L), eq(100L), eq(100L), eq(1),
                eq(PAGE_SIZE), eq(1L), eq(false), eq(22L)))
                .thenReturn(List.of(third));

        assertThat(reader.read()).isSameAs(first);
        assertThat(reader.read()).isSameAs(second);
        assertThat(reader.read()).isSameAs(third);
        assertThat(reader.read()).isNull();

        verify(readService).retrieveOverdueLoanPage(eq(1L), eq(100L), isNull(), isNull(),
                eq(PAGE_SIZE), eq(1L), eq(false), isNull());

        verify(readService).retrieveOverdueLoanPage(eq(1L), eq(100L), eq(100L), eq(1), eq(PAGE_SIZE), eq(1L), eq(false), eq(22L));
    }

    @Test
    void aShortPageEndsThePartitionWithoutAFurtherQuery() throws Exception {

        when(configurationDomainService.retrievePenaltyWaitPeriod()).thenReturn(1L);
        when(configurationDomainService.isBackdatePenaltiesEnabled()).thenReturn(false);

        final OverdueLoanScheduleData only = overdueData(100L, 1, 21L);

        when(readService.retrieveOverdueLoanPage(eq(1L), eq(100L), isNull(), isNull(), eq(PAGE_SIZE),
                eq(1L), eq(false), isNull()))
                .thenReturn(List.of(only));

        assertEquals(only, reader.read());
        assertNull(reader.read());
        assertNull(reader.read());

        verify(readService, times(1)).retrieveOverdueLoanPage(eq(1L), eq(100L), isNull(), isNull(),
                eq(PAGE_SIZE), eq(1L), eq(false), isNull());
    }

    private OverdueLoanScheduleData overdueData(Long loanId, Integer period, Long chargeId) {

        return new OverdueLoanScheduleData(loanId, chargeId, "2026-09-20", BigDecimal.valueOf(0.3),
                "yyyy-MM-dd", "en", BigDecimal.TEN, BigDecimal.ONE, period);
    }
}
