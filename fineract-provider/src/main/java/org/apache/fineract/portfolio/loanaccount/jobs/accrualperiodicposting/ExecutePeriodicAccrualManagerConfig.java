package org.apache.fineract.portfolio.loanaccount.jobs.accrualperiodicposting;


import org.apache.fineract.cob.conditions.BatchManagerCondition;
import org.apache.fineract.infrastructure.configuration.domain.ConfigurationDomainService;
import org.apache.fineract.infrastructure.jobs.service.JobName;
import org.apache.fineract.portfolio.loanaccount.jobs.paymentdueremider.ExecuteBatchJobConstant;
import org.apache.fineract.portfolio.loanaccount.service.LoanAccrualBatchReadService;
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

//@Configuration
//@EnableBatchIntegration
//@Conditional(BatchManagerCondition.class)
//public class ExecutePeriodicAccrualManagerConfig {
//
//    private static final int POLL_SIZE = 500;
//
//
//    @Autowired
//    private JobRepository jobRepository;
//    @Autowired
//    private RemotePartitioningManagerStepBuilderFactory stepBuilderFactory;
//    @Autowired
//    private DirectChannel outboundRequests;
//    @Autowired
//    private LoanAccrualBatchReadService loanAccrualBatchReadService;
//    @Autowired
//    private ConfigurationDomainService configurationDomainService;
//
//
//
//    @Bean
//    public PeriodicAccrualItemPartitioner periodicAccrualItemPartitioner() {
//        return new PeriodicAccrualItemPartitioner(loanAccrualBatchReadService, configurationDomainService);
//    }
//
//    @Bean
//    public Step executeAddPeriodicAccrualPartitionerStep() {
//        return stepBuilderFactory.get(ExecuteBatchJobConstant.PERIODIC_ACCRUAL_PARTITIONER_STEP)
//                .partitioner(ExecuteBatchJobConstant.PERIODIC_ACCRUAL_WORKER_STEP, periodicAccrualItemPartitioner())
//                .pollInterval(POLL_SIZE)
//                .outputChannel(outboundRequests)
//                .build();
//    }
//
//    @Bean
//    public Job executeAddPeriodicAccrualJob() {
//        return new JobBuilder(JobName.ADD_PERIODIC_ACCRUAL_ENTRIES.name(), jobRepository)
//                .start(executeAddPeriodicAccrualPartitionerStep())
//                .incrementer(new RunIdIncrementer())
//                .build();
//    }
//}
