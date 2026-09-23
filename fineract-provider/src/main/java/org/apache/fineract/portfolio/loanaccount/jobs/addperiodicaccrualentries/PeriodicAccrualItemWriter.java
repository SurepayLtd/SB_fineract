package org.apache.fineract.portfolio.loanaccount.jobs.addperiodicaccrualentries;

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

        log.info("Periodic accrual writer received {} loans for processing. tillDate={}", items.size(), tillDate);

        for (final LoanAccrualData item : items) {

            log.info("Processing periodic accrual for loanId={}", item.loanId());
            try {

                loanAccrualsProcessingService.addPeriodicAccrual(item.loanId(), tillDate);

                log.info("Successfully processed periodic accrual for loanId={}", item.loanId());
            } catch (Exception e) {

                log.error("Failed to process periodic accrual for loanId={}", item.loanId(), e);
                throw e;
            }
        }

    }
}
