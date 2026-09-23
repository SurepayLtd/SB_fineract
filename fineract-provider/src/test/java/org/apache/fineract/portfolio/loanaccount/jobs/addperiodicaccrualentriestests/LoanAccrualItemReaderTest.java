package org.apache.fineract.portfolio.loanaccount.jobs.addperiodicaccrualentriestests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.isNull;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.anyBoolean;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
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
import org.apache.fineract.portfolio.loanaccount.data.LoanAccrualData;
import org.apache.fineract.portfolio.loanaccount.jobs.addperiodicaccrualentries.PeriodicAccrualItemReader;
import org.apache.fineract.portfolio.loanaccount.jobs.paymentdueremider.ExecuteBatchJobConstant;
import org.apache.fineract.portfolio.loanaccount.service.LoanAccrualBatchReadService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepExecution;

public class LoanAccrualItemReaderTest {

    private LoanAccrualBatchReadService readService;
    private PeriodicAccrualItemReader reader;
    private ConfigurationDomainService configurationDomainService;

    private static final LocalDate BUSINESS_DATE = LocalDate.of(2026, 9, 23);
    private static final int PAGE_SIZE = 2;
    private static final Integer accountingType = AccountingRuleType.ACCRUAL_PERIODIC.getValue();

    @BeforeEach
    void setUp() {

        ThreadLocalContextUtil.setTenant(new FineractPlatformTenant(1L, "default", "Default", "Asia/Kolkata", null));

        ThreadLocalContextUtil.setActionContext(ActionContext.DEFAULT);

        ThreadLocalContextUtil.setBusinessDates(new HashMap<>(
                Map.of(BusinessDateType.BUSINESS_DATE, BUSINESS_DATE, BusinessDateType.COB_DATE, BUSINESS_DATE.minusDays(1))));

        readService = mock(LoanAccrualBatchReadService.class);
        configurationDomainService = mock(ConfigurationDomainService.class);

        reader = new PeriodicAccrualItemReader(PAGE_SIZE, configurationDomainService, readService);

        StepExecution stepExecution = new StepExecution(ExecuteBatchJobConstant.PERIODIC_ACCRUAL_PARTITIONER_STEP, new JobExecution(1L));

        stepExecution.getExecutionContext().putLong(ExecuteBatchJobConstant.MIN_ACCOUNT_KEY, 1L);
        stepExecution.getExecutionContext().putLong(ExecuteBatchJobConstant.MAX_ACCOUNT_KEY, 100L);
        stepExecution.getExecutionContext().putString(ExecuteBatchJobConstant.PARTITION_KEY, "partition_0");

        reader.beforeStep(stepExecution);
    }

    @AfterEach
    void tearDown() {
        ThreadLocalContextUtil.reset();
    }

    @Test
    void shouldReadItemsFromFirstPage() throws Exception {

        LoanAccrualData first = new LoanAccrualData(10L);
        LoanAccrualData second = new LoanAccrualData(50L);

        when(readService.retrieveLoanAccrualPage(eq(1L), eq(100L), isNull(), eq(PAGE_SIZE), eq(accountingType), eq(BUSINESS_DATE), anyBoolean()))
                .thenReturn(List.of(first, second));

        assertEquals(first, reader.read());
        assertEquals(second, reader.read());
    }

    @Test
    void shouldReturnNullWhenNoLoansExist() throws Exception {

        when(readService.retrieveLoanAccrualPage(any(), any(), any(), anyInt(), anyInt(), any(), anyBoolean()))
                .thenReturn(List.of());

        assertNull(reader.read());
    }

    @Test
    void eachPageResumesFromTheLastLoanOfThePreviousPage() throws Exception {

        LoanAccrualData first = new LoanAccrualData(10L);
        LoanAccrualData second = new LoanAccrualData(50L);
        LoanAccrualData third = new LoanAccrualData(75L);

        when(readService.retrieveLoanAccrualPage(eq(1L), eq(100L), isNull(), eq(PAGE_SIZE), eq(accountingType), eq(BUSINESS_DATE), anyBoolean()))
                .thenReturn(List.of(first, second));

        when(readService.retrieveLoanAccrualPage(eq(1L), eq(100L), eq(50L), eq(PAGE_SIZE), eq(accountingType), eq(BUSINESS_DATE), anyBoolean()))
                .thenReturn(List.of(third));

        assertThat(reader.read()).isSameAs(first);
        assertThat(reader.read()).isSameAs(second);
        assertThat(reader.read()).isSameAs(third);
        assertThat(reader.read()).isNull();

        verify(readService).retrieveLoanAccrualPage(eq(1L), eq(100L), isNull(), eq(PAGE_SIZE), eq(accountingType), eq(BUSINESS_DATE), anyBoolean());
        verify(readService).retrieveLoanAccrualPage(eq(1L), eq(100L), eq(50L), eq(PAGE_SIZE), eq(accountingType), eq(BUSINESS_DATE), anyBoolean());
    }

    @Test
    void aShortPageEndsThePartitionWithoutAFurtherQuery() throws Exception {

        LoanAccrualData only = new LoanAccrualData(20L);

        when(readService.retrieveLoanAccrualPage(eq(1L), eq(100L), isNull(), eq(PAGE_SIZE), eq(accountingType), eq(BUSINESS_DATE), anyBoolean()))
                .thenReturn(List.of(only));

        assertEquals(only, reader.read());
        assertNull(reader.read());
        assertNull(reader.read());

        verify(readService, times(1)).retrieveLoanAccrualPage(eq(1L), eq(100L), isNull(), eq(PAGE_SIZE), eq(accountingType), eq(BUSINESS_DATE), anyBoolean());
    }
}
