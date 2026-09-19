package org.apache.fineract.portfolio.savings.jobs.updatesavingsdormantaccounts;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.notification.data.SmsTypeEnum;
import org.apache.fineract.notification.service.SmsNotificationWritePlatformService;
import org.apache.fineract.portfolio.savings.data.SavingsDormancyReminderData;
import org.apache.fineract.portfolio.savings.domain.SavingsAccount;
import org.apache.fineract.portfolio.savings.domain.SavingsAccountRepositoryWrapper;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;

import static org.apache.fineract.portfolio.savings.jobs.updatesavingsdormantaccounts.SavingsDormancyProcessor.resolveSmsType;

@Slf4j
@RequiredArgsConstructor
public class SavingsDormancyWriter implements ItemWriter<SavingsDormancyReminderData> {

    private final SmsNotificationWritePlatformService smsService;
    private final SavingsAccountRepositoryWrapper savingsAccountRepositoryWrapper;

    @Override
    public void write(Chunk<? extends SavingsDormancyReminderData> chunk) {

        for (SavingsDormancyReminderData item : chunk) {

            SavingsAccount account = this.savingsAccountRepositoryWrapper.findOneWithNotFoundDetection(item.savingsId());

            SmsTypeEnum smsType = resolveSmsType(item);

            smsService.processSavingsAccountSmsNotification(account, smsType, null);
        }
    }

}
