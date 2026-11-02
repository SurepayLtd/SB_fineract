package org.apache.fineract.portfolio.crb.data;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@Accessors(chain = true)
@AllArgsConstructor
public class CreditReportJsonResponseData implements Serializable {

    private static final long serialVersionUID = 1L;

    private String message;
    private Integer status;
    private Integer count;

    private CreditInformationData credit_information;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreditInformationData implements Serializable {

        private static final long serialVersionUID = 1L;

        private List<IdentificationDetailData> identification_details;

        private CustomerDetailsData customer_details;

        private List<PhysicalAddressData> physical_address_history;

        private List<PostalAddressData> postal_address_history;

        private ContactHistoryData contact_history;

        private List<ShareholderDetailData> shareholder_details;

        private List<EmploymentHistoryData> employment_history;

        private ScoringDetailsData scoring_details;

        private List<Map<String, Object>> credit_applications_summary;

        private List<Map<String, Object>> credit_accounts_summary;

        private List<CreditAccountsClassificationData> credits_accounts_classification_summary;

        private List<Map<String, Object>> collateral_material_summary;

        private List<Map<String, Object>> collateral_guarantor_summary;

        private List<Map<String, Object>> bounced_cheques_summary;

        private List<Map<String, Object>> financial_fraud_summary;

        private List<CreditApplicationDetailData> credit_applications_details;

        private List<CreditAccountDetailData> credit_account_details;

        private List<CollateralMaterialDetailData> collateral_material_details;

        private List<CollateralGuarantorDetailData> collateral_guarantor_details;

        private List<BouncedChequeDetailData> bounced_cheques_details;

        private List<FinancialFraudDetailData> financial_fraud_details;

        private List<EnquiryDetailData> enquiries_details;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IdentificationDetailData implements Serializable {

        private static final long serialVersionUID = 1L;

        private Integer SNo;

        private String identifier, Identifier_Type, Country_of_Issue;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CustomerDetailsData implements Serializable {

        private static final long serialVersionUID = 1L;

        private String Photo, Name, Date_of_Birth, Marital_Status, Gender, Nationality, Contact, Address;

    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PhysicalAddressData implements Serializable {

        private static final long serialVersionUID = 1L;

        private Integer SNo;

        private String Address, Ownership, Period_at_Address;

    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PostalAddressData implements Serializable {

        private static final long serialVersionUID = 1L;

        private Integer SNo;

        private String address;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ContactHistoryData implements Serializable {

        private static final long serialVersionUID = 1L;

        private List<PhoneNumberData> Phone_Numbers;

        private List<EmailData> Emails;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PhoneNumberData implements Serializable {

        private static final long serialVersionUID = 1L;

        private Integer SNo;

        private String Phone_Number;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EmailData implements Serializable {

        private static final long serialVersionUID = 1L;

        private Integer SNo;

        private String Email;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ShareholderDetailData implements Serializable {

        private static final long serialVersionUID = 1L;

        private Integer SNo;

        private String Business_Name, stakeholderType, Stakeholder_Category, Shareholder_Percentage;

    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EmploymentHistoryData implements Serializable {

        private static final long serialVersionUID = 1L;

        private Integer SNo;

        private String Employer_Name, Occupation, Employment_Date, Income_Band;

    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScoringDetailsData implements Serializable {

        private static final long serialVersionUID = 1L;

        private String Evaluation_Period, Current_Score_Band, Current_Score_Rating, Probability_of_Default, Likelihood_to_Default;

        private Integer Current_Score;

        private List<String> Score_Explanation;

        private List<ScoreHistoryData> Score_History;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScoreHistoryData implements Serializable {

        private static final long serialVersionUID = 1L;

        private String MonthYear, Score;

    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreditAccountsClassificationData implements Serializable {

        private static final long serialVersionUID = 1L;

        private String Institution_Type, Currency;

        private Integer No_of_Accounts;

        private BigDecimal Loan_Amount, Loan_Balance;

    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreditApplicationDetailData implements Serializable {

        private static final long serialVersionUID = 1L;

        private Integer SNo;

        private String Institution, Reference, Product_Type, Application_Date, Currency, Amount, Application_Status, Last_Status_Date;

    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreditAccountDetailData implements Serializable {

        private static final long serialVersionUID = 1L;

        private Integer SNo, Number_of_Payments;

        private String Institution, Term, Loan_Purpose, Client_Number, Borrower_Classification, First_Payment_Date, Credit_Account_Reference, Amortization_Type, Type_of_Interest;

        private BigDecimal Annual_Interest_Rate_at_Disbursement, Credit_Amount, Facility_Amount, Monthly_Installment_Amount, Provision_Amount;

        private String Credit_Account_Date, Interest_Calculation_Method, Payment_Frequency, Currency, Credit_Account_Type, Group_ID, Client_Consented, Maturity_Date, Client_Given_Notice;

        private List<LastTransactionData> Last_Transaction;

        private List<CreditCycleData> Credit_Cycle_12_Months;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LastTransactionData implements Serializable {

        private static final long serialVersionUID = 1L;

        private String Transaction_Date, Currency, Last_Payment_Date, Closure_Reason, Closure_Date, Last_Status_Change_Date, Arrears_Date, Restructured_Credit, Account_Status, Risk_Classification;

        private BigDecimal Last_Payment_Amount, Current_Balance_Amount, Balance_Overdue, Annual_Interest_Rate_at_Reporting;

        private Integer Days_in_Arrears;

    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreditCycleData implements Serializable {

        private static final long serialVersionUID = 1L;

        private String Month, Year, Days_in_Arrears, Risk_Classification, Account_Status;

    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CollateralMaterialDetailData implements Serializable {

        private static final long serialVersionUID = 1L;

        private Integer SNo;

        private String Held_By, Client_Number, Valuation_Date, Account_Number, Expiry_Date, Classification, Currency;

        private BigDecimal Open_Market_Value, Forced_Sale_Value;

        private String Maturity_Date, Reference, Instrument_of_Claim, Description;

    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CollateralGuarantorDetailData implements Serializable {

        private static final long serialVersionUID = 1L;

        private Integer SNo;

        private String Institution, Currency, Reference, Maturity_Date, Credit_Account_Date, Credit_Account_Status;

        private BigDecimal Amount_Guaranteed, Current_Balance_Amount;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BouncedChequeDetailData implements Serializable {

        private static final long serialVersionUID = 1L;

        private Integer SNo, Cheque_Number, Amount;

        private String Institution, Reference, Bounced_Date, Beneficiary, Currency, Reason;

    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FinancialFraudDetailData implements Serializable {

        private static final long serialVersionUID = 1L;

        private Integer SNo;

        private String Institution, Category, Subcategory, Currency, Incident_Date;

        private BigDecimal Loss_Amount;

    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EnquiryDetailData implements Serializable {

        private static final long serialVersionUID = 1L;

        private Integer SNo, Count;

        private String Institution, Reason, Timestamp;

    }
}

