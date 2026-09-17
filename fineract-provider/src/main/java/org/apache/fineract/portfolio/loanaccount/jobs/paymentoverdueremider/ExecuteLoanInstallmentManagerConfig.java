package org.apache.fineract.portfolio.loanaccount.jobs.paymentoverdueremider;

import org.apache.fineract.cob.conditions.BatchManagerCondition;
import org.apache.fineract.infrastructure.jobs.service.JobName;
import org.apache.fineract.infrastructure.springbatch.PropertyService;
import org.apache.fineract.portfolio.loanaccount.loanschedule.service.LoanInstallmentReminderReadService;
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

/**
 * Manager half of the loan-installment job: it cuts the due set into partitions and hands them to workers.
 */
@Configuration
@EnableBatchIntegration
@Conditional(BatchManagerCondition.class)
public class ExecuteLoanInstallmentManagerConfig {

    @Autowired
    private JobRepository jobRepository;
    @Autowired
    private RemotePartitioningManagerStepBuilderFactory stepBuilderFactory;
    @Autowired
    private PropertyService propertyService;
    @Autowired
    private DirectChannel outboundRequests;
    @Autowired
    private LoanInstallmentReminderReadService loanInstallmentReminderReadService;

    @Bean
    public LoanInstallmentReminderPartitioner loanInstallmentReminderPartitioner() {
        return new LoanInstallmentReminderPartitioner(propertyService, loanInstallmentReminderReadService);
    }

    @Bean
    public Step executeLoanInstallmentReminderPartitionerStep() {
        return stepBuilderFactory.get(ExecuteInstallmentReminderConstant.PARTITIONER_STEP)
                .partitioner(ExecuteInstallmentReminderConstant.WORKER_STEP, loanInstallmentReminderPartitioner())
                .pollInterval(propertyService.getPollInterval(ExecuteInstallmentReminderConstant.JOB_NAME)).outputChannel(outboundRequests)
                .build();
    }

    @Bean
    public Job executeLoanInstallmentReminderJob() {
        return new JobBuilder(JobName.EXECUTE_LOAN_INSTALLMENT_PAYMENT_REMINDER.name(), jobRepository)
                .start(executeLoanInstallmentReminderPartitionerStep()).incrementer(new RunIdIncrementer()).build();
    }
}
