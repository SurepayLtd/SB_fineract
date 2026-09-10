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
package org.apache.fineract.notification.data;

import lombok.Getter;

@Getter
public enum SmsTypeEnum {

    LOAN_SUBMISSION(1, "SmsTypeEnum.loanSubmission", "Loan Submission", "Optional"), //
    LOAN_APPROVAL(2, "SmsTypeEnum.loanApproval", "Loan Approval", "Mandatory"), //
    LOAN_DISBURSEMENT(3, "SmsTypeEnum.loanDisbursement", "Loan Disbursement", "Mandatory"), //
    LOAN_REPAYMENT(4, "SmsTypeEnum.loanRepayment", "Loan Repayment", "Mandatory"), //
    LOAN_REJECTED(5, "SmsTypeEnum.loanRejected", "Loan Application Rejected", "Mandatory"), //
    LOAN_CLOSED(6, "SmsTypeEnum.loanClosed", "Loan Account Fully Paid and Closed", "Optional"),
    SAVINGS_DEPOSIT(7, "SmsTypeEnum.savingsDeposit", "Savings Account Deposit", "Mandatory"), //
    SAVINGS_WITHDRAW(8, "SmsTypeEnum.savingsWithdraw", "Savings Account Withdraw", "Mandatory"), //
    SAVINGS_CREATION(9, "SmsTypeEnum.savingsCreation", "Savings Account Creation", "Mandatory"), //
    SAVINGS_ACTIVATED(10, "SmsTypeEnum.savingsActivated", "Savings Account Activated", "Optional"), //
    USSD_LOAN_APPLICATION(11, "SmsTypeEnum.ussdLoanApplication", "USSD Loan Application", "Mandatory"),
    TWO_FACTOR_OTP(12, "SmsTypeEnum.twofactorOtp", "Two Factor OTP", "Mandatory"),
    ACTIVATE_MOMO_PAYMENT_OTP(13, "SmsTypeEnum.momoPaymentOtp", "Activate Momo Payment OTP", "Mandatory"),
    DEACTIVATE_MOMO_PAYMENT(14, "SmsTypeEnum.deactivateMomoPayment", "DeActivate Momo Payment", "Mandatory"),
    CLIENT_PIN(15, "SmsTypeEnum.clientPin", "Create Client Pin", "Mandatory"),
    UNBLOCK_CLIENT_PIN(16, "SmsTypeEnum.unblockClientPin", "Unblock Client Pin", "Mandatory"),
    RESET_CLIENT_PIN(17, "SmsTypeEnum.resetClientPin", "Reset Client Pin", "Mandatory"),
    SELF_SERVICE_PIN_CHANGE(18, "SmsTypeEnum.resetClientPin", "Self Service Client Pin Change", "Mandatory"),
    FAILED_MAX_PIN_ATTEMPTS(19, "SmsTypeEnum.maxPinAttemps", "Max Pin Attempts Reached", "Mandatory"),
    CLIENT_CREATION(20, "SmsTypeEnum.createClient", "Member Created", "Mandatory"),
    LOAN_CREATION(21, "SmsTypeEnum.loanAccount", "Loan Account Created", "Mandatory"),
    SHARE_ACCOUNT_CREATION(22, "SmsTypeEnum.shareAccount", "Share Account Created", "Mandatory"),
    FIXED_DEPOSIT_CREATION(23, "SmsTypeEnum.fixedDeposit", "Fixed Deposit Activated", "Mandatory"),
    DEPOSIT_FAILURE_VIA_USSD(24, "SmsTypeEnum.UssdDepositFailure", "Deposit Failure via USSD", "Mandatory"),
    SAVINGS_TO_SAVINGS_ACCOUNT_TRANSFER(25, "SmsTypeEnum.AccountTransfer", "Savings to Savings Transfer Posted", "Mandatory"),
    SAVINGS_TO_LOAN_ACCOUNT_TRANSFER(26, "SmsTypeEnum.AccountTransfer", "Savings to Loan Transfer Posted", "Mandatory"),
    LOAN_TO_SAVINGS_ACCOUNT_TRANSFER(27, "SmsTypeEnum.AccountTransfer", "Loan to Savings Transfer Posted", "Mandatory"),
    LOAN_TO_LOAN_ACCOUNT_TRANSFER(28, "SmsTypeEnum.AccountTransfer", "Loan to Loan Transfer Posted", "Mandatory"),
    LOAN_CHARGE_WAIVED(29, "SmsTypeEnum.ChargeWaived", "Loan Charge Waived", "Optional"),
    LOAN_CHARGE_APPLIED(30, "SmsTypeEnum.ChargeApplied", "Loan Charge Applied", "Mandatory"),
    CLIENT_CHARGE_WAIVED(29, "SmsTypeEnum.ChargeWaived", "Client Charge Waived", "Optional"),
    CLIENT_CHARGE_APPLIED(30, "SmsTypeEnum.ChargeApplied", "Client Charge Applied", "Mandatory"),
    SAVINGS_CHARGE_WAIVED(29, "SmsTypeEnum.ChargeWaived", "Savings Charge Waived", "Optional"),
    SAVINGS_CHARGE_APPLIED(30, "SmsTypeEnum.ChargeApplied", "Savings Charge Applied", "Mandatory"),
    TRANSACTION_REVERSED(31, "SmsTypeEnum.transactionReversed", "Transaction Reversed", "Mandatory"),
    INTEREST_CREDITED(31, "SmsTypeEnum.interestCredited", "Interest Credited", "Optional"),
    LOAN_REPAYMENT_FAILURE_VIA_USSD(32, "SmsTypeEnum.UssdloanFailure", "Loan Repayment Failure via USSD", "Mandatory"),
    SAVINGS_INTEREST_POSTED(33, "SmsTypeEnum.interestPosted", "Interest Credited", "Optional"),
    SAVINGS_REVERSAL(34, "SmsTypeEnum.savingReversal", "Saving Transaction Reversed", "Mandatory"),
    LOAN_REVERSAL(35, "SmsTypeEnum.loanReversal", "Loan Transaction Reversed", "Mandatory"),
    ;

    private final Integer value;
    private final String code;
    private final String description;
    private final String status;

    SmsTypeEnum(final Integer value, final String code, final String description, final String status) {
        this.value = value;
        this.code = code;
        this.description = description;
        this.status = status;
    }

    public Integer getValue() {
        return value;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public String getStatus() {
        return status;
    }
}
