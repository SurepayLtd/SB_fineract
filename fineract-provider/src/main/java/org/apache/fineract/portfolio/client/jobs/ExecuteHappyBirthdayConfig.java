package org.apache.fineract.portfolio.client.jobs;

import org.apache.fineract.infrastructure.campaigns.sms.domain.SmsTransactionRepository;
import org.apache.fineract.infrastructure.jobs.service.JobName;
import org.apache.fineract.notification.service.SmsNotificationWritePlatformService;
import org.apache.fineract.portfolio.client.domain.ClientRepository;
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
public class ExecuteHappyBirthdayConfig {

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private SmsNotificationWritePlatformService smsNotificationWritePlatformService;

    @Autowired
    private SmsTransactionRepository smsTransactionRepository;


    @Bean
    protected Step executeHappyBirthdayStep() {
        return new StepBuilder(JobName.EXECUTE_STAFF_CLIENT_BIRTHDAYS.name(), jobRepository).tasklet(executeHappyBirthdayTasklet(), transactionManager).build();
    }

    @Bean
    public Job executeHappyBirthdayJob() {
        return new JobBuilder(JobName.EXECUTE_STAFF_CLIENT_BIRTHDAYS.name(), jobRepository).start(executeHappyBirthdayStep()).incrementer(new RunIdIncrementer())
                .build();
    }

    @Bean
    public ExecuteHappyBirthdayTasklet executeHappyBirthdayTasklet(){
        return new ExecuteHappyBirthdayTasklet(clientRepository, smsNotificationWritePlatformService, smsTransactionRepository);
    }
}
