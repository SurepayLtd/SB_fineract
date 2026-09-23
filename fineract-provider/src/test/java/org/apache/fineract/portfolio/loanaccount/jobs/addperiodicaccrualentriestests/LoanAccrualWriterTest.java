package org.apache.fineract.portfolio.loanaccount.jobs.addperiodicaccrualentriestests;


import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.fineract.infrastructure.businessdate.domain.BusinessDateType;
import org.apache.fineract.infrastructure.core.domain.ActionContext;
import org.apache.fineract.infrastructure.core.domain.FineractPlatformTenant;
import org.apache.fineract.infrastructure.core.service.ThreadLocalContextUtil;
import org.apache.fineract.infrastructure.jobs.exception.JobExecutionException;
import org.apache.fineract.portfolio.loanaccount.data.LoanAccrualData;
import org.apache.fineract.portfolio.loanaccount.jobs.addperiodicaccrualentries.PeriodicAccrualItemWriter;
import org.apache.fineract.portfolio.loanaccount.service.LoanAccrualsProcessingService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.item.Chunk;

public class LoanAccrualWriterTest {

    private LoanAccrualsProcessingService accrualService;
    private PeriodicAccrualItemWriter writer;
    private static final LocalDate BUSINESS_DATE = LocalDate.of(2026, 9, 23);


    @BeforeEach
    void setUp() {

        ThreadLocalContextUtil.setTenant(new FineractPlatformTenant(1L, "default", "Default", "Asia/Kolkata", null));

        ThreadLocalContextUtil.setActionContext(ActionContext.DEFAULT);

        ThreadLocalContextUtil.setBusinessDates(new HashMap<>(
                Map.of(BusinessDateType.BUSINESS_DATE, BUSINESS_DATE, BusinessDateType.COB_DATE, BUSINESS_DATE.minusDays(1))));

        accrualService = mock(LoanAccrualsProcessingService.class);

        writer = new PeriodicAccrualItemWriter(accrualService);
    }

    @AfterEach
    void tearDown() {
        ThreadLocalContextUtil.reset();
    }

    @Test
    void shouldApplyAccrualForEveryLoan() throws Exception {

        LoanAccrualData loan1 = new LoanAccrualData(101L);
        LoanAccrualData loan2 = new LoanAccrualData(102L);

        writer.write(new Chunk<>(List.of(loan1, loan2)));

        verify(accrualService).addPeriodicAccrual(101L, BUSINESS_DATE);
        verify(accrualService).addPeriodicAccrual(102L, BUSINESS_DATE);
    }

    @Test
    void shouldThrowJobExecutionExceptionWhenAnyLoanFails() throws Exception {

        LoanAccrualData loan1 = new LoanAccrualData(101L);
        LoanAccrualData loan2 = new LoanAccrualData(102L);

        doThrow(new JobExecutionException(List.of(new RuntimeException("failure"))))
                .when(accrualService)
                .addPeriodicAccrual(102L, BUSINESS_DATE);

        Chunk<LoanAccrualData> chunk = new Chunk<>(List.of(loan1, loan2));

        assertThrows(JobExecutionException.class, () -> writer.write(chunk));

        verify(accrualService).addPeriodicAccrual(101L, BUSINESS_DATE);
        verify(accrualService).addPeriodicAccrual(102L, BUSINESS_DATE);
    }
}