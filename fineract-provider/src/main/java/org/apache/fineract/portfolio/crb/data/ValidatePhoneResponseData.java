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
public class ValidatePhoneResponseData implements Serializable {

    private static final long serialVersionUID = 1L;

    private String message;
    private Integer status, count;
    private ValidationData validation;


    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ValidationData implements Serializable{

        private static final long serialVersionUID = 1L;

        private String id, reference, timestamp, status, phonenumber, message, surname, firstname, middlename, cost, vat, currency;
        private String phone_status, error_message;
    }
}
