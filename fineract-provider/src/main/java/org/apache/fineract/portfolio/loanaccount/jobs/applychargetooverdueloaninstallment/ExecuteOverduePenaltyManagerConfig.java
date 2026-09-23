package org.apache.fineract.portfolio.loanaccount.jobs.applychargetooverdueloaninstallment;

import org.apache.fineract.cob.conditions.BatchManagerCondition;
import org.apache.fineract.infrastructure.configuration.domain.ConfigurationDomainService;
import org.apache.fineract.portfolio.loanaccount.jobs.paymentdueremider.ExecuteBatchJobConstant;
import org.apache.fineract.portfolio.loanaccount.service.LoanReadPlatformService;
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
public class ExecuteOverduePenaltyManagerConfig {

    private static final int POLL_SIZE = 500;


    @Autowired
    private JobRepository jobRepository;
    @Autowired
    private RemotePartitioningManagerStepBuilderFactory stepBuilderFactory;
    @Autowired
    private DirectChannel outboundRequests;
    @Autowired
    private LoanReadPlatformService loanReadPlatformService;
    @Autowired
    private ConfigurationDomainService configurationDomainService;



    @Bean
    public OverdueLoanPartitioner overdueLoanPartitioner() {
        return new OverdueLoanPartitioner(loanReadPlatformService, configurationDomainService);
    }

    @Bean
    public Step executeApplyChargeToOverdueLoanInstallmentPartitionerStep() {
        return stepBuilderFactory.get(ExecuteBatchJobConstant.APPLY_CHARGE_TO_OVERDUE_LOAN_INSTALLMENT_PARTITIONER_STEP)
                .partitioner(ExecuteBatchJobConstant.APPLY_CHARGE_TO_OVERDUE_LOAN_INSTALLMENT_WORKER_STEP, overdueLoanPartitioner())
                .pollInterval(POLL_SIZE)
                .outputChannel(outboundRequests)
                .build();
    }

    @Bean
    public Job executeApplyChargeToOverdueLoanInstallmentJob() {
        return new JobBuilder(ExecuteBatchJobConstant.APPLY_CHARGE_TO_OVERDUE_LOAN_INSTALLMENT_JOB_NAME, jobRepository)
                .start(executeApplyChargeToOverdueLoanInstallmentPartitionerStep())
                .incrementer(new RunIdIncrementer())
                .build();
    }
}