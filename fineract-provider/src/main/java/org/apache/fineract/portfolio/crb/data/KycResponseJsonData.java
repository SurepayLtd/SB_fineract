package org.apache.fineract.portfolio.crb.data;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.List;

@Data
@NoArgsConstructor
@Accessors(chain = true)
@AllArgsConstructor
public class KycResponseJsonData implements Serializable {

    private static final long serialVersionUID = 1L;

    private String message;
    private Integer status;
    private Integer count;

    private KycInformationData kyc_information;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class KycInformationData implements Serializable {

        private static final long serialVersionUID = 1L;

        private List<IdentificationDetailData> identification_details;

        private CustomerDetailsData customer_details;

        private BusinessDetailsData business_details;

        private List<PhysicalAddressData> physical_address_history;

        private List<PostalAddressData> postal_address_history;

        private ContactHistoryData contact_history;

        private List<ShareholderDetailData> shareholder_details;

        private List<EmploymentHistoryData> employment_history;
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
    public static class BusinessDetailsData implements Serializable {

        private static final long serialVersionUID = 1L;

        private String Business_Name, Trading_Name, Industry_Sector, Date_of_Registration, Postal_Address, Business_Type, Contact, Address;
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

        private List<CreditReportJsonResponseData.PhoneNumberData> Phone_Numbers;

        private List<CreditReportJsonResponseData.EmailData> Emails;
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
}
