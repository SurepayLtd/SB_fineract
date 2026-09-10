/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.fineract.notification.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.apache.fineract.infrastructure.campaigns.sms.domain.SmsProvider;
import org.apache.fineract.infrastructure.campaigns.sms.domain.SmsTransaction;
import org.apache.fineract.infrastructure.campaigns.sms.domain.SmsTransactionRepository;
import org.apache.fineract.infrastructure.configuration.api.GlobalConfigurationConstants;
import org.apache.fineract.infrastructure.configuration.domain.GlobalConfigurationProperty;
import org.apache.fineract.infrastructure.configuration.domain.GlobalConfigurationRepositoryWrapper;
import org.apache.fineract.infrastructure.core.exception.GeneralPlatformDomainRuleException;
import org.apache.fineract.infrastructure.core.exception.PlatformDataIntegrityException;
import org.apache.fineract.infrastructure.core.service.ThreadLocalContextUtil;
import org.apache.fineract.infrastructure.security.data.OTPRequest;
import org.apache.fineract.infrastructure.security.service.TwoFactorConfigurationService;
import org.apache.fineract.notification.data.MamboSmsRequest;
import org.apache.fineract.notification.data.MamboSmsResponse;
import org.apache.fineract.notification.data.SmsNotificationData;
import org.apache.fineract.notification.data.SmsTypeEnum;
import org.apache.fineract.notification.domain.MamboSms;
import org.apache.fineract.notification.domain.MamboSmsRepository;
import org.apache.fineract.notification.domain.SMSNotification;
import org.apache.fineract.notification.domain.SMSNotificationRepository;
import org.apache.fineract.portfolio.account.domain.AccountTransferDetails;
import org.apache.fineract.portfolio.account.domain.AccountTransferTransaction;
import org.apache.fineract.portfolio.client.domain.Client;
import org.apache.fineract.portfolio.client.domain.ClientCharge;
import org.apache.fineract.portfolio.loanaccount.domain.Loan;
import org.apache.fineract.portfolio.loanaccount.domain.LoanCharge;
import org.apache.fineract.portfolio.loanaccount.domain.LoanTransaction;
import org.apache.fineract.portfolio.savings.domain.FixedDepositAccount;
import org.apache.fineract.portfolio.savings.domain.SavingsAccount;
import org.apache.fineract.portfolio.savings.domain.SavingsAccountCharge;
import org.apache.fineract.portfolio.savings.domain.SavingsAccountTransaction;
import org.apache.fineract.portfolio.shareaccounts.domain.ShareAccount;
import org.apache.fineract.useradministration.domain.AppUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

@RequiredArgsConstructor
@Slf4j
public class SMSNotificationWritePlatformServiceImpl implements SmsNotificationWritePlatformService {

    @Autowired
    private Environment env;
    @Autowired
    private final GlobalConfigurationRepositoryWrapper configurationRepositoryWrapper;
    @Autowired
    private TwoFactorConfigurationService twoFactorConfigurationService;
    public static final String FORM_URL_CONTENT_TYPE = "application/json";

    private final SMSNotificationRepository smsNotificationRepository;
    private final MamboSmsRepository mamboSmsRepository;
    private final SmsTransactionRepository smsTransactionRepository;

    private static final String txnId = "TXN" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) +
            ThreadLocalRandom.current().nextInt(100, 999);


    @Override
    public void sendSms(SmsNotificationData smsNotificationData) {

        boolean surePayEnabled = configurationRepositoryWrapper.findOneByNameWithNotFoundDetection(GlobalConfigurationConstants.ENABLE_SMS_NOTIFICATIONS)
                .isEnabled();

        boolean mamboEnabled = configurationRepositoryWrapper.findOneByNameWithNotFoundDetection(GlobalConfigurationConstants.ENABLE_MAMBO_SMS_NOTIFICATIONS)
                .isEnabled();

        if (surePayEnabled && mamboEnabled) {
            throw new GeneralPlatformDomainRuleException("error.msg.sms.multiple.providers.enabled", "Only one SMS provider can be enabled at a time.");
        }

        if (mamboEnabled) {
            var mamboSmsRequest = new MamboSmsRequest(
                    smsNotificationData.getMessage(), smsNotificationData.getPhoneNumber()
            );
            var response = sendMamboSms(mamboSmsRequest);

            boolean success = Boolean.TRUE.equals(response.getSuccess());

            String errorMessage = null;

            if (!success && response.getMessages() != null) {
                errorMessage = String.join(", ", response.getMessages());
            }

            recordSmsTransaction(smsNotificationData, SmsProvider.MAMBO.getDescription(), success, errorMessage);
            return;
        }

        if (surePayEnabled) {
            sendSurePaySms(smsNotificationData);
            return;
        }

        log.info("No SMS provider enabled for tenant {}", ThreadLocalContextUtil.getTenant().getName());

    }

    private void sendSurePaySms(SmsNotificationData smsNotificationData){

        final GlobalConfigurationProperty property = this.configurationRepositoryWrapper
                .findOneByNameWithNotFoundDetection(GlobalConfigurationConstants.ENABLE_SMS_NOTIFICATIONS);

        if (property.isEnabled()) {
            Gson gson = new GsonBuilder().create();

            Optional<SMSNotification> smsNotification = this.smsNotificationRepository.findById(1L);

            if (smsNotification.isEmpty()){
                throw new GeneralPlatformDomainRuleException("error.msg.sms.failed.due.missing.activation.details",
                        "SMS sending has failed due to missing activation credentials.");
            }

            smsNotificationData.setSender(smsNotification.get().getVendorCode());
            smsNotificationData.setService(getConfigProperty("sms.service"));
            smsNotificationData.setPassword(smsNotification.get().getVendorPassword());

//            log.info("Vendor: {}, password: {}", smsNotificationData.getSender(), smsNotificationData.getPassword());


            String notificationObj = gson.toJson(smsNotificationData);
            log.info("SMS Message is constructed :=> " + notificationObj);

            HttpUrl.Builder urlBuilder = HttpUrl.parse(getConfigProperty("sms.url")).newBuilder();
            String url = urlBuilder.build().toString();

            log.info("SMS URL :=>" + url);
            OkHttpClient client = new OkHttpClient();
            Response response = null;

            RequestBody formBody = RequestBody.create(MediaType.parse(FORM_URL_CONTENT_TYPE), notificationObj);

            Request request = new Request.Builder().url(url).post(formBody).build();

            List<Throwable> exceptions = new ArrayList<>();

            try {
                response = client.newCall(request).execute();
                String resObject = response.body().string();
                if (response.isSuccessful()) {

                    log.info("Sms Message Response :=>" + resObject);

                    recordSmsTransaction(smsNotificationData, SmsProvider.SUREPAY.getDescription(), true, null);

                } else {
                    log.error("Failed to deliver sms message notification :" + resObject);

                    handleAPIIntegrityIssues(resObject);

                    recordSmsTransaction(smsNotificationData, SmsProvider.SUREPAY.getDescription(), false, resObject);

                }
            } catch (Exception e) {
                log.error("Posting sms notification has failed " + e);
                recordSmsTransaction(smsNotificationData, SmsProvider.SUREPAY.getDescription(), false, e.getMessage());
                exceptions.add(e);
            }
        } else {
            log.info("** SMS Notification is disabled for this Tenant :-> " + ThreadLocalContextUtil.getTenant().getName());
        }
    }


    private MamboSmsResponse sendMamboSms(MamboSmsRequest mamboSmsRequest) {

        MamboSmsResponse smsResponse = new MamboSmsResponse();

        final GlobalConfigurationProperty property = configurationRepositoryWrapper.findOneByNameWithNotFoundDetection(GlobalConfigurationConstants.ENABLE_MAMBO_SMS_NOTIFICATIONS);

        if (property.isEnabled()) {

            MamboSms mamboSms = mamboSmsRepository.findById(1L)
                    .orElseThrow(() -> new GeneralPlatformDomainRuleException("error.msg.sms.failed.due.missing.activation.details",
                            "SMS sending has failed due to missing activation credentials."));

            mamboSmsRequest.setSender_id("MamboSMS");
            mamboSmsRequest.setMessage_category("non_customised");

            log.info("MamboBody: {}", mamboSmsRequest);

            try {
                RestTemplate restTemplate = new RestTemplate();

                String url = getConfigProperty("mambo.api.url");

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
                headers.set("Authorization", mamboSms.getApiKey());

                HttpEntity<MamboSmsRequest> entity = new HttpEntity<>(mamboSmsRequest, headers);

                ResponseEntity<MamboSmsResponse> responseEntity = restTemplate.exchange(url, HttpMethod.POST, entity, MamboSmsResponse.class);

                log.info("MamboResponse: {}", responseEntity);

                if (responseEntity.getBody() != null) {
                    return handleResponse(responseEntity);
                }

                return smsResponse
                        .setStatusCode(String.valueOf(responseEntity.getStatusCode().value()))
                        .setSuccess(false)
                        .setMessages(List.of("Empty response received from Mambo SMS API."));

            } catch (HttpClientErrorException e) {
                log.error("Mambo client error: {}", e.getMessage());
                return handleClientError(e);

            } catch (HttpServerErrorException e) {
                log.error("Mambo server error: {}", e.getMessage());

                return smsResponse
                        .setStatusCode("500")
                        .setSuccess(false)
                        .setMessages(List.of("Mambo SMS server encountered an internal error."));

            } catch (ResourceAccessException e) {
                log.error("Unable to reach Mambo SMS API", e);

                return smsResponse
                        .setStatusCode("503")
                        .setSuccess(false)
                        .setMessages(List.of("Unable to connect to Mambo SMS service."));

            } catch (Exception e) {
                log.error("Unexpected Mambo SMS error", e);

                return smsResponse
                        .setStatusCode("500")
                        .setSuccess(false)
                        .setMessages(List.of("Unexpected error while sending SMS."));
            }
        }else {
            log.info("** Mambo SMS Notification is disabled for this Tenant :-> " + ThreadLocalContextUtil.getTenant().getName());
            return smsResponse
                    .setSuccess(false)
                    .setMessages(List.of("Mambo Sms disbled for this Tenant"))
                    .setStatusCode("400");
        }
    }

    private String getConfigProperty(String propertyName) {
        return this.env.getProperty(propertyName);
    }

    private void handleAPIIntegrityIssues(String httpResponse) {
        throw new PlatformDataIntegrityException(httpResponse, httpResponse);
    }

    @Override
    public void processLoanSmsNotification(Loan loan, SmsTypeEnum smsType, LoanTransaction transaction) {

        smsPropertyEnabled();

        String clientName = null;
        if (loan.client() != null){
            clientName = loan.client().getDisplayName();
        }else{
            clientName = loan.group().getName();
        }

        String mobileNo = null;
        if (loan.client() != null) {
            mobileNo = loan.client().getMobileNo();
        }else{
            mobileNo = loan.group().getActiveClientMembers()
                    .stream()
                    .map(Client::getMobileNo)
                    .filter(Objects::nonNull)
                    .filter(phone -> !phone.trim().isEmpty())
                    .findFirst()
                    .orElse(null);
        }

        String message = null;
        String messageId = null;
        boolean isMandatory = smsType.getStatus().equalsIgnoreCase("Mandatory");

        switch (smsType) {
            case LOAN_SUBMISSION:
                final GlobalConfigurationProperty submitLoan = this.configurationRepositoryWrapper
                        .findOneByNameWithNotFoundDetection(GlobalConfigurationConstants.SEND_SMS_NOTIFICATION_WHEN_LOAN_CREATED);
                if (submitLoan.isEnabled() || isMandatory) {
                    message = String.format(
                            "Dear %s, your loan application of %s %s has been received successfully. Ref: %s.",
                            clientName, loan.getCurrency().getCode(), loan.getProposedPrincipal(), loan.getAccountNumber());
                    messageId = String.format("LOAN-SUBMISSION-%s", loan.getId());
                }
            break;
            case USSD_LOAN_APPLICATION:
                final GlobalConfigurationProperty submitUssdLoan = this.configurationRepositoryWrapper
                        .findOneByNameWithNotFoundDetection(GlobalConfigurationConstants.SEND_SMS_NOTIFICATION_ON_USSD_LOAN_APPLICATION);
                if (submitUssdLoan.isEnabled() || isMandatory) {
                    message = String.format(
                            "Dear %s, your loan application of %s %s,  has been received successfully . Your ref %s",
                            clientName, loan.getCurrency().getCode(), loan.getProposedPrincipal(), loan.getAccountNumber());
                    messageId = String.format("LOAN-SUBMISSION-%s", loan.getId());
                }
            break;
            case LOAN_APPROVAL:
                final GlobalConfigurationProperty approvedLoan = this.configurationRepositoryWrapper
                        .findOneByNameWithNotFoundDetection(GlobalConfigurationConstants.SEND_SMS_NOTIFICATION_WHEN_LOAN_APPROVED);
                if (approvedLoan.isEnabled() || isMandatory) {
                    message = String.format(
                            "Dear %s, your loan application %s has been approved. Please contact %s for more information.",
                            clientName, loan.getAccountNumber(), ThreadLocalContextUtil.getTenant().getName());
                    messageId = String.format("LOAN-APPROVAL-%s", loan.getId());
                }
            break;
            case LOAN_DISBURSEMENT:
                final GlobalConfigurationProperty disburseLoan = this.configurationRepositoryWrapper
                        .findOneByNameWithNotFoundDetection(GlobalConfigurationConstants.SEND_SMS_NOTIFICATION_WHEN_LOAN_DISBURSED);
                if (disburseLoan.isEnabled() || isMandatory) {
                    message = String.format(
                            "Dear %s, %s %s has been successfully disbursed for your loan %s.", clientName,
                            loan.getCurrencyCode(), loan.getApprovedPrincipal(), loan.getAccountNumber());
                    messageId = String.format("LOAN-DISBURSEMENT-%s", loan.getId());
                }
            break;
            case LOAN_REJECTED:
                final GlobalConfigurationProperty rejectedLoan = this.configurationRepositoryWrapper
                        .findOneByNameWithNotFoundDetection(GlobalConfigurationConstants.SEND_SMS_NOTIFICATION_WHEN_LOAN_REJECTED);
                if (rejectedLoan.isEnabled() || isMandatory) {
                    message = String.format(
                            "Dear %s, your loan application %s has been rejected. Please contact %s for more information.",
                            clientName, loan.getAccountNumber(), ThreadLocalContextUtil.getTenant().getName());
                    messageId = String.format("LOAN-REJECTED-%s", loan.getId());
                }
            break;
            case LOAN_REPAYMENT:
                final GlobalConfigurationProperty repaymentLoan = this.configurationRepositoryWrapper
                        .findOneByNameWithNotFoundDetection(GlobalConfigurationConstants.SEND_SMS_NOTIFICATION_WHEN_LOAN_REPAYMENT);
                if (repaymentLoan.isEnabled() || isMandatory) {
                    message = String.format(
                            "Dear %s, we have received your loan repayment of %s %s. Outstanding balance: %s %s.",
                            clientName, loan.getCurrencyCode(), transaction.getAmount(), loan.getCurrencyCode(), loan.getSummary().getTotalOutstanding());
                    messageId = String.format("LOAN-REPAYMENT-%s", transaction.getId());
                }
            break;
            case LOAN_CLOSED:
                final GlobalConfigurationProperty closeLoan = this.configurationRepositoryWrapper
                        .findOneByNameWithNotFoundDetection(GlobalConfigurationConstants.SEND_SMS_NOTIFICATION_WHEN_LOAN_CLOSED);
                if (closeLoan.isEnabled() || isMandatory) {
                    message = String.format(
                            "Congratulations %s! Your loan %s has been fully repaid and successfully closed.", clientName,
                            loan.getAccountNumber());
                    messageId = String.format("LOAN-CLOSED-%s", transaction.getId());
                }
            break;
            case LOAN_CREATION:
                message = String.format("Dear %s, your loan account %s has been successfully created.", clientName,
                        loan.getAccountNumber());
                messageId = String.format("LOAN-CREATED-%s", loan.getId());

            break;
            default:
                log.info("No sms type found to process a notification");
                return;

        }

        if (mobileNo != null && messageId != null) {
            sendSms(new SmsNotificationData(mobileNo, message, messageId, smsType.getDescription(), smsType.getStatus()));
        }
    }

    @Override
    public void processSavingsAccountSmsNotification(SavingsAccount savingsAccount, SmsTypeEnum smsType, SavingsAccountTransaction transaction) {

        smsPropertyEnabled();

        String clientName = null;
        if (savingsAccount.getClient() != null){
            clientName = savingsAccount.getClient().getDisplayName();
        }else {
            clientName = savingsAccount.getGroup().getName();
        }

        String mobileNo =  null;
        if (savingsAccount.getClient() != null){
            mobileNo =  savingsAccount.getClient().getMobileNo();
        }else {
            mobileNo = savingsAccount.getGroup().getActiveClientMembers()
                    .stream()
                    .map(Client::getMobileNo)
                    .filter(Objects::nonNull)
                    .filter(phone -> !phone.trim().isEmpty())
                    .findFirst()
                    .orElse(null);
        }

        String message = null;
        String messageId = null;
        boolean isMandatory = smsType.getStatus().equalsIgnoreCase("Mandatory");


        switch (smsType) {
            case SAVINGS_DEPOSIT:
                final GlobalConfigurationProperty deposit = this.configurationRepositoryWrapper.findOneByNameWithNotFoundDetection(
                        GlobalConfigurationConstants.SEND_SMS_NOTIFICATION_WHEN_SAVINGS_ACCOUNT_DEPOSIT);
                if (deposit.isEnabled() || isMandatory) {
                    message = String.format("Dear %s, %s %s has been deposited into account %s. New balance: UGX %s. Ref: %s.", clientName,
                            savingsAccount.getCurrency().getCode(), transaction.getAmount().setScale(2, RoundingMode.HALF_UP),
                            savingsAccount.getAccountNumber(), savingsAccount.getAccountBalance(), txnId);
                    messageId = String.format("SAVINGS-DEPOSIT-%s", transaction.getId());
                }
            break;
            case SAVINGS_WITHDRAW:
                final GlobalConfigurationProperty withdrawal = this.configurationRepositoryWrapper.findOneByNameWithNotFoundDetection(
                        GlobalConfigurationConstants.SEND_SMS_NOTIFICATION_WHEN_SAVINGS_ACCOUNT_WITHDRAW);
                if (withdrawal.isEnabled() || isMandatory) {
                    message = String.format("Dear %s, %s %s has been withdrawn from account %s. New balance: UGX %s. Ref: %s.", clientName,
                            savingsAccount.getCurrency().getCode(), transaction.getAmount().setScale(2, RoundingMode.HALF_UP), savingsAccount.getAccountNumber(),
                            savingsAccount.getAccountBalance(), txnId);
                    messageId = String.format("SAVINGS-WITHDRAW-%s", transaction.getId());
                }
            break;
            case SAVINGS_CREATION:
                final GlobalConfigurationProperty creation = this.configurationRepositoryWrapper.findOneByNameWithNotFoundDetection(
                        GlobalConfigurationConstants.SEND_SMS_NOTIFICATION_WHEN_SAVINGS_ACCOUNT_CREATION);
                if (creation.isEnabled() || isMandatory) {

                    message = String.format("Dear %s, your savings account %s has been successfully created", clientName,
                            savingsAccount.getAccountNumber());
                    messageId = String.format("SAVINGS-CREATION-%s", savingsAccount.getId());
                }
            break;
            case SAVINGS_ACTIVATED:
                final GlobalConfigurationProperty approval = this.configurationRepositoryWrapper.findOneByNameWithNotFoundDetection(
                        GlobalConfigurationConstants.SEND_SMS_NOTIFICATION_WHEN_SAVINGS_ACCOUNT_ACTIVATED);
                if (approval.isEnabled() || isMandatory) {
                    message = String.format("Dear %s, your account has been successfully Activated. Welcome to %s ! ", clientName,
                            ThreadLocalContextUtil.getTenant().getName());
                    messageId = String.format("SAVINGS-ACTIVATED-%s", savingsAccount.getId());
                }
            break;
            case SAVINGS_INTEREST_POSTED:
                final GlobalConfigurationProperty interest = this.configurationRepositoryWrapper.findOneByNameWithNotFoundDetection(
                        GlobalConfigurationConstants.SEND_SMS_NOTIFICATION_WHEN_INTEREST_CREDITED);
                if (interest.isEnabled() || isMandatory) {
                    message = String.format("Dear %s, interest of %s %s has been credited to your account %s.", clientName,
                            savingsAccount.getCurrency().getCode(), transaction.getAmount().setScale(2, RoundingMode.HALF_UP),
                            savingsAccount.getAccountNumber());
                    messageId = String.format("SAVINGS-INTEREST-%s", transaction.getId());
                }
                break;
            default:
                log.info("No sms type found to process a notification");
                return;
        }

        if (mobileNo != null && messageId != null) {
            sendSms(new SmsNotificationData(mobileNo, message, messageId, smsType.getDescription(), smsType.getStatus()));
        }
    }

    @Override
    public void processOTPSmsNotification(AppUser user, OTPRequest request) {

        smsPropertyEnabled();

        log.info(" :: -> Generated OTP message for user {} is {}", user.getUsername(), request.getToken());

        if (user.getStaff() == null) {
            log.warn("User {} does not have staff associated, cannot send OTP SMS", user.getUsername());
            return;
        }

        String mobileNo = user.getStaff().mobileNo();
        if (mobileNo == null) {
            log.warn("User {} staff does not have mobile number, cannot send OTP SMS", user.getUsername());
            return;
        }

        String message = twoFactorConfigurationService.getFormattedSmsTextFor(user, request);
        log.info("Message: {}", message);
        String messageId = String.format("OTP-TOKEN-%s", user.getId());
        var event = SmsTypeEnum.TWO_FACTOR_OTP;
        log.info("Generated OTP message for user {} is {}", user.getUsername(), message);
        sendSms(new SmsNotificationData(mobileNo, message, messageId, event.getDescription(), event.getStatus()));
    }

    @Override
    public void processClientSmsNotification(Client client, SmsTypeEnum smsTypeEnum, Integer otp, Integer otpExpiryMinutes) {
        smsPropertyEnabled();

        String mobileNo = client != null ? client.getMobileNo() : null;
        String message = null;
        String messageId = null;
        Long clientId = client != null ? client.getId() : null;
        String clientName = client !=null ? client.getDisplayName() : null;

        switch (smsTypeEnum){
            case CLIENT_CREATION:
                message = String.format("Dear %s, your member profile has been successfully created with %s",
                        clientName, ThreadLocalContextUtil.getTenant().getName());
                messageId = String.format("CLIENT-CREATION-%s", clientId);
            break;

            case CLIENT_PIN:
                message = String.format("Dear %s, Momo Payment PIN has been setup successfully on Surebanker!",
                        clientName);
                messageId = String.format("CLIENT-PIN-%s", clientId);
            break;

            case ACTIVATE_MOMO_PAYMENT_OTP:
                message = String.format("Dear %s, Here is the OTP to Activate you're account on Surebanker %s!",
                        clientName, otp);
                messageId = String.format("CLIENT-MOMO-OTP-%s", clientId);
            break;

            case DEACTIVATE_MOMO_PAYMENT:
                message = String.format("Dear %s, Momo Payment has been de-activated from you're account !",
                        clientName);
                messageId = String.format("DEACTIVATED-MOMO-PIN-%s", clientId);
            break;

            case UNBLOCK_CLIENT_PIN:
                message = String.format("Hello %s, your Mobile Banking PIN has been unblocked."+
                        "Use OTP %s to set a new PIN. Expires in %s minutes. Do not share.",
                        clientName, otp, otpExpiryMinutes
                );
                messageId = String.format("UNBLOCK-CLIENT-PIN-%s", clientId);
            break;

            case RESET_CLIENT_PIN:
                message = String.format("Hello %s, your Mobile Banking PIN has been updated successfully.",
                        clientName
                );
                messageId = String.format("RESET-CLIENT-PIN-%s", clientId);
            break;
            case SELF_SERVICE_PIN_CHANGE:
                message = String.format("Hello %s, your Mobile Banking PIN has been updated successfully.",
                        clientName
                );
                messageId = String.format("SELF_SERVICE_PIN_CHANGE-%s", clientId);
            break;
            case FAILED_MAX_PIN_ATTEMPTS:
                message = String.format("Hello %s, Alert: Your USSD PIN has been blocked after multiple unsuccessful PIN attempts."
                        + "Please contact %s for support",
                        clientName, ThreadLocalContextUtil.getTenant().getName()
                );
                messageId = String.format("USSD_PIN_BLOCKED-%s", clientId);
                break;
            default:
                log.info("No sms type found to process a notification");
                return;


        }

        if (mobileNo != null && messageId != null) {

            sendSms(new SmsNotificationData(mobileNo, message, messageId, smsTypeEnum.getDescription(), smsTypeEnum.getStatus()));
        }
    }

    @Override
    public void processShareSmsNotification(ShareAccount shareAccount, SmsTypeEnum smsTypeEnum) {
        smsPropertyEnabled();

        String mobileNo = shareAccount != null ? shareAccount.getClient().getMobileNo() : null;
        String message = null;
        String messageId = null;
        Long shareId = shareAccount != null ? shareAccount.getId() : null;
        String clientName = shareAccount !=null ? shareAccount.getClient().getDisplayName() : null;

        switch (smsTypeEnum){
            case SHARE_ACCOUNT_CREATION:
                assert shareAccount != null;
                message = String.format("Dear %s, your share account %s has been successfully created",
                        clientName, shareAccount.getAccountNumber()
                );
                messageId = String.format("SHARE-ACCOUNT-%s", shareId);
            break;
            default:
                log.info("No sms type found to process a notification");
            return;

        }
        if (mobileNo != null && messageId != null) {

            sendSms(new SmsNotificationData(mobileNo, message, messageId, smsTypeEnum.getDescription(), smsTypeEnum.getStatus()));
        }
    }

    @Override
    public void processFixedDepositSmsNotification(FixedDepositAccount fixedDepositAccount, SmsTypeEnum smsTypeEnum) {
        smsPropertyEnabled();

        String mobileNo = fixedDepositAccount != null ? fixedDepositAccount.getClient().getMobileNo() : null;
        String message = null;
        String messageId = null;
        Long depositId = fixedDepositAccount != null ? fixedDepositAccount.getId() : null;
        String clientName = fixedDepositAccount !=null ? fixedDepositAccount.getClient().getDisplayName() : null;

        switch (smsTypeEnum){
            case FIXED_DEPOSIT_CREATION:
                assert fixedDepositAccount != null;
                message = String.format("Dear %s, your fixed deposit account %s has been successfully activated. Amount: %s %s.",
                        clientName, fixedDepositAccount.getAccountNumber(), fixedDepositAccount.getCurrency().getCode(), fixedDepositAccount.getDepositAmount().setScale(2, RoundingMode.HALF_UP)
                );
                messageId = String.format("FIXED-DEPOSIT-ACCOUNT-%s", depositId);
            break;
            default:
                log.info("No sms type found to process a notification");
            return;

        }
        if (mobileNo != null && messageId != null) {

            sendSms(new SmsNotificationData(mobileNo, message, messageId, smsTypeEnum.getDescription(), smsTypeEnum.getStatus()));
        }
    }

    @Override
    public void processAccountTransferSmsNotification(AccountTransferDetails details, SmsTypeEnum smsTypeEnum, BigDecimal amount) {
        smsPropertyEnabled();

        String senderMobileNo = details != null ? details.fromClient().getMobileNo() : null;
        String receiverMobileNo = details != null ? details.toClient().getMobileNo() : null;
        String senderMessage = null;
        String receiveMessage = null;
        String messageId = null;
        Long id = details !=null ? details.getId() : null;
        String senderClientName = details !=null ? details.fromClient().getDisplayName() : null;
        String receiverClientName = details !=null ? details.toClient().getDisplayName() : null;

        switch (smsTypeEnum){
            case SAVINGS_TO_SAVINGS_ACCOUNT_TRANSFER:
                assert details != null;
                senderMessage = String.format("Dear %s, %s %s has been successfully debited from account %s. Ref: %s.",
                        senderClientName, details.fromSavingsAccount().getCurrency().getCode(), amount.setScale(2, RoundingMode.HALF_UP),
                        details.fromSavingsAccount().getAccountNumber(), txnId
                );
                receiveMessage = String.format("Dear %s, %s %s has been successfully credited to account %s. Ref: %s.",
                        receiverClientName, details.toSavingsAccount().getCurrency().getCode(), amount.setScale(2, RoundingMode.HALF_UP),
                        details.toSavingsAccount().getAccountNumber(), txnId
                );
                messageId = String.format("ACCOUNT-TRANSFER-ACCOUNT-%s", id);
            break;
            case SAVINGS_TO_LOAN_ACCOUNT_TRANSFER:
                assert details != null;
                senderMessage = String.format("Dear %s, %s %s has been successfully debited from account %s for repayment of loan %s. Ref: %s.",
                        senderClientName, details.fromSavingsAccount().getCurrency().getCode(), amount.setScale(2, RoundingMode.HALF_UP),
                        details.fromSavingsAccount().getAccountNumber(), details.toLoanAccount().getAccountNumber(), txnId
                );
                receiveMessage = String.format("Dear %s, %s %s has been successfully credited to loan account %s as a repayment from account %s. Ref: %s.",
                        receiverClientName, details.toLoanAccount().getCurrency().getCode(), amount.setScale(2, RoundingMode.HALF_UP),
                        details.toLoanAccount().getAccountNumber(), details.fromSavingsAccount().getAccountNumber(), txnId
                );
                messageId = String.format("ACCOUNT-TRANSFER-ACCOUNT-%s", id);
            break;
            case LOAN_TO_SAVINGS_ACCOUNT_TRANSFER:
                assert details != null;
                senderMessage = String.format("Dear %s, %s %s has been successfully debited from loan account %s. Ref: %s.",
                        senderClientName, details.fromLoanAccount().getCurrency().getCode(), amount.setScale(2, RoundingMode.HALF_UP),
                        details.fromLoanAccount().getAccountNumber(), txnId
                );
                receiveMessage = String.format("Dear %s, %s %s has been successfully credited to saving account %s . Ref: %s.",
                        receiverClientName,details.toSavingsAccount().getCurrency().getCode(), amount.setScale(2, RoundingMode.HALF_UP),
                        details.toSavingsAccount().getAccountNumber(), txnId
                );
                messageId = String.format("ACCOUNT-TRANSFER-ACCOUNT-%s", id);
            break;
            case LOAN_TO_LOAN_ACCOUNT_TRANSFER:
                assert details != null;
                senderMessage = String.format("Dear %s, %s %s has been successfully debited from loan account %s. Ref: %s.",
                        senderClientName, details.fromLoanAccount().getCurrency().getCode(), amount.setScale(2, RoundingMode.HALF_UP),
                        details.fromLoanAccount().getAccountNumber(), txnId
                );
                receiveMessage = String.format("Dear %s, %s %s has been successfully credited to loan account %s . Ref: %s.",
                        receiverClientName, details.toLoanAccount().getCurrency().getCode(), amount.setScale(2, RoundingMode.HALF_UP),
                        details.toLoanAccount().getAccountNumber(), txnId
                );
                messageId = String.format("ACCOUNT-TRANSFER-ACCOUNT-%s", id);
            break;
            default:
                log.info("No sms type found to process a notification");
                return;

        }
        if (senderMobileNo != null && messageId != null) {

            sendSms(new SmsNotificationData(senderMobileNo, senderMessage, messageId, smsTypeEnum.getDescription(), smsTypeEnum.getStatus()));
        }
        if (receiverMobileNo != null){
            sendSms(new SmsNotificationData(receiverMobileNo, receiveMessage, messageId, smsTypeEnum.getDescription(), smsTypeEnum.getStatus()));
        }
    }

    @Override
    public void processChargeSmsNotification(Client client, SmsTypeEnum smsTypeEnum, SavingsAccountCharge savingsAccountCharge, LoanCharge loanCharge, ClientCharge clientCharge) {

        smsPropertyEnabled();

        String mobileNo = client != null ? client.getMobileNo() : null;
        String clientName = client != null ? client.getDisplayName() : null;

        String message = null;
        String messageId = null;
        boolean isMandatory = smsTypeEnum.getStatus().equalsIgnoreCase("Mandatory");

        switch (smsTypeEnum) {
            case LOAN_CHARGE_WAIVED:
                final GlobalConfigurationProperty loan = this.configurationRepositoryWrapper.findOneByNameWithNotFoundDetection(
                        GlobalConfigurationConstants.SEND_SMS_NOTIFICATION_WHEN_CHARGE_WAIVED);
                if (loan.isEnabled() || isMandatory) {
                    message = String.format("Dear %s, a charge of %s %s on your loan account %s has been waived.", clientName,
                            loanCharge.currencyCode(), loanCharge.getAmount(), loanCharge.getLoan().getAccountNumber()
                            );
                    messageId = String.format("LOAN-CHARGE-WAIVED-%s", loanCharge.getId());
                }
            break;

            case SAVINGS_CHARGE_WAIVED:
                final GlobalConfigurationProperty savings = this.configurationRepositoryWrapper.findOneByNameWithNotFoundDetection(
                        GlobalConfigurationConstants.SEND_SMS_NOTIFICATION_WHEN_CHARGE_WAIVED);
                if (savings.isEnabled() || isMandatory) {
                    message = String.format("Dear %s, a charge of %s %s on your saving account %s has been waived.", clientName,
                            savingsAccountCharge.currencyCode(), savingsAccountCharge.getCharge().getAmount(), savingsAccountCharge.savingsAccount().getAccountNumber()
                    );
                    messageId = String.format("SAVINGS-CHARGE-WAIVED-%s", savingsAccountCharge.getId());
                }
                break;

            case CLIENT_CHARGE_WAIVED:
                final GlobalConfigurationProperty charge = this.configurationRepositoryWrapper.findOneByNameWithNotFoundDetection(
                        GlobalConfigurationConstants.SEND_SMS_NOTIFICATION_WHEN_CHARGE_WAIVED);
                if (charge.isEnabled() || isMandatory) {
                    message = String.format("Dear %s, a charge of %s %s on your client account %s has been waived.", clientName,
                            clientCharge.getCurrency().getCode(), clientCharge.getAmount(), clientCharge.getClient().getAccountNumber()
                    );
                    messageId = String.format("CLIENT-CHARGE-WAIVED-%s", clientCharge.getId());
                }
                break;

            case CLIENT_CHARGE_APPLIED:
                message = String.format("Dear %s, a charge of %s %s has been applied to your client account %s.", clientName,
                        clientCharge.getCurrency().getCode(), clientCharge.getAmount(), clientCharge.getClient().getAccountNumber()
                );
                messageId = String.format("CLIENT-CHARGE-APPLIED-%s", clientCharge.getId());

            break;

            case SAVINGS_CHARGE_APPLIED:
                message = String.format("Dear %s, a charge of %s %s has been applied to your savings account %s.", clientName,
                        savingsAccountCharge.currencyCode(), savingsAccountCharge.getCharge().getAmount(), savingsAccountCharge.savingsAccount().getAccountNumber()
                );
                messageId = String.format("SAVINGS-CHARGE-APPLIED-%s", savingsAccountCharge.getId());
            break;

            case LOAN_CHARGE_APPLIED:
                message = String.format("Dear %s, a charge of %s %s has been applied to your loan account %s.", clientName,
                        loanCharge.currencyCode(), loanCharge.getAmount(), loanCharge.getLoan().getAccountNumber()
                );
                messageId = String.format("LOAN-CHARGE-APPLIED-%s", loanCharge.getId());

            break;

            default:
                log.info("No sms type found to process a notification");
            return;
        }

        if (mobileNo != null && messageId != null) {

            sendSms(new SmsNotificationData(mobileNo, message, messageId, smsTypeEnum.getDescription(), smsTypeEnum.getStatus()));
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processFailedUssdSmsNotification(Client client, SmsTypeEnum smsTypeEnum, SavingsAccount savingsAccount, Loan loan, BigDecimal amount) {
        smsPropertyEnabled();

        String mobileNo = client != null ? client.getMobileNo() : null;
        String clientName = client != null ? client.getDisplayName() : null;

        String message = null;
        String messageId = null;
        switch (smsTypeEnum){

            case DEPOSIT_FAILURE_VIA_USSD:
                message = String.format("Dear %s, your deposit transaction of %s %s was unsuccessful. Ref: %s.", clientName,
                        savingsAccount.getCurrency().getCode(), amount.setScale(2, RoundingMode.HALF_UP), txnId);
                messageId = String.format("SAVINGS-DEPOSIT-USSD-FAILURE-%s", savingsAccount.getId());

            break;
            case LOAN_REPAYMENT_FAILURE_VIA_USSD:
                message = String.format("Dear %s, your loan repayment of %s %s was unsuccessful. Please try again. Ref: %s.", clientName,
                        loan.getCurrency().getCode(), amount.setScale(2, RoundingMode.HALF_UP), txnId);
                messageId = String.format("LOAN-REPAYMENT-USSD-FAILURE-%s", loan.getId());

                break;

            default:
                log.info("No sms type found to process a notification");
                return;
        }
        if (mobileNo != null && messageId != null) {

            sendSms(new SmsNotificationData(mobileNo, message, messageId, smsTypeEnum.getDescription(), smsTypeEnum.getStatus()));
        }
    }

    @Override
    public void processTransactionReversals(Client client, SmsTypeEnum smsTypeEnum, SavingsAccount savingsAccount, Loan loan, SavingsAccountTransaction savingsAccountTransaction, LoanTransaction loanTransaction) {

        smsPropertyEnabled();

        String mobileNo = client != null ? client.getMobileNo() : null;
        String clientName = client != null ? client.getDisplayName() : null;

        String message = null;
        String messageId = null;

        switch (smsTypeEnum){
            case SAVINGS_REVERSAL:
                message = String.format("Dear %s, your transaction of %s %s on saving account %s has been reversed. Ref: %s.", clientName,
                        savingsAccount.getCurrency().getCode(), savingsAccountTransaction.getAmount().setScale(2, RoundingMode.HALF_UP), savingsAccount.getAccountNumber(), txnId);
                messageId = String.format("SAVING-REVERSAL-%s", savingsAccount.getId());
            break;

            case LOAN_REVERSAL:
                message = String.format("Dear %s, your transaction of %s %s on loan account %s has been reversed. Ref: %s.", clientName,
                        loan.getCurrency().getCode(), loanTransaction.getAmount().setScale(2, RoundingMode.HALF_UP), loan.getAccountNumber(), txnId);
                messageId = String.format("LOAN-REVERSAL-%s", loan.getId());
            break;

            default:
                log.info("No sms type found to process a notification");
                return;
        }

        if (mobileNo != null && messageId != null) {

            sendSms(new SmsNotificationData(mobileNo, message, messageId, smsTypeEnum.getDescription(), smsTypeEnum.getStatus()));
        }
    }

    private MamboSmsResponse handleResponse(ResponseEntity<MamboSmsResponse> responseEntity) {

        MamboSmsResponse response = responseEntity.getBody();

        if (response == null) {
            response = new MamboSmsResponse();
        }

        int status = responseEntity.getStatusCode().value();

        switch (status) {

            case 200:
                response.setStatusCode("200");
                response.setSuccess(Boolean.TRUE.equals(response.getSuccess()));
                if (response.getMessages() == null || response.getMessages().isEmpty()) {
                    response.setMessages(List.of("Request processed successfully."));
                }
                break;

            case 201:
                response.setStatusCode("201");
                response.setSuccess(true);
                if (response.getMessages() == null || response.getMessages().isEmpty()) {
                    response.setMessages(List.of("SMS sent successfully."));
                }
                break;

            default:
                response.setStatusCode(String.valueOf(status));
                response.setSuccess(false);
                response.setMessages(List.of("Unexpected response from Mambo SMS."));
        }

        return response;
    }
    private MamboSmsResponse handleClientError(HttpClientErrorException exception) {

        MamboSmsResponse response = new MamboSmsResponse();

        int status = exception.getStatusCode().value();

        response.setStatusCode(String.valueOf(status));
        response.setSuccess(false);

        switch (status) {

            case 400:
                response.setMessages(List.of("Bad request. Missing or invalid parameters."));
                break;

            case 401:
                response.setMessages(List.of("Unauthorized. Invalid or missing API key."));
                break;

            case 405:
                response.setMessages(List.of("Method not allowed. Invalid HTTP method."));
                break;

            default:
                response.setMessages(List.of(exception.getResponseBodyAsString()));
        }

        return response;
    }

    private void smsPropertyEnabled(){
        boolean surePayEnabled = configurationRepositoryWrapper.findOneByNameWithNotFoundDetection(GlobalConfigurationConstants.ENABLE_SMS_NOTIFICATIONS)
                .isEnabled();

        boolean mamboEnabled = configurationRepositoryWrapper.findOneByNameWithNotFoundDetection(GlobalConfigurationConstants.ENABLE_MAMBO_SMS_NOTIFICATIONS)
                .isEnabled();

        if (!surePayEnabled && !mamboEnabled) {
            log.info("** SMS Notification is disabled for this Tenant :-> " + ThreadLocalContextUtil.getTenant().getName());
            return;
        }
    }

    private void recordSmsTransaction(final SmsNotificationData smsNotificationData, final String smsProvider, final boolean success, final String errorMessage) {

        final String smsEvent = smsNotificationData.getSmsEvent();
        final String smsId = smsNotificationData.getMessageId();
        final String mobileNumber = smsNotificationData.getPhoneNumber();
        final String message = smsNotificationData.getMessage();
        final String requirement = smsNotificationData.getRequirement();

        final Integer messageLength = message != null ? message.length() : 0;

        SmsTransaction transaction;

        if (success) {
            transaction = SmsTransaction.createSmsTransaction(smsEvent, mobileNumber, smsId, messageLength, smsProvider, requirement);
        } else {
            transaction = SmsTransaction.recordSmsFailure(smsEvent, mobileNumber, smsId, errorMessage, messageLength, smsProvider, requirement);
        }

        this.smsTransactionRepository.save(transaction);
    }

}
