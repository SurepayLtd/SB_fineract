package org.apache.fineract.portfolio.loanaccount.jobs.paymentoverdueremider;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.infrastructure.campaigns.sms.domain.SmsTransactionRepository;
import org.apache.fineract.notification.data.SmsTypeEnum;
import org.apache.fineract.portfolio.loanaccount.data.LoanInstallmentReminderData;
import org.springframework.batch.item.ItemProcessor;

@Slf4j
@RequiredArgsConstructor
public class LoanInstallmentReminderProcessor implements ItemProcessor<LoanInstallmentReminderData, LoanInstallmentReminderData> {

    private final SmsTransactionRepository smsTransactionRepository;


    @Override
    public LoanInstallmentReminderData process(LoanInstallmentReminderData item) throws Exception {
        if (item.mobileNo() == null || item.mobileNo().isBlank()) {

            log.debug("Skipping loan installment reminder because client has no mobile number. loanId={}, installmentId={}",
                    item.loanId(), item.installmentId()
            );

            return null;
        }

        if (item.totalDue() == null || item.totalDue().signum() <= 0) {

            log.debug("Skipping loan installment reminder because amount due is zero. loanId={}, installmentId={}",
                    item.loanId(), item.installmentId()
            );

            return null;
        }

        final SmsTypeEnum smsType = smsType(item.reminderDays());

        if (!smsType.getStatus().equalsIgnoreCase("Mandatory")) {

            log.debug("Loan installment reminder SMS is not mandatory loanId={}, installmentId={}, smsType={}",
                    item.loanId(), item.installmentId(), smsType);

            return null;
        }

        final String messageId = messageId(item);

        final Long count = smsTransactionRepository.countBySmsId(messageId);

        if (count != null && count > 0) {

            log.info("Loan installment reminder already sent loanId={}, installmentId={}, smsType={}, messageId={}",
                    item.loanId(), item.installmentId(), smsType, messageId);

            return null;
        }

        return item;
    }

    public static SmsTypeEnum smsType(final Integer reminderDays) {

        return switch (reminderDays) {
            case ExecuteInstallmentReminderConstant.T_7 -> SmsTypeEnum.LOAN_INSTALLMENT_DUE_T7;
            case ExecuteInstallmentReminderConstant.T_3 -> SmsTypeEnum.LOAN_INSTALLMENT_DUE_T3;
            case ExecuteInstallmentReminderConstant.T_1 -> SmsTypeEnum.LOAN_INSTALLMENT_DUE_T1;
            case ExecuteInstallmentReminderConstant.T_0 -> SmsTypeEnum.LOAN_INSTALLMENT_DUE_T0;

            default -> throw new IllegalArgumentException("Unsupported installment reminder offset: " + reminderDays
            );
        };
    }

    public static String messageId(final LoanInstallmentReminderData item) {

        return switch (item.reminderDays()) {

            case ExecuteInstallmentReminderConstant.T_7 -> "LOAN-INSTALLMENT-DUE-T7-" + item.installmentId();

            case ExecuteInstallmentReminderConstant.T_3  -> "LOAN-INSTALLMENT-DUE-T3-" + item.installmentId();

            case ExecuteInstallmentReminderConstant.T_1 -> "LOAN-INSTALLMENT-DUE-T1-" + item.installmentId();

            case ExecuteInstallmentReminderConstant.T_0 -> "LOAN-INSTALLMENT-DUE-T0-" + item.installmentId();

            default -> throw new IllegalArgumentException("Unsupported installment reminder offset: " + item.reminderDays()
            );
        };
    }
}
