package org.apache.fineract.portfolio.crb.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class KycReportResponseJsonData implements Serializable {

    private static final long serialVersionUID = 1L;

    @JsonProperty("message")
    private String message;

    @JsonProperty("status")
    private Integer status;

    @JsonProperty("count")
    private Integer count;

    @JsonProperty("kyc_information")
    private KycInformationData kycInformation;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class KycInformationData implements Serializable {

        private static final long serialVersionUID = 1L;

        @JsonProperty("identification_details")
        private List<IdentificationDetailData> identificationDetails;

        @JsonProperty("customer_details")
        private CustomerDetailsData customerDetails;

        @JsonProperty("business_details")
        private BusinessDetailsData businessDetails;

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
    public static class BusinessDetailsData implements Serializable {

        private static final long serialVersionUID = 1L;

        @JsonProperty("Business_Name")
        private String businessName;

        @JsonProperty("Trading_Name")
        private String tradingName;

        @JsonProperty("Industry_Sector")
        private String industrySector;

        @JsonProperty("Date_of_Registration")
        private String dateOfRegistration;

        @JsonProperty("Postal_Address")
        private String postalAddress;

        @JsonProperty("Business_Type")
        private String businessType;

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
}