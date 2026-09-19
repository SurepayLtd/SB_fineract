package org.apache.fineract.portfolio.savings.jobs.updatesavingsdormantaccounts;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.infrastructure.campaigns.sms.domain.SmsTransactionRepository;
import org.apache.fineract.notification.data.SmsTypeEnum;
import org.apache.fineract.portfolio.savings.data.SavingsDormancyReminderData;
import org.springframework.batch.item.ItemProcessor;

@Slf4j
@RequiredArgsConstructor
public class SavingsDormancyProcessor implements ItemProcessor<SavingsDormancyReminderData, SavingsDormancyReminderData> {

    private final SmsTransactionRepository smsTransactionRepository;

    @Override
    public SavingsDormancyReminderData process(SavingsDormancyReminderData item) {

        SmsTypeEnum smsType = resolveSmsType(item);

        if (smsType == null) {
            return null;
        }


        Long alreadySent = smsTransactionRepository.countSms(item.mobileNo(), smsType.getDescription());

        if (alreadySent > 0) {

            log.info("Skipping duplicate dormancy reminder {}", item.savingsId());

            return null;
        }

        return item;
    }

    public static SmsTypeEnum resolveSmsType(SavingsDormancyReminderData item) {

        if (item == null || item.reminderDays() == null) {
            return null;
        }

        return switch (item.reminderDays()) {
            case 30 -> SmsTypeEnum.SAVINGS_INACTIVE_30_DAYS;
            case 60 -> SmsTypeEnum.SAVINGS_INACTIVE_60_DAYS;
            case 90 -> SmsTypeEnum.SAVINGS_INACTIVE_90_DAYS;
            default -> null;
        };
    }
}