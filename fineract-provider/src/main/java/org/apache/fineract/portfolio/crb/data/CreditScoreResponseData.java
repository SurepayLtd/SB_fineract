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
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreditScoreData implements Serializable {

        private static final long serialVersionUID = 1L;

        @JsonProperty("Enquiry")
        private EnquiryData enquiry;

        @JsonProperty("MNO")
        private MnoScoreData mno;

        @JsonProperty("CRB")
        private CrbScoreData crb;

        @JsonProperty("Cost")
        private Cost cost;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EnquiryData implements Serializable {

        private static final long serialVersionUID = 1L;

        @JsonProperty("ID")
        private String id;

        @JsonProperty("Reference")
        private String reference;

        @JsonProperty("Entity")
        private String entity;

        @JsonProperty("Identifier")
        private String identifier;

        @JsonProperty("Timestamp")
        private String timestamp;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MnoScoreData implements Serializable {

        private static final long serialVersionUID = 1L;

        @JsonProperty("Customer")
        private MnoCustomerData customer;

        @JsonProperty("Scoring")
        private MnoScoringData scoring;

        @JsonProperty("Credit_Accounts")
        private MnoCreditAccountsData creditAccounts;

        @JsonProperty("Income_Proxy")
        private MnoIncomeProxyData incomeProxy;

        @JsonProperty("Cost")
        private CostData cost;

        @JsonProperty("Errors")
        private ErrorData errors;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MnoCustomerData implements Serializable {

        private static final long serialVersionUID = 1L;

        @JsonProperty("Category")
        private String category;
        @JsonProperty("Name")
        private String name;
        @JsonProperty("Gender")
        private String gender;
        @JsonProperty("Date_of_Birth")
        private String dateOfBirth;
        @JsonProperty("Age")
        private Integer age;
        @JsonProperty("Months_Active")
        private Integer monthsActive;
        @JsonProperty("Phone_Count")
        private Integer phoneCount;

        @JsonProperty("Phone_Numbers")
        private List<MnoPhoneNumberData> phoneNumbers;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MnoPhoneNumberData implements Serializable {

        private static final long serialVersionUID = 1L;

        private String msisdn;

        private String status;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MnoScoringData implements Serializable {

        private static final long serialVersionUID = 1L;

        @JsonProperty("Period")
        private String period;

        @JsonProperty("Band")
        private String band;

        @JsonProperty("Rating")
        private String rating;

        @JsonProperty("Likelihood_to_Default")
        private String likelihoodToDefault;

        @JsonProperty("Probability_of_Default_Percent")
        private BigDecimal probabilityOfDefaultPercent;

        @JsonProperty("Previous_Score")
        private BigDecimal previousScore;

        @JsonProperty("Score")
        private Integer score;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MnoCreditAccountsData implements Serializable {

        private static final long serialVersionUID = 1L;

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

        @JsonProperty("Accounts_6Months")
        private String accounts6Months;

        @JsonProperty("Total_Loan_Amount_6Months")
        private String totalLoanAmount6Months;

        @JsonProperty("Closed_Accounts_6Months")
        private String closedAccounts6Months;

        @JsonProperty("Total_Payment_6Months")
        private String totalPayment6Months;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MnoIncomeProxyData implements Serializable {

        private static final long serialVersionUID = 1L;

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

        @JsonProperty("Total_Spend_Count_3Months")
        private BigDecimal totalSpendCount3Months;

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
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CrbScoreData implements Serializable {

        private static final long serialVersionUID = 1L;

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
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CrbCustomerData implements Serializable {

        private static final long serialVersionUID = 1L;

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
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CrbScoringData implements Serializable {

        private static final long serialVersionUID = 1L;

        @JsonProperty("Period")
        private String period;

        @JsonProperty("Band")
        private String band;

        @JsonProperty("Rating")
        private String rating;

        @JsonProperty("Likelihood_to_Default")
        private String likelihoodToDefault;

        @JsonProperty("Score")
        private BigDecimal score;

        @JsonProperty("Probability_of_Default_Percent")
        private BigDecimal probabilityOfDefaultPercent;

        @JsonProperty("History_Max_12Months")
        private Map<String, BigDecimal> historyMax12Months;
    }


    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreditApplicationsData implements Serializable {

        private static final long serialVersionUID = 1L;

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
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreditApplicationSummaryData implements Serializable {

        private static final long serialVersionUID = 1L;

        @JsonProperty("Currency")
        private String currency;

        @JsonProperty("Count")
        private Integer count;

        @JsonProperty("Value")
        private BigDecimal value;
    }


    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CrbCreditAccountsData implements Serializable {

        private static final long serialVersionUID = 1L;

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

        @JsonProperty("Worst_Days_in_Arrears")
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
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreditAccountValueData implements Serializable {

        private static final long serialVersionUID = 1L;

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
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreditAccountValue12MonthsData implements Serializable {

        private static final long serialVersionUID = 1L;

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
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MonthlyInstallmentData implements Serializable {

        private static final long serialVersionUID = 1L;

        @JsonProperty("Currency")
        private String currency;

        @JsonProperty("Count")
        private Integer count;

        @JsonProperty("Amount")
        private BigDecimal amount;
    }


    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CollateralMaterialData implements Serializable {

        private static final long serialVersionUID = 1L;

        @JsonProperty("Currency")
        private String currency;

        @JsonProperty("Items")
        private Integer items;

        @JsonProperty("Value")
        private BigDecimal value;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CollateralGuarantorData implements Serializable {

        private static final long serialVersionUID = 1L;

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
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BouncedChequesData implements Serializable {

        private static final long serialVersionUID = 1L;

        @JsonProperty("Currency")
        private String currency;

        @JsonProperty("Cheques")
        private Integer cheques;

        @JsonProperty("Value")
        private BigDecimal value;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FinancialFraudData implements Serializable {

        private static final long serialVersionUID = 1L;

        @JsonProperty("Currency")
        private String currency;

        @JsonProperty("Cases")
        private Integer cases;

        @JsonProperty("Loss_Amount")
        private BigDecimal lossAmount;
    }


    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CostData implements Serializable {

        private static final long serialVersionUID = 1L;

        @JsonProperty("Currency")
        private String currency;

        @JsonProperty("Amount")
        private BigDecimal amount;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ErrorData implements Serializable {

        private static final long serialVersionUID = 1L;

        @JsonProperty("Code")
        private String code;

        @JsonProperty("Message")
        private String message;
    }


    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Cost implements Serializable {

        private static final long serialVersionUID = 1L;

        @JsonProperty("Currency")
        private String currency;

        @JsonProperty("Amount")
        private BigDecimal amount;

        @JsonProperty("Charge")
        private BigDecimal charge;

        @JsonProperty("VAT")
        private BigDecimal vat;
    }
}
