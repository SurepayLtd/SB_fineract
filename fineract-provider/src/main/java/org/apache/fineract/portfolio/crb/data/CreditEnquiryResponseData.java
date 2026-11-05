package org.apache.fineract.portfolio.crb.data;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@Accessors(chain = true)
@AllArgsConstructor
public class CreditEnquiryResponseData implements Serializable {

    private static final long serialVersionUID = 1L;

    private String message;
    private Integer status;
    private Integer count;
    private ValidationData enquiry;

    @Data
    @NoArgsConstructor
    @Accessors(chain = true)
    @AllArgsConstructor
    public static class ValidationData implements Serializable {

        private static final long serialVersionUID = 1L;

        private String id;
        private String entity;
        private LocalDateTime timestamp;
        private String reference;
        private String criteria;
        private String search_value;
        private String identification_type;
        private String status;
        private BigDecimal cost;
        private BigDecimal vat;
        private BigDecimal charge;

        private SectorData formal;
        private SectorData informal;
    }

    @Data
    @NoArgsConstructor
    @Accessors(chain = true)
    @AllArgsConstructor
    public static class SectorData implements Serializable {

        private static final long serialVersionUID = 1L;

        private String id;
        private String entity;
        private LocalDateTime timestamp;
        private String reference;
        private String criteria;
        private String search_value;
        private String identification_type;
        private BigDecimal charge;
        private BigDecimal vat;
        private String sector;
        private Integer matches;


        private List<IndividualData> individuals;
        private List<NonIndividualData> nonindividuals;
    }

    @Data
    @NoArgsConstructor
    @Accessors(chain = true)
    @AllArgsConstructor
    public static class IndividualData implements Serializable {

        private static final long serialVersionUID = 1L;

        private Integer individual_id;
        private String image;
        private String surname;
        private String forename1;
        private String forename2;
        private String forename3;
        private String gender;
        private String marital_status;
        private String date_of_birth;
        private String nationality;
    }

    @Data
    @NoArgsConstructor
    @Accessors(chain = true)
    @AllArgsConstructor
    public static class NonIndividualData implements Serializable {

        private static final long serialVersionUID = 1L;

        private Integer non_individual_id;
        private String business_name;
        private String trading_name;
        private String business_type;
        private String industry;
        private LocalDate date_registered;
        private String activity_description;
    }
}
