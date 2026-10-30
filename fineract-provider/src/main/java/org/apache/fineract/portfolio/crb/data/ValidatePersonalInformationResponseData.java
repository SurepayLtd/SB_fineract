package org.apache.fineract.portfolio.crb.data;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serializable;

@Data
@NoArgsConstructor
@Accessors(chain = true)
@AllArgsConstructor
public class ValidatePersonalInformationResponseData implements Serializable {

    private static final long serialVersionUID = 1L;

    private String message;
    private Integer status, count;
    private ValidationData validation;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ValidationData implements Serializable{

        private static final long serialVersionUID = 1L;

        private String id, reference, timestamp, status, message, nin_status, matching_status, card_status, cost, currency;
        private String nin, document_id, surname, given_name, other_name, date_of_birth, vat, error_message;
    }

}




