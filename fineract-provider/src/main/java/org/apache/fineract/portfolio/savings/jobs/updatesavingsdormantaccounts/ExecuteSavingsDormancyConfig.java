package org.apache.fineract.portfolio.savings.jobs.updatesavingsdormantaccounts;

import org.apache.fineract.infrastructure.campaigns.sms.domain.SmsTransactionRepository;
import org.apache.fineract.infrastructure.jobs.service.JobName;
import org.apache.fineract.notification.service.SmsNotificationWritePlatformService;
import org.apache.fineract.portfolio.client.domain.ClientRepository;
import org.apache.fineract.portfolio.client.jobs.ExecuteHappyBirthdayTasklet;
import org.apache.fineract.portfolio.savings.domain.SavingsAccountRepository;
import org.apache.fineract.portfolio.savings.domain.SavingsAccountRepositoryWrapper;
import org.apache.fineract.portfolio.savings.domain.SavingsAccountTransactionRepository;
import org.apache.fineract.portfolio.savings.service.SavingsAccountReadPlatformService;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class ExecuteSavingsDormancyConfig {

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private SmsNotificationWritePlatformService smsNotificationWritePlatformService;

    @Autowired
    private SmsTransactionRepository smsTransactionRepository;

    @Autowired
    private SavingsAccountTransactionRepository savingsAccountTransactionRepository;

    @Autowired
    private SavingsAccountRepositoryWrapper savingsAccountRepositoryWrapper;

    @Autowired
    private SavingsAccountRepository savingsAccountRepository;

    @Autowired
    private SavingsAccountReadPlatformService savingAccountReadPlatformService;


    @Bean
    protected Step executeDormantAccountSmsStep() {
        return new StepBuilder(JobName.EXECUTE_DORMANT_ACCOUNTS_SMS.name(), jobRepository).tasklet(executeSavingsDormancyTasklet(), transactionManager).build();
    }

    @Bean
    public Job executeDormantAccountSmsJob() {
        return new JobBuilder(JobName.EXECUTE_DORMANT_ACCOUNTS_SMS.name(), jobRepository).start(executeDormantAccountSmsStep()).incrementer(new RunIdIncrementer())
                .build();
    }

    @Bean
    public ExecuteSavingsDormancyTasklet executeSavingsDormancyTasklet(){
        return new ExecuteSavingsDormancyTasklet(savingsAccountTransactionRepository, smsTransactionRepository, smsNotificationWritePlatformService, savingsAccountRepositoryWrapper, savingsAccountRepository, savingAccountReadPlatformService);
    }
}
