package org.apache.fineract.portfolio.savings.jobs.updatesavingsdormantaccounts;

import org.apache.fineract.cob.conditions.BatchWorkerCondition;
import org.apache.fineract.infrastructure.campaigns.sms.domain.SmsTransactionRepository;
import org.apache.fineract.infrastructure.core.exception.AbstractPlatformServiceUnavailableException;
import org.apache.fineract.notification.service.SmsNotificationWritePlatformService;
import org.apache.fineract.portfolio.loanaccount.jobs.paymentdueremider.ExecuteBatchJobConstant;
import org.apache.fineract.portfolio.loanaccount.jobs.paymentdueremider.LoanInstallmentSkipPolicy;
import org.apache.fineract.portfolio.loanaccount.loanschedule.service.BatchSmSReadService;
import org.apache.fineract.portfolio.savings.data.SavingsDormancyReminderData;
import org.apache.fineract.portfolio.savings.domain.SavingsAccountRepositoryWrapper;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.integration.partition.RemotePartitioningWorkerStepBuilderFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@Conditional(BatchWorkerCondition.class)
public class ExecuteSavingsDormancyWorkerConfig {

    @Autowired
    private PlatformTransactionManager transactionManager;
    @Autowired
    private RemotePartitioningWorkerStepBuilderFactory stepBuilderFactory;
    @Autowired
    private QueueChannel inboundRequests;
    @Autowired
    private BatchSmSReadService batchSmSReadService;
    @Autowired
    private SmsNotificationWritePlatformService smsNotificationWritePlatformService;
    @Autowired
    private SmsTransactionRepository smsTransactionRepository;
    @Autowired
    private SavingsAccountRepositoryWrapper savingsAccountRepositoryWrapper;

    private static final int CHUNCK_SIZE = 100;
    private static final int PARTITION_SIZE = 100;



    @Bean(name = ExecuteBatchJobConstant.SAVINGS_DORMANCY_WORKER_STEP)
    public Step executeDormantSavingsAccountReminderWorkerStep() {
        return stepBuilderFactory.get(ExecuteBatchJobConstant.SAVINGS_DORMANCY_WORKER_STEP)
                .inputChannel(inboundRequests)//
                .<SavingsDormancyReminderData, SavingsDormancyReminderData>chunk(CHUNCK_SIZE, transactionManager) //
                .reader(savingsDormancyItemReader())//
                .processor(savingsDormancyProcessor())//
                .writer(savingsDormancyWriter())//
                .faultTolerant()//
                .retry(TransientDataAccessException.class)//
                .retry(ConcurrencyFailureException.class) //
                .retry(AbstractPlatformServiceUnavailableException.class) //
                .retryLimit(5) //
                .skipPolicy(new LoanInstallmentSkipPolicy()) //
                .build();
    }

    @Bean
    @StepScope
    public SavingsDormancyItemReader savingsDormancyItemReader() {
        return new SavingsDormancyItemReader(batchSmSReadService, PARTITION_SIZE);
    }

    @Bean
    @StepScope
    public SavingsDormancyProcessor savingsDormancyProcessor() {
        return new SavingsDormancyProcessor(smsTransactionRepository);
    }

    @Bean
    @StepScope
    public SavingsDormancyWriter savingsDormancyWriter() {
        return new SavingsDormancyWriter(smsNotificationWritePlatformService, savingsAccountRepositoryWrapper);
    }
}
