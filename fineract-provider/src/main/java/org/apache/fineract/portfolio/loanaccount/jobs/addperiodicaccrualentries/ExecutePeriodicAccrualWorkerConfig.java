package org.apache.fineract.portfolio.loanaccount.jobs.addperiodicaccrualentries;


import org.apache.fineract.cob.conditions.BatchWorkerCondition;
import org.apache.fineract.infrastructure.configuration.domain.ConfigurationDomainService;
import org.apache.fineract.infrastructure.core.exception.AbstractPlatformServiceUnavailableException;
import org.apache.fineract.portfolio.loanaccount.data.LoanAccrualData;
import org.apache.fineract.portfolio.loanaccount.jobs.paymentdueremider.ExecuteBatchJobConstant;
import org.apache.fineract.portfolio.loanaccount.service.LoanAccrualBatchReadService;
import org.apache.fineract.portfolio.loanaccount.service.LoanAccrualsProcessingService;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.integration.partition.RemotePartitioningWorkerStepBuilderFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.apache.fineract.portfolio.loanaccount.jobs.paymentdueremider.LoanInstallmentSkipPolicy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@Conditional(BatchWorkerCondition.class)
public class ExecutePeriodicAccrualWorkerConfig {

    @Autowired
    private PlatformTransactionManager transactionManager;
    @Autowired
    private RemotePartitioningWorkerStepBuilderFactory stepBuilderFactory;
    @Autowired
    private QueueChannel inboundRequests;
    @Autowired
    private LoanAccrualBatchReadService loanAccrualBatchReadService;
    @Autowired
    private LoanAccrualsProcessingService loanAccrualsProcessingService;
    @Autowired
    private ConfigurationDomainService configurationDomainService;


    private static final int CHUNCK_SIZE = 100;
    private static final int PARTITION_SIZE = 100;


    @Bean(name = ExecuteBatchJobConstant.PERIODIC_ACCRUAL_WORKER_STEP)
    public Step executeAddPeriodicAccrualWorkerStep() {
        return stepBuilderFactory.get(ExecuteBatchJobConstant.PERIODIC_ACCRUAL_WORKER_STEP)
                .inputChannel(inboundRequests)//
                .<LoanAccrualData, LoanAccrualData>chunk(CHUNCK_SIZE, transactionManager) //
                .reader(periodicAccrualItemReader())//
                .processor(periodicAccrualItemProcessor())//
                .writer(periodicAccrualItemWriter())//
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
    public PeriodicAccrualItemReader periodicAccrualItemReader() {
        return new PeriodicAccrualItemReader(PARTITION_SIZE, configurationDomainService, loanAccrualBatchReadService);
    }

    @Bean
    @StepScope
    public PeriodicAccrualItemProcessor periodicAccrualItemProcessor() {
        return new PeriodicAccrualItemProcessor();
    }

    @Bean
    @StepScope
    public PeriodicAccrualItemWriter periodicAccrualItemWriter() {
        return new PeriodicAccrualItemWriter(loanAccrualsProcessingService);
    }
}
