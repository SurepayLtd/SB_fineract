package org.apache.fineract.portfolio.loanaccount.jobs.applychargetooverdueloaninstallment;


import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.List;

import org.apache.fineract.infrastructure.jobs.exception.JobExecutionException;
import org.apache.fineract.portfolio.loanaccount.loanschedule.data.OverdueLoanScheduleData;
import org.apache.fineract.portfolio.loanaccount.service.LoanChargeWritePlatformService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.item.Chunk;

public class OverdueLoanWriterTest {

    private LoanChargeWritePlatformService loanChargeWritePlatformService;

    private OverdueLoanWriter writer;

    @BeforeEach
    void setUp() {
        loanChargeWritePlatformService = mock(LoanChargeWritePlatformService.class);

        writer = new OverdueLoanWriter(loanChargeWritePlatformService);
    }

    @Test
    void shouldGroupInstallmentsByLoanAndApplyChargesOncePerLoan() throws Exception {

        OverdueLoanScheduleData loan100Installment1 = overdueData(100L, 1, 21L);
        OverdueLoanScheduleData loan100Installment2 = overdueData(100L, 2, 21L);
        OverdueLoanScheduleData loan101Installment1 = overdueData(101L, 1, 22L);

        Chunk<OverdueLoanScheduleData> chunk = new Chunk<>(List.of(loan100Installment1, loan100Installment2, loan101Installment1));

        writer.write(chunk);

        verify(loanChargeWritePlatformService, times(1)).applyOverdueChargesForLoan(eq(100L), argThat(collection -> collection.size() == 2));

        verify(loanChargeWritePlatformService, times(1)).applyOverdueChargesForLoan(eq(101L), argThat(collection -> collection.size() == 1));
    }

    @Test
    void shouldThrowJobExecutionExceptionWhenAnyLoanFails() throws Exception {

        OverdueLoanScheduleData loan100 = overdueData(100L, 1, 21L);

        doThrow(new RuntimeException("Database failure"))
                .when(loanChargeWritePlatformService)
                .applyOverdueChargesForLoan(eq(100L), anyCollection());

        Chunk<OverdueLoanScheduleData> chunk = new Chunk<>(List.of(loan100));

        assertThrows(JobExecutionException.class, () -> writer.write(chunk));

        verify(loanChargeWritePlatformService).applyOverdueChargesForLoan(eq(100L), anyCollection());
    }

    private OverdueLoanScheduleData overdueData(Long loanId, Integer period, Long chargeId) {

        return new OverdueLoanScheduleData(loanId, chargeId, "2026-09-20", BigDecimal.valueOf(0.3), "yyyy-MM-dd",
                "en", BigDecimal.TEN, BigDecimal.ONE, period);
    }
}