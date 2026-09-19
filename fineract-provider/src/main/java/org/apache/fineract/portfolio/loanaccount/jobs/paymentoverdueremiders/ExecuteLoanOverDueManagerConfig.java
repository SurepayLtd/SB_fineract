package org.apache.fineract.portfolio.loanaccount.jobs.paymentoverdueremiders;

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
public class ExecuteLoanOverDueManagerConfig {

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
    public LoanOverDueItemPartitioner loanOverDueItemPartitioner() {
        return new LoanOverDueItemPartitioner(batchSmSReadService);
    }

    @Bean
    public Step executeLoanInstallmentOverdueReminderPartitionerStep() {
        return stepBuilderFactory.get(ExecuteBatchJobConstant.LOAN_INSTALLMENT_OVERDUE_PARTITIONER_STEP)
                .partitioner(ExecuteBatchJobConstant.LOAN_INSTALLMENT_OVERDUE_WORKER_STEP, loanOverDueItemPartitioner())
                .pollInterval(POLL_SIZE)
                .outputChannel(outboundRequests)
                .build();
    }

    @Bean
    public Job executeLoanInstallmentOverdueReminderJob() {
        return new JobBuilder(ExecuteBatchJobConstant.LOAN_INSTALLMENT_OVERDUE_JOB_NAME, jobRepository)
                .start(executeLoanInstallmentOverdueReminderPartitionerStep())
                .incrementer(new RunIdIncrementer())
                .build();
    }
}
