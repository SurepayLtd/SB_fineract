package org.apache.fineract.portfolio.crb.data;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@Accessors(chain = true)
@AllArgsConstructor
public class KycEnquiryResponseData implements Serializable {

    private static final long serialVersionUID = 1L;

    private String message;
    private Integer status;
    private Integer count;

    private KycInformationData enquiry;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class KycInformationData implements Serializable {

        private static final long serialVersionUID = 1L;

        private String id, entity, timestamp, reference, criteria, search_value, identification_type, status;
        private BigDecimal cost, vat, charge;

        private Formal formal;

        private Informal informal;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Formal implements Serializable {

        private static final long serialVersionUID = 1L;

        private Integer matches;
        private String id, entity, timestamp, reference, criteria, search_value, identification_type, sector;

        private List<Individuals> individuals;
        private List<NonIndividuals> nonindividuals;

    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Individuals implements Serializable {

        private static final long serialVersionUID = 1L;

        private String  image, surname, forename1, forename2, forename3, gender, marital_status, date_of_birth, nationality;
        private Integer individual_id;

    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NonIndividuals implements Serializable {

        private static final long serialVersionUID = 1L;

        private String business_name, trading_name, business_type, industry, date_registered, activity_description;
        private Integer non_individual_id;


    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Informal implements Serializable {

        private static final long serialVersionUID = 1L;

        private Integer matches;

        private String sector;

        private List<Individuals> individuals;
        private List<NonIndividuals> nonindividuals;

    }
}
