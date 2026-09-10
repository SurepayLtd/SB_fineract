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

import org.apache.fineract.infrastructure.security.data.OTPRequest;
import org.apache.fineract.notification.data.MamboSmsRequest;
import org.apache.fineract.notification.data.MamboSmsResponse;
import org.apache.fineract.notification.data.SmsNotificationData;
import org.apache.fineract.notification.data.SmsTypeEnum;
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

import java.math.BigDecimal;

public interface SmsNotificationWritePlatformService {

    void sendSms(SmsNotificationData smsNotificationData);

    void processLoanSmsNotification(Loan loan, SmsTypeEnum smsType, LoanTransaction transaction);

    void processSavingsAccountSmsNotification(SavingsAccount savingsAccount, SmsTypeEnum smsType, SavingsAccountTransaction transaction);

    void processOTPSmsNotification(AppUser user, OTPRequest request);

    void processClientSmsNotification(Client client, SmsTypeEnum smsTypeEnum, Integer otp, Integer otpExpiryMinutes);

    void processShareSmsNotification(ShareAccount shareAccount, SmsTypeEnum smsTypeEnum);

    void processFixedDepositSmsNotification(FixedDepositAccount fixedDepositAccount, SmsTypeEnum smsTypeEnum);

    void processAccountTransferSmsNotification(AccountTransferDetails details, SmsTypeEnum smsTypeEnum, BigDecimal amount);

    void processChargeSmsNotification(Client client, SmsTypeEnum smsTypeEnum, SavingsAccountCharge savingsAccountCharge, LoanCharge loanCharge, ClientCharge clientCharge);

    void processFailedUssdSmsNotification(Client client, SmsTypeEnum smsTypeEnum, SavingsAccount savingsAccount, Loan loan, BigDecimal amount);

    void processTransactionReversals(Client client, SmsTypeEnum smsTypeEnum, SavingsAccount savingsAccount, Loan loan, SavingsAccountTransaction savingsAccountTransaction, LoanTransaction loanTransaction);


}
