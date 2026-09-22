package org.apache.fineract.portfolio.loanaccount.jobs.applychargetooverdueloaninstallment;

import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.portfolio.loanaccount.loanschedule.data.OverdueLoanScheduleData;
import org.springframework.batch.item.ItemProcessor;

@Slf4j
public class OverdueLoanProcessor implements ItemProcessor<OverdueLoanScheduleData, OverdueLoanScheduleData> {

    @Override
    public OverdueLoanScheduleData process(OverdueLoanScheduleData item) {

        return item;
    }
}
