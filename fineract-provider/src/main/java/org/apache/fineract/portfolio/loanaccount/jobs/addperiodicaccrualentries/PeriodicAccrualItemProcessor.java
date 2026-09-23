package org.apache.fineract.portfolio.loanaccount.jobs.addperiodicaccrualentries;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.portfolio.loanaccount.data.LoanAccrualData;
import org.springframework.batch.item.ItemProcessor;

@Slf4j
@RequiredArgsConstructor
public class PeriodicAccrualItemProcessor implements ItemProcessor<LoanAccrualData, LoanAccrualData> {

    @Override
    public LoanAccrualData process(final LoanAccrualData item) {
        return item;
    }
}
