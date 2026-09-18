package org.apache.fineract.portfolio.savings.jobs.updatesavingsdormantaccounts;

import org.apache.fineract.cob.conditions.BatchManagerCondition;
import org.apache.fineract.portfolio.loanaccount.jobs.paymentdueremider.ExecuteBatchJobConstant;
import org.apache.fineract.portfolio.loanaccount.loanschedule.service.BatchSmSReadService;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.integration.config.annotation.EnableBatchIntegration;
import org.springframework.batch.integration.partition.RemotePartitioningManagerStepBuilderFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.channel.DirectChannel;

@Configuration
@EnableBatchIntegration
@Conditional(BatchManagerCondition.class)
public class ExecuteSavingsDormancyManagerConfig {
    private static final int POLL_SIZE = 500;

    @Autowired
    private JobRepository jobRepository;
    @Autowired
    private RemotePartitioningManagerStepBuilderFactory stepBuilderFactory;

    @Autowired
    private DirectChannel outboundRequests;
    @Autowired
    private BatchSmSReadService batchSmSReadService;

    @Bean
    public SavingsDormancyPartitioner savingsDormancyPartitioner() {
        return new SavingsDormancyPartitioner(batchSmSReadService);
    }

    @Bean
    public Step executeDormantSavingsAccountReminderPartitionerStep() {
        return stepBuilderFactory.get(ExecuteBatchJobConstant.SAVINGS_DORMANCY_PARTITIONER_STEP)
                .partitioner(ExecuteBatchJobConstant.SAVINGS_DORMANCY_WORKER_STEP, savingsDormancyPartitioner())
                .pollInterval(POLL_SIZE)
                .outputChannel(outboundRequests)
                .build();
    }

    @Bean
    public Job executeDormantSavingsAccountReminderJob() {
        return new JobBuilder(ExecuteBatchJobConstant.SAVINGS_DORMANCY_JOB_NAME, jobRepository)
                .start(executeDormantSavingsAccountReminderPartitionerStep())
                .incrementer(new RunIdIncrementer())
                .build();
    }
}
