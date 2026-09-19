package org.apache.fineract.portfolio.loanaccount.jobs.paymentoverdueremiders;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.infrastructure.campaigns.sms.domain.SmsTransactionRepository;
import org.apache.fineract.notification.data.SmsTypeEnum;
import org.apache.fineract.portfolio.loanaccount.data.LoanInstallmentOverdueReminderData;
import org.apache.fineract.portfolio.loanaccount.jobs.paymentdueremider.ExecuteBatchJobConstant;
import org.springframework.batch.item.ItemProcessor;

@Slf4j
@RequiredArgsConstructor
public class LoanOverDueItemProcessor implements ItemProcessor<LoanInstallmentOverdueReminderData, LoanInstallmentOverdueReminderData> {

    private final SmsTransactionRepository smsTransactionRepository;


    @Override
    public LoanInstallmentOverdueReminderData process(LoanInstallmentOverdueReminderData item) throws Exception {
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

        final SmsTypeEnum smsType = smsType(item.overdueDays());

        if (!smsType.getStatus().equalsIgnoreCase("Mandatory")) {

            log.debug("Loan overdue reminder SMS is not mandatory loanId={}, installmentId={}, smsType={}",
                    item.loanId(), item.installmentId(), smsType);

            return null;
        }

        final String messageId = messageId(item);

        final Long count = smsTransactionRepository.countBySmsId(messageId);

        if (count != null && count > 0) {

            log.info("Loan overdue reminder already sent loanId={}, installmentId={}, smsType={}, messageId={}",
                    item.loanId(), item.installmentId(), smsType, messageId);

            return null;
        }

        return item;
    }

    public static SmsTypeEnum smsType(final Integer overdueDays) {

        return switch (overdueDays) {
            case ExecuteBatchJobConstant.D1 -> SmsTypeEnum.LOAN_INSTALLMENT_OVERDUE_D1;
            case ExecuteBatchJobConstant.D7-> SmsTypeEnum.LOAN_INSTALLMENT_OVERDUE_D7;
            case ExecuteBatchJobConstant.D30 -> SmsTypeEnum.LOAN_INSTALLMENT_OVERDUE_D30;
            case ExecuteBatchJobConstant.D60 -> SmsTypeEnum.LOAN_INSTALLMENT_OVERDUE_D60;
            case ExecuteBatchJobConstant.D90 -> SmsTypeEnum.LOAN_INSTALLMENT_OVERDUE_D90;


            default -> throw new IllegalArgumentException("Unsupported overdue reminder offset: " + overdueDays
            );
        };
    }

    public static String messageId(final LoanInstallmentOverdueReminderData item) {

        return switch (item.overdueDays()) {

            case ExecuteBatchJobConstant.D1 -> "LOAN-INSTALLMENT-OVERDUE-D1-" + item.installmentId();

            case ExecuteBatchJobConstant.D7  -> "LOAN-INSTALLMENT-OVERDUE-D7-" + item.installmentId();

            case ExecuteBatchJobConstant.D30 -> "LOAN-INSTALLMENT-OVERDUE-D30-" + item.installmentId();

            case ExecuteBatchJobConstant.D60 -> "LOAN-INSTALLMENT-OVERDUE-D60-" + item.installmentId();

            case ExecuteBatchJobConstant.D90 -> "LOAN-INSTALLMENT-OVERDUE-D90-" + item.installmentId();


            default -> throw new IllegalArgumentException("Unsupported overdue reminder offset: " + item.overdueDays()
            );
        };
    }
}
