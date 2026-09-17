package org.apache.fineract.portfolio.loanaccount.jobs.paymentoverdueremider;

import org.apache.fineract.cob.conditions.BatchWorkerCondition;
import org.apache.fineract.infrastructure.campaigns.sms.domain.SmsTransactionRepository;
import org.apache.fineract.infrastructure.core.exception.AbstractPlatformServiceUnavailableException;
import org.apache.fineract.infrastructure.springbatch.PropertyService;
import org.apache.fineract.notification.service.SmsNotificationWritePlatformService;
import org.apache.fineract.portfolio.loanaccount.data.LoanInstallmentReminderData;
import org.apache.fineract.portfolio.loanaccount.loanschedule.service.LoanInstallmentReminderReadService;
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

/**
 * Worker half of the loan reminder Installment job: it executes the instructions of one partition.
 *
 * <p>
 * The step is chunk oriented, so a chunk of instructions is attempted in a single transaction and, if any of them
 * fails, replayed one instruction per transaction — the fallback is Spring Batch's own, not hand written. Only
 * transient failures are retried; an instruction that cannot be paid is skipped and recorded against its mandate,
 * because a mandate that fails is an outcome of that mandate and not a failure of the run.
 * </p>
 */
@Configuration
@Conditional(BatchWorkerCondition.class)
public class ExecuteLoanInstallmentWorkerConfig {
    @Autowired
    private PlatformTransactionManager transactionManager;
    @Autowired
    private RemotePartitioningWorkerStepBuilderFactory stepBuilderFactory;
    @Autowired
    private QueueChannel inboundRequests;
    @Autowired
    private PropertyService propertyService;
    @Autowired
    private LoanInstallmentReminderReadService loanInstallmentReminderReadService;
    @Autowired
    private SmsNotificationWritePlatformService smsNotificationWritePlatformService;
    @Autowired
    private SmsTransactionRepository smsTransactionRepository;

    @Bean(name = ExecuteInstallmentReminderConstant.WORKER_STEP)
    public Step executeStandingInstructionsWorkerStep() {
        final int chunkSize = propertyService.getChunkSize(ExecuteInstallmentReminderConstant.JOB_NAME);
        return stepBuilderFactory.get(ExecuteInstallmentReminderConstant.WORKER_STEP)
                .inputChannel(inboundRequests)
                .<LoanInstallmentReminderData, LoanInstallmentReminderData>chunk(chunkSize, transactionManager) //
                .reader(loanInstallmentReminderItemReader())
                .processor(loanInstallmentReminderProcessor())
                .writer(loanInstallmentReminderWriter())
                .faultTolerant()
                .retry(TransientDataAccessException.class)
                .retry(ConcurrencyFailureException.class) //
                .retry(AbstractPlatformServiceUnavailableException.class) //
                .retryLimit(propertyService.getRetryLimit(ExecuteInstallmentReminderConstant.JOB_NAME)) //
                .skipPolicy(new LoanInstallmentSkipPolicy()) //
                .build();
    }

    @Bean
    @StepScope
    public LoanInstallmentReminderItemReader loanInstallmentReminderItemReader() {
        int pageSize = propertyService.getChunkSize(ExecuteInstallmentReminderConstant.JOB_NAME);
        return new LoanInstallmentReminderItemReader(loanInstallmentReminderReadService, pageSize);
    }

    @Bean
    @StepScope
    public LoanInstallmentReminderProcessor loanInstallmentReminderProcessor() {
        return new LoanInstallmentReminderProcessor(smsTransactionRepository);
    }

    @Bean
    @StepScope
    public LoanInstallmentReminderWriter loanInstallmentReminderWriter() {
        return new LoanInstallmentReminderWriter(smsNotificationWritePlatformService);
    }


}
