package org.apache.fineract.portfolio.loanaccount.jobs.addperiodicaccrualentriestests;

import static org.junit.jupiter.api.Assertions.assertSame;

import org.apache.fineract.portfolio.loanaccount.data.LoanAccrualData;
import org.apache.fineract.portfolio.loanaccount.jobs.addperiodicaccrualentries.PeriodicAccrualItemProcessor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LoanAccrualProcessorTest {

    private PeriodicAccrualItemProcessor processor;

    @BeforeEach
    void setUp() {
        processor = new PeriodicAccrualItemProcessor();
    }

    @Test
    void shouldReturnSameLoan() throws Exception {

        LoanAccrualData loan = new LoanAccrualData(101L);

        assertSame(loan, processor.process(loan));
    }
}
