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

        private EnquiryData Enquiry;

        private MnoScoreData MNO;

        private CrbScoreData CRB;

        private Cost Cost;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EnquiryData implements Serializable {

        private static final long serialVersionUID = 1L;

        private String ID, Reference, Entity, Identifier, Timestamp;

    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
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
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MnoCustomerData {

        private String Category, Name, Gender, Date_of_Birth;

        private Integer Age, Months_Active, Phone_Count;

        private List<MnoPhoneNumberData> Phone_Numbers;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MnoPhoneNumberData {

        private String msisdn;

        private String status;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MnoScoringData {

        private String Period, Band, Rating, Likelihood_to_Default;
        private BigDecimal Probability_of_Default_Percent, Previous_Score;
        private Integer Score;

    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MnoCreditAccountsData {

        private String Months_Active, Accounts_12Months,Total_Loan_Amount_12Months, Closed_Accounts_12Months, Total_Payment_12Months, Accounts_6Months, Total_Loan_Amount_6Months, Closed_Accounts_6Months, Total_Payment_6Months;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MnoIncomeProxyData {

        // Individual fields

        private BigDecimal Total_Turnover_Count_6Months, Monthly_Turnover_Amount, Total_Spend_Count_6Months, Monthly_Spend_Amount,
                Total_Turnover_Count_3Months, Monthly_Turnover_Amount_3Months, Total_Spend_Count_3Months, Monthly_Spend_Amount_3Months;


        // Agent fields

        private BigDecimal Monthly_Float_Purchased, Monthly_Commission_Earned;


        // Merchant fields

        private BigDecimal Total_Inflow_Count_6Months, Monthly_Inflow_Amount;

    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CrbScoreData {

        private CrbCustomerData Customer;

        private CrbScoringData Scoring;

        private CreditApplicationsData Credit_Applications;

        private CrbCreditAccountsData Credit_Accounts;

        private List<CollateralMaterialData> Collateral_Material;

        private List<CollateralGuarantorData> Collateral_Guarantor;

        private List<BouncedChequesData> Bounced_Cheques;

        private List<FinancialFraudData> Financial_Fraud;

        private CostData Cost;

        private ErrorData Errors;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CrbCustomerData {

        private String Disputed, Name, Gender, Date_of_Birth;
        private Integer Age;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CrbScoringData {

        private String Period, Band, Rating, Likelihood_to_Default;

        private BigDecimal Score, Probability_of_Default_Percent;

        private Map<String, BigDecimal> History_Max_12Months;
    }


    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreditApplicationsData {

        private List<CreditApplicationSummaryData> Total_Applications;

        private List<CreditApplicationSummaryData> Applications_12Months;

        private List<CreditApplicationSummaryData> Pending_Applications;

        private List<CreditApplicationSummaryData> Approved_Applications;

        private List<CreditApplicationSummaryData> Rejected_Applications;

        private List<CreditApplicationSummaryData> Cancelled_Applications;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreditApplicationSummaryData {

        private String Currency;

        private Integer Count;

        private BigDecimal Value;
    }


    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CrbCreditAccountsData {

        private Integer Total_Accounts, Total_Accounts_12Months, Open_Accounts, Open_Accounts_12Months, Closed_Accounts, Closed_Accounts_12Months, Adverse_Accounts,
                Adverse_Accounts_12Months, Worst_Active_Days_in_arrears, Worst_Days_in_Arrears, Worst_Days_in_arrears_12Months;


        private List<CreditAccountValueData> Total_Value;

        private List<CreditAccountValue12MonthsData> Value_12Months;

        private List<MonthlyInstallmentData> Monthly_Installment;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreditAccountValueData {

        private String Currency;

        private BigDecimal Loan_Amount, Loan_Balance, Amount_Overdue;

    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreditAccountValue12MonthsData {

        private String Currency;

        private BigDecimal Loan_Amount_12Months, Payment_12Months, Loan_Balance_12Months, Amount_Overdue_12Months;

    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MonthlyInstallmentData {

        private String Currency;

        private Integer Count;

        private BigDecimal Amount;
    }


    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CollateralMaterialData {

        private String Currency;

        private Integer Items;

        private BigDecimal Value;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CollateralGuarantorData {

        private String Currency;

        private Integer Accounts;

        private BigDecimal Loan_Amount, Loan_Balance;

    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BouncedChequesData {

        private String Currency;

        private Integer Cheques;

        private BigDecimal Value;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FinancialFraudData {

        private String Currency;

        private Integer Cases;

        private BigDecimal Loss_Amount;
    }


    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CostData implements Serializable {

        private static final long serialVersionUID = 1L;

        private String Currency;

        private BigDecimal Amount;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ErrorData implements Serializable {

        private static final long serialVersionUID = 1L;

        private String Code;

        private String Message;
    }


    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Cost implements Serializable {

        private static final long serialVersionUID = 1L;

        private String Currency;

        private BigDecimal Amount, Charge, VAT;
    }
}
