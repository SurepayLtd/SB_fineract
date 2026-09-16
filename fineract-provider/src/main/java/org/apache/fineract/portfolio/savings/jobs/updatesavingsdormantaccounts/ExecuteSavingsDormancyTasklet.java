package org.apache.fineract.portfolio.savings.jobs.updatesavingsdormantaccounts;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.fineract.infrastructure.campaigns.sms.domain.SmsTransactionRepository;
import org.apache.fineract.infrastructure.core.service.DateUtils;
import org.apache.fineract.infrastructure.jobs.exception.JobExecutionException;
import org.apache.fineract.notification.data.SmsTypeEnum;
import org.apache.fineract.notification.service.SmsNotificationWritePlatformService;
import org.apache.fineract.portfolio.client.domain.Client;
import org.apache.fineract.portfolio.savings.domain.*;
import org.apache.fineract.portfolio.savings.service.SavingsAccountReadPlatformService;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Component
public class ExecuteSavingsDormancyTasklet implements Tasklet {

    private static final long DEFAULT_DAYS_TO_INACTIVE = 30L;
    private static final long DEFAULT_DAYS_TO_DORMANCY = 60L;
    private static final long DEFAULT_DAYS_TO_ESCHEAT = 90L;

    private final SavingsAccountTransactionRepository savingsAccountTransactionRepository;
    private final SmsTransactionRepository smsTransactionRepository;
    private final SmsNotificationWritePlatformService smsNotificationWritePlatformService;
    private final SavingsAccountRepositoryWrapper savingsAccountRepositoryWrapper;
    private final SavingsAccountRepository savingsAccountRepository;
    private final SavingsAccountReadPlatformService savingAccountReadPlatformService;


    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {

        log.info("Started processing savings dormancy, {}", DateUtils.getLocalDateTimeOfTenant());

        LocalDate businessDate = DateUtils.getBusinessLocalDate();

        List<Throwable> exceptions = new ArrayList<>();

        processDefaultDormancy(businessDate, exceptions);
        processDormancyEnabled(businessDate, exceptions);

        if (!exceptions.isEmpty()) {
            throw new JobExecutionException(exceptions);
        }
        log.info("Completed processing savings dormancy, {}", DateUtils.getLocalDateTimeOfTenant());

        return RepeatStatus.FINISHED;
    }

    private void processDefaultDormancy(LocalDate businessDate, List<Throwable> exceptions) {

        List<Long> accounts = this.savingsAccountRepository.findAccountsForDormancyTracking();

        log.info("Savings Accounts: {}", accounts.size());

        if (CollectionUtils.isEmpty(accounts)) {
            return;
        }

        for (Long savingsAccountId : accounts) {

            try {

                processDefaultDormancyAccount(savingsAccountId, businessDate);

            } catch (Exception exception) {

                log.error("Failed to process default dormancy for savings account {}", savingsAccountId, exception);

                exceptions.add(exception);
            }
        }
    }

    public void processDefaultDormancyAccount(Long savingsAccountId, LocalDate businessDate) {

        SavingsAccount savingsAccount = this.savingsAccountRepositoryWrapper.findOneWithNotFoundDetection(savingsAccountId);

        LocalDate lastTransactionDate = this.savingsAccountTransactionRepository.findLastTransactionDate(savingsAccount.getId());

        /*
         * If the account has never had a transaction,
         * start counting from the activation date.
         */
        if (lastTransactionDate == null) {
            lastTransactionDate = savingsAccount.getActivatedOnDate();
        }

        if (lastTransactionDate == null) {
            log.warn("Skipping dormancy processing for savings account {} because no transaction or activation date exists.", savingsAccount.getAccountNumber());
            return;
        }

        long inactiveDays = ChronoUnit.DAYS.between(lastTransactionDate, businessDate);

        long daysToInactive = DEFAULT_DAYS_TO_INACTIVE;
        long daysToDormancy = DEFAULT_DAYS_TO_DORMANCY;
        long daysToEscheat = DEFAULT_DAYS_TO_ESCHEAT;

        log.info("Savings account {} last transaction={}, inactiveDays={}, thresholds inactive={}, dormant={}, escheat={}", savingsAccount.getAccountNumber(), lastTransactionDate, inactiveDays, daysToInactive,
                daysToDormancy, daysToEscheat);

        /*
         * Check the highest milestone first.
         *
         * This means an account that has been inactive for
         * 95 days gets the 90-day notification if it has not
         * already received it.
         */
        if (inactiveDays >= daysToEscheat) {

            sendSmsIfRequired(savingsAccount, SmsTypeEnum.SAVINGS_INACTIVE_90_DAYS);

            return;
        }

        if (inactiveDays >= daysToDormancy) {

            sendSmsIfRequired(savingsAccount, SmsTypeEnum.SAVINGS_INACTIVE_60_DAYS);

            return;
        }

        if (inactiveDays >= daysToInactive) {

            sendSmsIfRequired(savingsAccount, SmsTypeEnum.SAVINGS_INACTIVE_30_DAYS);
        }

    }

    private void processDormancyEnabled(LocalDate businessDate, List<Throwable> exceptions) {


        SavingsAccount savingsAccount = null;

        List<Long> savingsPendingInactive = savingAccountReadPlatformService.retrieveSavingsIdsPendingInactive(businessDate);
        log.info("Inactive Accounts: {}", savingsPendingInactive.size());

        if (savingsPendingInactive != null && !savingsPendingInactive.isEmpty()) {

            for (Long savingsId : savingsPendingInactive) {

                try {

                    savingsAccount = this.savingsAccountRepositoryWrapper.findOneWithNotFoundDetection(savingsId);

                    sendSmsIfRequired(savingsAccount, SmsTypeEnum.SAVINGS_INACTIVE_30_DAYS);

                } catch (Exception exception) {

                    log.error("Failed to process inactive SMS for savings account {}", savingsId, exception);
                    exceptions.add(exception);
                }
            }
        }

        List<Long> savingsPendingDormant = savingAccountReadPlatformService.retrieveSavingsIdsPendingDormant(businessDate);
        log.info("Dormant Accounts: {}", savingsPendingDormant.size());


        if (savingsPendingDormant != null && !savingsPendingDormant.isEmpty()) {

            for (Long savingsId : savingsPendingDormant) {

                try {

                    savingsAccount = this.savingsAccountRepositoryWrapper.findOneWithNotFoundDetection(savingsId);

                    sendSmsIfRequired(savingsAccount, SmsTypeEnum.SAVINGS_INACTIVE_60_DAYS);

                } catch (Exception exception) {

                    log.error("Failed to process dormant SMS for savings account {}", savingsId, exception);
                    exceptions.add(exception);
                }
            }
        }

        List<Long> savingsPendingEscheat = savingAccountReadPlatformService.retrieveSavingsIdsPendingEscheat(businessDate);

        log.info("Escheat Accounts: {}", savingsPendingEscheat.size());

        if (savingsPendingEscheat != null && !savingsPendingEscheat.isEmpty()) {

            for (Long savingsId : savingsPendingEscheat) {

                try {


                    savingsAccount = this.savingsAccountRepositoryWrapper.findOneWithNotFoundDetection(savingsId);

                    sendSmsIfRequired(savingsAccount, SmsTypeEnum.SAVINGS_INACTIVE_90_DAYS);

                } catch (Exception exception) {

                    log.error("Failed to process escheat SMS for savings account {}", savingsId, exception);
                    exceptions.add(exception);
                }
            }
        }
    }


    private void sendSmsIfRequired(SavingsAccount savingsAccount, SmsTypeEnum smsType) {

        Client client = savingsAccount.getClient();

        if (client == null) {
            log.info("Skipping dormancy SMS for savings account {} because it has no client.", savingsAccount.getAccountNumber());
            return;
        }

        String mobileNumber = client.getMobileNo();

        if (mobileNumber == null || mobileNumber.isBlank()) {
            log.info("Skipping dormancy SMS for savings account {} because client has no mobile number.", savingsAccount.getAccountNumber());
            return;
        }

        Long alreadySent = this.smsTransactionRepository.countSms(mobileNumber, smsType.getDescription());

        if (alreadySent > 0) {
            log.info("SMS {} already sent to mobile {}. Skipping.", smsType.getDescription(), mobileNumber);
            return;
        }

        log.info("Sending {} SMS for savings account {} to {}.", smsType.getDescription(), savingsAccount.getAccountNumber(), mobileNumber);

        this.smsNotificationWritePlatformService.processSavingsAccountSmsNotification(savingsAccount, smsType, null);
    }
}
