package org.apache.fineract.portfolio.loanaccount.jobs.applychargetooverdueloaninstallment;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.infrastructure.core.data.ApiParameterError;
import org.apache.fineract.infrastructure.core.exception.AbstractPlatformDomainRuleException;
import org.apache.fineract.infrastructure.core.exception.PlatformApiDataValidationException;
import org.apache.fineract.infrastructure.jobs.exception.JobExecutionException;
import org.apache.fineract.portfolio.loanaccount.loanschedule.data.OverdueLoanScheduleData;
import org.apache.fineract.portfolio.loanaccount.service.LoanChargeWritePlatformService;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class OverdueLoanWriter implements ItemWriter<OverdueLoanScheduleData> {

    private final LoanChargeWritePlatformService loanChargeWritePlatformService;

    @Override
    public void write(Chunk<? extends OverdueLoanScheduleData> chunk) throws Exception {

        log.info("Overdue charge writer received {} records", chunk.size());

        Map<Long, Collection<OverdueLoanScheduleData>> overdueScheduleData = new HashMap<>();

        for (OverdueLoanScheduleData overdueInstallment : chunk.getItems()) {

            log.info("Processing overdue installment for loanId: {}, Amount: {}, ChargeId: {}", overdueInstallment.getLoanId(), overdueInstallment.getAmount(), overdueInstallment.getChargeId());

            if (overdueScheduleData.containsKey(overdueInstallment.getLoanId())) {
                overdueScheduleData.get(overdueInstallment.getLoanId()).add(overdueInstallment);
            } else {
                Collection<OverdueLoanScheduleData> loanData = new ArrayList<>();
                loanData.add(overdueInstallment);
                overdueScheduleData.put(overdueInstallment.getLoanId(), loanData);
            }
        }

        log.info("Total overdue loans to process for applying charges: {}", overdueScheduleData.size());

        final List<Throwable> exceptions = new ArrayList<>();

        for (Map.Entry<Long, Collection<OverdueLoanScheduleData>> entry : overdueScheduleData.entrySet()) {

            try {

                log.info("Applying charges for loanId: {}, installments: {}", entry.getKey(), entry.getValue().stream()
                                .map(data -> String.format(
                                        "[period=%s, chargeId=%s, amount=%s, dueDate=%s]",
                                        data.getPeriodNumber(),
                                        data.getChargeId(),
                                        data.getAmount(),
                                        data.getDueDate()))
                                .toList());

                if (!entry.getValue().isEmpty()) {
                    loanChargeWritePlatformService.applyOverdueChargesForLoan(entry.getKey(), entry.getValue());

                    log.info("SUCCESS applying overdue charges for loanId: {}", entry.getKey());
                }

            } catch (PlatformApiDataValidationException e) {

                final List<ApiParameterError> errors = e.getErrors();
                for (final ApiParameterError error : errors) {
                    log.error("Apply Charges due for overdue loans failed for account {} with message: {}", entry.getKey(),
                            error.getDeveloperMessage(), e);
                }
                exceptions.add(e);

            } catch (AbstractPlatformDomainRuleException e) {

                log.error("Apply Charges due for overdue loans failed for account {} with message: {}", entry.getKey(),
                            e.getDefaultUserMessage(), e);
                exceptions.add(e);

            } catch (Exception e) {

                log.error("Apply Charges due for overdue loans failed for account {}", entry.getKey(), e);

                exceptions.add(e);
            }
        }

        if (!exceptions.isEmpty()) {
            throw new JobExecutionException(exceptions);
        }
    }
}
