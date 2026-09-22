package org.apache.fineract.portfolio.loanaccount.jobs.applychargetooverdueloaninstallment;

import org.apache.fineract.cob.conditions.BatchWorkerCondition;
import org.apache.fineract.infrastructure.configuration.domain.ConfigurationDomainService;
import org.apache.fineract.infrastructure.core.exception.AbstractPlatformServiceUnavailableException;
import org.apache.fineract.portfolio.loanaccount.jobs.paymentdueremider.ExecuteBatchJobConstant;
import org.apache.fineract.portfolio.loanaccount.jobs.paymentdueremider.LoanInstallmentSkipPolicy;
import org.apache.fineract.portfolio.loanaccount.loanschedule.data.OverdueLoanScheduleData;
import org.apache.fineract.portfolio.loanaccount.service.LoanChargeWritePlatformService;
import org.apache.fineract.portfolio.loanaccount.service.LoanReadPlatformService;
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
public class ExecutePeriodicAccrualWorkerConfig {

    @Autowired
    private PlatformTransactionManager transactionManager;
    @Autowired
    private RemotePartitioningWorkerStepBuilderFactory stepBuilderFactory;
    @Autowired
    private QueueChannel inboundRequests;
    @Autowired
    private LoanReadPlatformService loanReadPlatformService;
    @Autowired
    private LoanChargeWritePlatformService loanChargeWritePlatformService;
    @Autowired
    private ConfigurationDomainService configurationDomainService;


    private static final int CHUNCK_SIZE = 100;
    private static final int PARTITION_SIZE = 100;


    @Bean(name = ExecuteBatchJobConstant.APPLY_CHARGE_TO_OVERDUE_LOAN_INSTALLMENT_WORKER_STEP)
    public Step executeApplyChargeToOverdueLoanInstallmentWorkerStep() {
        return stepBuilderFactory.get(ExecuteBatchJobConstant.APPLY_CHARGE_TO_OVERDUE_LOAN_INSTALLMENT_WORKER_STEP)
                .inputChannel(inboundRequests)//
                .<OverdueLoanScheduleData, OverdueLoanScheduleData>chunk(CHUNCK_SIZE, transactionManager) //
                .reader(overdueLoanItemReader())//
                .processor(overdueLoanProcessor())//
                .writer(overdueLoanWriter())//
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
    public OverdueLoanItemReader overdueLoanItemReader() {
        return new OverdueLoanItemReader(loanReadPlatformService, configurationDomainService, PARTITION_SIZE);
    }

    @Bean
    @StepScope
    public OverdueLoanProcessor overdueLoanProcessor() {
        return new OverdueLoanProcessor();
    }

    @Bean
    @StepScope
    public OverdueLoanWriter overdueLoanWriter() {
        return new OverdueLoanWriter(loanChargeWritePlatformService);
    }
}
