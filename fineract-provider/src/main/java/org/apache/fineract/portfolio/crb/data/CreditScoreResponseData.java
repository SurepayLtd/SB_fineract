package org.apache.fineract.portfolio.crb.data;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Data
@NoArgsConstructor
@Accessors(chain = true)
@AllArgsConstructor
public class CreditScoreResponseData implements Serializable {

    private static final long serialVersionUID = 1L;

    private String message;

    private Integer status;

    private Integer count;

    private CreditScoreData data;

    @Data
    public static class CreditScoreData implements Serializable {

        private static final long serialVersionUID = 1L;

        private EnquiryData Enquiry;

        private MnoScoreData MNO;

        private CrbScoreData CRB;
    }

    @Data
    public static class EnquiryData implements Serializable {

        private static final long serialVersionUID = 1L;

        private String ID, Reference, Entity, Identifier, Timestamp;

    }

    @Data
    public static class MnoScoreData implements Serializable {

        private static final long serialVersionUID = 1L;

        private MnoCustomerData Customer;

        private MnoScoringData Scoring;

        private MnoCreditAccountsData Credit_Accounts;

        private MnoIncomeProxyData Income_Proxy;

        private CostData Cost;

        private ErrorData Errors;
    }

    @Data
    public static class MnoCustomerData {

        private String Category, Name, Gender, Date_of_Birth;

        private Integer Age;

        private List<MnoPhoneNumberData> Phone_Numbers;
    }

    @Data
    public static class MnoPhoneNumberData {

        @JsonProperty("msisdn")
        private String msisdn;

        @JsonProperty("status")
        private String status;
    }

    @Data
    public static class MnoScoringData {

        @JsonProperty("Period")
        private String period;

        // The provider documents this value as a string, e.g. "411.0".
        @JsonProperty("Score")
        private String score;

        @JsonProperty("Band")
        private String band;

        @JsonProperty("Rating")
        private String rating;

        @JsonProperty("Probability_of_Default_Percent")
        private BigDecimal probabilityOfDefaultPercent;

        @JsonProperty("Likelihood_to_Default")
        private String likelihoodToDefault;

        @JsonProperty("Previous_Score")
        private BigDecimal previousScore;
    }

    @Data
    public static class MnoCreditAccountsData {

        @JsonProperty("Months_Active")
        private String monthsActive;

        @JsonProperty("Accounts_12Months")
        private String accounts12Months;

        @JsonProperty("Total_Loan_Amount_12Months")
        private String totalLoanAmount12Months;

        @JsonProperty("Closed_Accounts_12Months")
        private String closedAccounts12Months;

        @JsonProperty("Total_Payment_12Months")
        private String totalPayment12Months;
    }

    @Data
    public static class MnoIncomeProxyData {

        // Individual fields

        @JsonProperty("Total_Turnover_Count_6Months")
        private BigDecimal totalTurnoverCount6Months;

        @JsonProperty("Monthly_Turnover_Amount")
        private BigDecimal monthlyTurnoverAmount;

        @JsonProperty("Total_Spend_Count_6Months")
        private BigDecimal totalSpendCount6Months;

        @JsonProperty("Monthly_Spend_Amount")
        private BigDecimal monthlySpendAmount;

        @JsonProperty("Total_Turnover_Count_3Months")
        private BigDecimal totalTurnoverCount3Months;

        @JsonProperty("Monthly_Turnover_Amount_3Months")
        private BigDecimal monthlyTurnoverAmount3Months;

        // Field name preserved exactly as documented.
        @JsonProperty("Total_Spend_Count_6Months_3Months")
        private BigDecimal totalSpendCount6Months3Months;

        @JsonProperty("Monthly_Spend_Amount_3Months")
        private BigDecimal monthlySpendAmount3Months;

        // Agent fields

        @JsonProperty("Monthly_Float_Purchased")
        private BigDecimal monthlyFloatPurchased;

        @JsonProperty("Monthly_Commission_Earned")
        private BigDecimal monthlyCommissionEarned;

        // Merchant fields

        @JsonProperty("Total_Inflow_Count_6Months")
        private BigDecimal totalInflowCount6Months;

        @JsonProperty("Monthly_Inflow_Amount")
        private BigDecimal monthlyInflowAmount;
    }

    @Data
    public static class CrbScoreData {

        @JsonProperty("Customer")
        private CrbCustomerData customer;

        @JsonProperty("Scoring")
        private CrbScoringData scoring;

        @JsonProperty("Credit_Applications")
        private CreditApplicationsData creditApplications;

        @JsonProperty("Credit_Accounts")
        private CrbCreditAccountsData creditAccounts;

        @JsonProperty("Collateral_Material")
        private List<CollateralMaterialData> collateralMaterial;

        @JsonProperty("Collateral_Guarantor")
        private List<CollateralGuarantorData> collateralGuarantor;

        @JsonProperty("Bounced_Cheques")
        private List<BouncedChequesData> bouncedCheques;

        @JsonProperty("Financial_Fraud")
        private List<FinancialFraudData> financialFraud;

        @JsonProperty("Cost")
        private CostData cost;

        @JsonProperty("Errors")
        private ErrorData errors;
    }

    @Data
    public static class CrbCustomerData {

        @JsonProperty("Disputed")
        private String disputed;

        @JsonProperty("Name")
        private String name;

        @JsonProperty("Gender")
        private String gender;

        @JsonProperty("Date_of_Birth")
        private String dateOfBirth;

        @JsonProperty("Age")
        private Integer age;
    }

    @Data
    public static class CrbScoringData {

        @JsonProperty("Period")
        private String period;

        @JsonProperty("Score")
        private BigDecimal score;

        @JsonProperty("Band")
        private String band;

        @JsonProperty("Rating")
        private String rating;

        @JsonProperty("Probability_of_Default_Percent")
        private BigDecimal probabilityOfDefaultPercent;

        @JsonProperty("Likelihood_to_Default")
        private String likelihoodToDefault;

        // Keys are dynamic month-year values, e.g. "2023-11".
        @JsonProperty("History_Max_12Months")
        private Map<String, BigDecimal> historyMax12Months;
    }


    @Data
    public static class CreditApplicationsData {

        @JsonProperty("Total_Applications")
        private List<CreditApplicationSummaryData> totalApplications;

        @JsonProperty("Applications_12Months")
        private List<CreditApplicationSummaryData> applications12Months;

        @JsonProperty("Pending_Applications")
        private List<CreditApplicationSummaryData> pendingApplications;

        @JsonProperty("Approved_Applications")
        private List<CreditApplicationSummaryData> approvedApplications;

        @JsonProperty("Rejected_Applications")
        private List<CreditApplicationSummaryData> rejectedApplications;

        @JsonProperty("Cancelled_Applications")
        private List<CreditApplicationSummaryData> cancelledApplications;
    }

    @Data
    public static class CreditApplicationSummaryData {

        @JsonProperty("Currency")
        private String currency;

        @JsonProperty("Count")
        private Integer count;

        @JsonProperty("Value")
        private BigDecimal value;
    }


    @Data
    public static class CrbCreditAccountsData {

        @JsonProperty("Total_Accounts")
        private Integer totalAccounts;

        @JsonProperty("Total_Accounts_12Months")
        private Integer totalAccounts12Months;

        @JsonProperty("Open_Accounts")
        private Integer openAccounts;

        @JsonProperty("Open_Accounts_12Months")
        private Integer openAccounts12Months;

        @JsonProperty("Closed_Accounts")
        private Integer closedAccounts;

        @JsonProperty("Closed_Accounts_12Months")
        private Integer closedAccounts12Months;

        @JsonProperty("Adverse_Accounts")
        private Integer adverseAccounts;

        @JsonProperty("Adverse_Accounts_12Months")
        private Integer adverseAccounts12Months;

        @JsonProperty("Worst_Active_Days_in_arrears")
        private Integer worstActiveDaysInArrears;

        @JsonProperty("Worst_Days_in_Arreas")
        private Integer worstDaysInArrears;

        @JsonProperty("Worst_Days_in_arrears_12Months")
        private Integer worstDaysInArrears12Months;

        @JsonProperty("Total_Value")
        private List<CreditAccountValueData> totalValue;

        @JsonProperty("Value_12Months")
        private List<CreditAccountValue12MonthsData> value12Months;

        @JsonProperty("Monthly_Installment")
        private List<MonthlyInstallmentData> monthlyInstallment;
    }

    @Data
    public static class CreditAccountValueData {

        @JsonProperty("Currency")
        private String currency;

        @JsonProperty("Loan_Amount")
        private BigDecimal loanAmount;

        @JsonProperty("Loan_Balance")
        private BigDecimal loanBalance;

        @JsonProperty("Amount_Overdue")
        private BigDecimal amountOverdue;
    }

    @Data
    public static class CreditAccountValue12MonthsData {

        @JsonProperty("Currency")
        private String currency;

        @JsonProperty("Loan_Amount_12Months")
        private BigDecimal loanAmount12Months;

        @JsonProperty("Payment_12Months")
        private BigDecimal payment12Months;

        @JsonProperty("Loan_Balance_12Months")
        private BigDecimal loanBalance12Months;

        @JsonProperty("Amount_Overdue_12Months")
        private BigDecimal amountOverdue12Months;
    }

    @Data
    public static class MonthlyInstallmentData {

        @JsonProperty("Currency")
        private String currency;

        @JsonProperty("Amount")
        private BigDecimal amount;
    }


    @Data
    public static class CollateralMaterialData {

        @JsonProperty("Currency")
        private String currency;

        @JsonProperty("Items")
        private Integer items;

        @JsonProperty("Value")
        private BigDecimal value;
    }

    @Data
    public static class CollateralGuarantorData {

        @JsonProperty("Currency")
        private String currency;

        @JsonProperty("Accounts")
        private Integer accounts;

        @JsonProperty("Loan_Amount")
        private BigDecimal loanAmount;

        @JsonProperty("Loan_Balance")
        private BigDecimal loanBalance;
    }

    @Data
    public static class BouncedChequesData {

        @JsonProperty("Currency")
        private String currency;

        @JsonProperty("Cheques")
        private Integer cheques;

        @JsonProperty("Value")
        private BigDecimal value;
    }

    @Data
    public static class FinancialFraudData {

        @JsonProperty("Currency")
        private String currency;

        @JsonProperty("Cases")
        private Integer cases;

        @JsonProperty("Loss_Amount")
        private BigDecimal lossAmount;
    }


    @Data
    public static class CostData implements Serializable {

        private static final long serialVersionUID = 1L;

        @JsonProperty("Currency")
        private String currency;

        @JsonProperty("Amount")
        private Object amount;
    }

    @Data
    public static class ErrorData implements Serializable {

        private static final long serialVersionUID = 1L;

        @JsonProperty("Code")
        private String code;

        @JsonProperty("Message")
        private String message;
    }
}
