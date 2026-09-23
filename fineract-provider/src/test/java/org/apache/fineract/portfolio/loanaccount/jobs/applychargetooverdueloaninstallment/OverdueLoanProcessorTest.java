package org.apache.fineract.portfolio.loanaccount.jobs.applychargetooverdueloaninstallment;

import static org.junit.jupiter.api.Assertions.assertSame;

import java.math.BigDecimal;

import org.apache.fineract.portfolio.loanaccount.loanschedule.data.OverdueLoanScheduleData;
import org.junit.jupiter.api.Test;

public class OverdueLoanProcessorTest {

    private final OverdueLoanProcessor processor = new OverdueLoanProcessor();

    @Test
    void shouldReturnSameItem() throws Exception {

        OverdueLoanScheduleData data = new OverdueLoanScheduleData(50L, 21L, "2026-09-20", BigDecimal.ONE, "yyyy-MM-dd", "en",
                        BigDecimal.TEN, BigDecimal.ONE, 1);

        assertSame(data, processor.process(data));
    }
}