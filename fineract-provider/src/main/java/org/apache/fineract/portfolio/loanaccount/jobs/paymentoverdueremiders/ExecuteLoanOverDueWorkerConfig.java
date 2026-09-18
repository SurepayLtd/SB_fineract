package org.apache.fineract.portfolio.loanaccount.jobs.paymentoverdueremiders;

import org.apache.fineract.cob.conditions.BatchWorkerCondition;
import org.apache.fineract.infrastructure.campaigns.sms.domain.SmsTransactionRepository;
import org.apache.fineract.infrastructure.core.exception.AbstractPlatformServiceUnavailableException;
import org.apache.fineract.notification.service.SmsNotificationWritePlatformService;
import org.apache.fineract.portfolio.loanaccount.data.LoanInstallmentOverdueReminderData;
import org.apache.fineract.portfolio.loanaccount.jobs.paymentdueremider.ExecuteBatchJobConstant;
import org.apache.fineract.portfolio.loanaccount.jobs.paymentdueremider.LoanInstallmentSkipPolicy;
import org.apache.fineract.portfolio.loanaccount.loanschedule.service.BatchSmSReadService;
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
public class ExecuteLoanOverDueWorkerConfig {

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

    private static final int CHUNCK_SIZE = 100;
    private static final int PARTITION_SIZE = 100;


    @Bean(name = ExecuteBatchJobConstant.LOAN_INSTALLMENT_OVERDUE_WORKER_STEP)
    public Step executeLoanInstallmentOverdueReminderWorkerStep() {
        return stepBuilderFactory.get(ExecuteBatchJobConstant.LOAN_INSTALLMENT_OVERDUE_WORKER_STEP)
                .inputChannel(inboundRequests)//
                .<LoanInstallmentOverdueReminderData, LoanInstallmentOverdueReminderData>chunk(CHUNCK_SIZE, transactionManager) //
                .reader(loanOverDueItemReader())//
                .processor(loanOverDueItemProcessor())//
                .writer(loanOverDueItemWriter())//
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
    public LoanOverDueItemReader loanOverDueItemReader() {
        return new LoanOverDueItemReader(batchSmSReadService, PARTITION_SIZE);
    }

    @Bean
    @StepScope
    public LoanOverDueItemProcessor loanOverDueItemProcessor() {
        return new LoanOverDueItemProcessor(smsTransactionRepository);
    }

    @Bean
    @StepScope
    public LoanOverDueItemWriter loanOverDueItemWriter() {
        return new LoanOverDueItemWriter(smsNotificationWritePlatformService);
    }
}
