package org.apache.fineract.portfolio.loanaccount.jobs.paymentoverdueremider;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.infrastructure.campaigns.sms.domain.SmsTransactionRepository;
import org.apache.fineract.notification.data.SmsTypeEnum;
import org.apache.fineract.notification.service.SmsNotificationWritePlatformService;
import org.apache.fineract.portfolio.loanaccount.data.LoanInstallmentReminderData;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;

@RequiredArgsConstructor
@Slf4j
public class LoanInstallmentReminderWriter implements ItemWriter<LoanInstallmentReminderData> {

    private final SmsNotificationWritePlatformService smsNotificationWritePlatformService;


    @Override
    public void write(Chunk<? extends LoanInstallmentReminderData> chunk) throws Exception {
        for (LoanInstallmentReminderData item : chunk) {

            final SmsTypeEnum smsType = LoanInstallmentReminderProcessor.smsType(item.reminderDays());

            smsNotificationWritePlatformService.processLoanInstallmentNotification(item, smsType);
        }
    }
}
