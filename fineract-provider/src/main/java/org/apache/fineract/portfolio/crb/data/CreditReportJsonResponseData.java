package org.apache.fineract.portfolio.crb.data;


import com.fasterxml.jackson.annotation.JsonProperty;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
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

    @JsonProperty("message")
    private String message;
    @JsonProperty("status")
    private Integer status;
    @JsonProperty("count")
    private Integer count;

    @JsonProperty("credit_information")
    private CreditInformationData creditInformation;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreditInformationData implements Serializable {

        private static final long serialVersionUID = 1L;

        @JsonProperty("identification_details")
        private List<IdentificationDetailData> identificationDetails;

        @JsonProperty("customer_details")
        private CustomerDetailsData customerDetails;

        @JsonProperty("physical_address_history")
        private List<PhysicalAddressData> physicalAddressHistory;

        @JsonProperty("postal_address_history")
        private List<PostalAddressData> postalAddressHistory;

        @JsonProperty("contact_history")
        private ContactHistoryData contactHistory;

        @JsonProperty("shareholder_details")
        private List<ShareholderDetailData> shareholderDetails;

        @JsonProperty("employment_history")
        private List<EmploymentHistoryData> employmentHistory;

        @JsonProperty("scoring_details")
        private ScoringDetailsData scoringDetails;

        @JsonProperty("credit_applications_summary")
        private List<Map<String, Object>> creditApplicationsSummary;

        @JsonProperty("credit_accounts_summary")
        private List<Map<String, Object>> creditAccountsSummary;

        @JsonProperty("credits_accounts_classification_summary")
        private List<CreditAccountsClassificationData> creditsAccountsClassificationSummary;

        @JsonProperty("collateral_material_summary")
        private List<Map<String, Object>> collateralMaterialSummary;

        @JsonProperty("collateral_guarantor_summary")
        private List<Map<String, Object>> collateralGuarantorSummary;

        @JsonProperty("bounced_cheques_summary")
        private List<Map<String, Object>> bouncedChequesSummary;

        @JsonProperty("financial_fraud_summary")
        private List<Map<String, Object>> financialFraudSummary;

        @JsonProperty("credit_applications_details")
        private List<CreditApplicationDetailData> creditApplicationsDetails;

        @JsonProperty("credit_account_details")
        private List<CreditAccountDetailData> creditAccountDetails;

        @JsonProperty("collateral_material_details")
        private List<CollateralMaterialDetailData> collateralMaterialDetails;

        @JsonProperty("collateral_guarantor_details")
        private List<CollateralGuarantorDetailData> collateralGuarantorDetails;

        @JsonProperty("bounced_cheques_details")
        private List<BouncedChequeDetailData> bouncedChequesDetails;

        @JsonProperty("financial_fraud_details")
        private List<FinancialFraudDetailData> financialFraudDetails;

        @JsonProperty("enquiries_details")
        private List<EnquiryDetailData> enquiriesDetails;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IdentificationDetailData implements Serializable {

        private static final long serialVersionUID = 1L;

        @JsonProperty("SNo")
        private Integer sNo;
        @JsonProperty("Identifier")
        private String identifier;
        @JsonProperty("Identifier_Type")
        private String identifierType;
        @JsonProperty("Country_of_Issue")
        private String countryOfIssue;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CustomerDetailsData implements Serializable {

        private static final long serialVersionUID = 1L;

        @JsonProperty("Photo")
        private String photo;
        @JsonProperty("Name")
        private String name;
        @JsonProperty("Date_of_Birth")
        private String dateOfBirth;
        @JsonProperty("Marital_Status")
        private String maritalStatus;
        @JsonProperty("Gender")
        private String gender;
        @JsonProperty("Nationality")
        private String nationality;
        @JsonProperty("Contact")
        private String contact;
        @JsonProperty("Address")
        private String address;

    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PhysicalAddressData implements Serializable {

        private static final long serialVersionUID = 1L;

        @JsonProperty("SNo")
        private Integer sNo;

        @JsonProperty("Address")
        private String address;
        @JsonProperty("Ownership")
        private String ownership;
        @JsonProperty("Period_at_Address")
        private String periodAtAddress;

    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PostalAddressData implements Serializable {

        private static final long serialVersionUID = 1L;

        @JsonProperty("SNo")
        private Integer sNo;

        @JsonProperty("address")
        private String address;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ContactHistoryData implements Serializable {

        private static final long serialVersionUID = 1L;

        @JsonProperty("Phone_Numbers")
        private List<PhoneNumberData> phoneNumbers;

        @JsonProperty("Emails")
        private List<EmailData> emails;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PhoneNumberData implements Serializable {

        private static final long serialVersionUID = 1L;

        @JsonProperty("SNo")
        private Integer sNo;

        @JsonProperty("Phone_Number")
        private String phoneNumber;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EmailData implements Serializable {

        private static final long serialVersionUID = 1L;

        @JsonProperty("SNo")
        private Integer sNo;

        @JsonProperty("Email")
        private String email;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ShareholderDetailData implements Serializable {

        private static final long serialVersionUID = 1L;

        @JsonProperty("SNo")
        private Integer sNo;

        @JsonProperty("Business_Name")
        private String businessName;
        @JsonProperty("stakeholderType")
        private String stakeholderType;
        @JsonProperty("Stakeholder_Category")
        private String stakeholderCategory;
        @JsonProperty("Shareholder_Percentage")
        private String shareholderPercentage;

    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EmploymentHistoryData implements Serializable {

        private static final long serialVersionUID = 1L;

        @JsonProperty("SNo")
        private Integer sNo;

        @JsonProperty("Employer_Name")
        private String employerName;
        @JsonProperty("Occupation")
        private String occupation;
        @JsonProperty("Employment_Date")
        private String employmentDate;
        @JsonProperty("Income_Band")
        private String incomeBand;

    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScoringDetailsData implements Serializable {

        private static final long serialVersionUID = 1L;

        @JsonProperty("Evaluation_Period")
        private String evaluationPeriod;
        @JsonProperty("Current_Score_Band")
        private String currentScoreBand;
        @JsonProperty("Current_Score_Rating")
        private String currentScoreRating;
        @JsonProperty("Probability_of_Default")
        private String probabilityOfDefault;
        @JsonProperty("Likelihood_to_Default")
        private String likelihoodToDefault;

        @JsonProperty("Current_Score")
        private Integer currentScore;

        @JsonProperty("Score_Explanation")
        @JsonDeserialize(using = StringOrStringListDeserializer.class)
        private List<String> scoreExplanation;

        @JsonProperty("Score_History")
        private List<ScoreHistoryData> scoreHistory;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScoreHistoryData implements Serializable {

        private static final long serialVersionUID = 1L;

        @JsonProperty("MonthYear")
        private String monthYear;
        @JsonProperty("Score")
        private String score;

    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreditAccountsClassificationData implements Serializable {

        private static final long serialVersionUID = 1L;

        @JsonProperty("Institution_Type")
        private String institutionType;
        @JsonProperty("Currency")
        private String currency;

        @JsonProperty("No_of_Accounts")
        private Integer noOfAccounts;

        @JsonProperty("Loan_Amount")
        private BigDecimal loanAmount;
        @JsonProperty("Loan_Balance")
        private BigDecimal loanBalance;

    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreditApplicationDetailData implements Serializable {

        private static final long serialVersionUID = 1L;

        @JsonProperty("SNo")
        private Integer sNo;

        @JsonProperty("Institution")
        private String institution;
        @JsonProperty("Reference")
        private String reference;
        @JsonProperty("Product_Type")
        private String productType;
        @JsonProperty("Application_Date")
        private String applicationDate;
        @JsonProperty("Currency")
        private String currency;
        @JsonProperty("Amount")
        private String amount;
        @JsonProperty("Application_Status")
        private String applicationStatus;
        @JsonProperty("Last_Status_Date")
        private String lastStatusDate;

    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreditAccountDetailData implements Serializable {

        private static final long serialVersionUID = 1L;

        @JsonProperty("SNo")
        private Integer sNo;
        @JsonProperty("Number_of_Payments")
        private Integer numberOfPayments;

        @JsonProperty("Institution")
        private String institution;
        @JsonProperty("Term")
        private String term;
        @JsonProperty("Loan_Purpose")
        private String loanPurpose;
        @JsonProperty("Client_Number")
        private String clientNumber;
        @JsonProperty("Borrower_Classification")
        private String borrowerClassification;
        @JsonProperty("First_Payment_Date")
        private String firstPaymentDate;
        @JsonProperty("Credit_Account_Reference")
        private String creditAccountReference;
        @JsonProperty("Amortization_Type")
        private String amortizationType;
        @JsonProperty("Type_of_Interest")
        private String typeOfInterest;

        @JsonProperty("Annual_Interest_Rate_at_Disbursement")
        private BigDecimal annualInterestRateAtDisbursement;
        @JsonProperty("Credit_Amount")
        private BigDecimal creditAmount;
        @JsonProperty("Facility_Amount")
        private BigDecimal facilityAmount;
        @JsonProperty("Monthly_Installment_Amount")
        private BigDecimal monthlyInstallmentAmount;
        @JsonProperty("Provision_Amount")
        private BigDecimal provisionAmount;

        @JsonProperty("Credit_Account_Date")
        private String creditAccountDate;
        @JsonProperty("Interest_Calculation_Method")
        private String interestCalculationMethod;
        @JsonProperty("Payment_Frequency")
        private String paymentFrequency;
        @JsonProperty("Currency")
        private String currency;
        @JsonProperty("Credit_Account_Type")
        private String creditAccountType;
        @JsonProperty("Group_ID")
        private String groupID;
        @JsonProperty("Client_Consented")
        private String clientConsented;
        @JsonProperty("Maturity_Date")
        private String maturityDate;
        @JsonProperty("Client_Given_Notice")
        private String clientGivenNotice;

        @JsonProperty("Last_Transaction")
        private List<LastTransactionData> lastTransaction;

        @JsonProperty("Credit_Cycle_12_Months")
        private List<CreditCycleData> creditCycle12Months;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LastTransactionData implements Serializable {

        private static final long serialVersionUID = 1L;

        @JsonProperty("Transaction_Date")
        private String transactionDate;
        @JsonProperty("Currency")
        private String currency;
        @JsonProperty("Last_Payment_Date")
        private String lastPaymentDate;
        @JsonProperty("Closure_Reason")
        private String closureReason;
        @JsonProperty("Closure_Date")
        private String closureDate;
        @JsonProperty("Last_Status_Change_Date")
        private String lastStatusChangeDate;
        @JsonProperty("Arrears_Date")
        private String arrearsDate;
        @JsonProperty("Restructured_Credit")
        private String restructuredCredit;
        @JsonProperty("Account_Status")
        private String accountStatus;
        @JsonProperty("Risk_Classification")
        private String riskClassification;

        @JsonProperty("Last_Payment_Amount")
        private BigDecimal lastPaymentAmount;
        @JsonProperty("Current_Balance_Amount")
        private BigDecimal currentBalanceAmount;
        @JsonProperty("Balance_Overdue")
        private BigDecimal balanceOverdue;
        @JsonProperty("Annual_Interest_Rate_at_Reporting")
        private BigDecimal annualInterestRateAtReporting;

        @JsonProperty("Days_in_Arrears")
        private Integer daysInArrears;

    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreditCycleData implements Serializable {

        private static final long serialVersionUID = 1L;

        @JsonProperty("Month")
        private String month;
        @JsonProperty("Year")
        private String year;
        @JsonProperty("Days_in_Arrears")
        private String daysInArrears;
        @JsonProperty("Risk_Classification")
        private String riskClassification;
        @JsonProperty("Account_Status")
        private String accountStatus;

    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CollateralMaterialDetailData implements Serializable {

        private static final long serialVersionUID = 1L;

        @JsonProperty("SNo")
        private Integer sNo;

        @JsonProperty("Held_By")
        private String heldBy;
        @JsonProperty("Client_Number")
        private String clientNumber;
        @JsonProperty("Valuation_Date")
        private String valuationDate;
        @JsonProperty("Account_Number")
        private String accountNumber;
        @JsonProperty("Expiry_Date")
        private String expiryDate;
        @JsonProperty("Classification")
        private String classification;
        @JsonProperty("Currency")
        private String currency;

        @JsonProperty("Open_Market_Value")
        private BigDecimal openMarketValue;
        @JsonProperty("Forced_Sale_Value")
        private BigDecimal forcedSaleValue;

        @JsonProperty("Maturity_Date")
        private String maturityDate;
        @JsonProperty("Reference")
        private String reference;
        @JsonProperty("Instrument_of_Claim")
        private String instrumentOfClaim;
        @JsonProperty("Description")
        private String description;

    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CollateralGuarantorDetailData implements Serializable {

        private static final long serialVersionUID = 1L;

        @JsonProperty("SNo")
        private Integer sNo;

        @JsonProperty("Institution")
        private String institution;
        @JsonProperty("Currency")
        private String currency;
        @JsonProperty("Reference")
        private String reference;
        @JsonProperty("Maturity_Date")
        private String maturityDate;
        @JsonProperty("Credit_Account_Date")
        private String creditAccountDate;
        @JsonProperty("Credit_Account_Status")
        private String creditAccountStatus;

        @JsonProperty("Amount_Guaranteed")
        private BigDecimal amountGuaranteed;
        @JsonProperty("Current_Balance_Amount")
        private BigDecimal currentBalanceAmount;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BouncedChequeDetailData implements Serializable {

        private static final long serialVersionUID = 1L;

        @JsonProperty("SNo")
        private Integer sNo;
        @JsonProperty("Cheque_Number")
        private Integer chequeNumber;
        @JsonProperty("Amount")
        private Integer amount;

        @JsonProperty("Institution")
        private String institution;
        @JsonProperty("Reference")
        private String reference;
        @JsonProperty("Bounced_Date")
        private String bouncedDate;
        @JsonProperty("Beneficiary")
        private String beneficiary;
        @JsonProperty("Currency")
        private String currency;
        @JsonProperty("Reason")
        private String reason;

    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FinancialFraudDetailData implements Serializable {

        private static final long serialVersionUID = 1L;

        @JsonProperty("SNo")
        private Integer sNo;

        @JsonProperty("Institution")
        private String institution;
        @JsonProperty("Category")
        private String category;
        @JsonProperty("Subcategory")
        private String subcategory;
        @JsonProperty("Currency")
        private String currency;
        @JsonProperty("Incident_Date")
        private String incidentDate;

        @JsonProperty("Loss_Amount")
        private BigDecimal lossAmount;

    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EnquiryDetailData implements Serializable {

        private static final long serialVersionUID = 1L;

        @JsonProperty("SNo")
        private Integer sNo;
        @JsonProperty("Count")
        private Integer count;

        @JsonProperty("Institution")
        private String institution;
        @JsonProperty("Reason")
        private String reason;
        @JsonProperty("Timestamp")
        private String timestamp;

    }
}
