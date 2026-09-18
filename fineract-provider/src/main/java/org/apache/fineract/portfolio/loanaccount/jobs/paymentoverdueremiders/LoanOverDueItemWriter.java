package org.apache.fineract.portfolio.loanaccount.jobs.paymentoverdueremiders;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.notification.data.SmsTypeEnum;
import org.apache.fineract.notification.service.SmsNotificationWritePlatformService;
import org.apache.fineract.portfolio.loanaccount.data.LoanInstallmentOverdueReminderData;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;

@RequiredArgsConstructor
@Slf4j
public class LoanOverDueItemWriter implements ItemWriter<LoanInstallmentOverdueReminderData> {

    private final SmsNotificationWritePlatformService smsNotificationWritePlatformService;

    @Override
    public void write(Chunk<? extends LoanInstallmentOverdueReminderData> chunk) throws Exception {

        for (LoanInstallmentOverdueReminderData item: chunk){
            SmsTypeEnum smsTypeEnum = LoanOverDueItemProcessor.smsType(item.overdueDays());
            smsNotificationWritePlatformService.processLoanOverdueNotification(item, smsTypeEnum);
        }

    }
}
