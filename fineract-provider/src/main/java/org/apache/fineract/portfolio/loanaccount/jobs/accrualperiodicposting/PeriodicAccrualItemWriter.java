package org.apache.fineract.portfolio.loanaccount.jobs.accrualperiodicposting;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.infrastructure.core.exception.MultiException;
import org.apache.fineract.infrastructure.core.service.DateUtils;
import org.apache.fineract.portfolio.loanaccount.data.LoanAccrualData;
import org.apache.fineract.portfolio.loanaccount.service.LoanAccrualsProcessingService;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;

import java.time.LocalDate;



@RequiredArgsConstructor
@Slf4j
public class PeriodicAccrualItemWriter implements ItemWriter<LoanAccrualData> {

    private final LoanAccrualsProcessingService loanAccrualsProcessingService;

    @Override
    public void write(final Chunk<? extends LoanAccrualData> items) throws MultiException {

        final LocalDate tillDate = DateUtils.getBusinessLocalDate();

        for (final LoanAccrualData item : items) {

            loanAccrualsProcessingService.addPeriodicAccrual(item.loanId(), tillDate);
        }
    }
}
