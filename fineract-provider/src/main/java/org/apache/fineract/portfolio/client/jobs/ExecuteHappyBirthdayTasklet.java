package org.apache.fineract.portfolio.client.jobs;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.fineract.infrastructure.campaigns.sms.domain.SmsTransactionRepository;
import org.apache.fineract.infrastructure.core.service.DateUtils;
import org.apache.fineract.infrastructure.jobs.exception.JobExecutionException;
import org.apache.fineract.notification.data.SmsTypeEnum;
import org.apache.fineract.notification.service.SmsNotificationWritePlatformService;
import org.apache.fineract.portfolio.client.domain.Client;
import org.apache.fineract.portfolio.client.domain.ClientRepository;
import org.apache.fineract.portfolio.client.domain.ClientStatus;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class ExecuteHappyBirthdayTasklet implements Tasklet {

    private final ClientRepository clientRepository;
    private final SmsNotificationWritePlatformService smsNotificationWritePlatformService;
    private final SmsTransactionRepository smsTransactionRepository;

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        log.info("Started processing Happy Birthday SMS, {}", DateUtils.getLocalDateTimeOfTenant());

        final List<Throwable> exceptions = new ArrayList<>();

        final List<Client> birthdayClients = this.clientRepository.findBirthdays(ClientStatus.ACTIVE.getValue(), false);

        final List<Client> birthdayStaff = this.clientRepository.findBirthdays(ClientStatus.ACTIVE.getValue(), true);

        log.info("Found {} client birthday(s) and {} staff birthday(s).", birthdayClients.size(), birthdayStaff.size());

        processBirthdays(birthdayClients, exceptions, "client");
        processBirthdays(birthdayStaff, exceptions, "staff");

        if (!exceptions.isEmpty()) {

            log.error("Happy Birthday processing completed with {} failure(s).", exceptions.size());

            throw new JobExecutionException(exceptions);
        }

        log.info("Completed processing Happy Birthday SMS, {}", DateUtils.getLocalDateTimeOfTenant());

        return RepeatStatus.FINISHED;
    }

    private void processBirthdays(List<Client> clients, List<Throwable> exceptions, String recipientType) {

        if (CollectionUtils.isEmpty(clients)) {
            return;
        }

        LocalDateTime now = DateUtils.getLocalDateTimeOfTenant();
        LocalDateTime startOfDay = now.toLocalDate().atStartOfDay();
        LocalDateTime startOfTomorrow = startOfDay.plusDays(1);

        for (Client client : clients) {

            try {

                Long birthdaySmsCount = smsTransactionRepository.existsBirthdaySms(client.getMobileNo(), SmsTypeEnum.HAPPY_BIRTHDAY.getDescription(),
                        startOfDay, startOfTomorrow
                );

                if (birthdaySmsCount > 0) {
                    log.info("Happy Birthday SMS already sent to client id={}, mobile={}. Skipping.", client.getId(), client.getMobileNo());
                    continue;
                }

                log.info("Sending Happy Birthday SMS to {} id={}, name={}, mobile={}", recipientType, client.getId(), client.getDisplayName(), client.getMobileNo());

                this.smsNotificationWritePlatformService.processClientSmsNotification(client, SmsTypeEnum.HAPPY_BIRTHDAY, null, null);

            } catch (Exception exception) {

                log.error("Failed to send Happy Birthday SMS to {} id={}, name={}, mobile={}", recipientType, client.getId(), client.getDisplayName(), client.getMobileNo(), exception);

                exceptions.add(exception);
            }
        }
    }
}
