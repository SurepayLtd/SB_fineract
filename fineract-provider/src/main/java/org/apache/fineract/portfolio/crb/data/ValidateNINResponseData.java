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
public class ValidateNINResponseData implements Serializable {

    private static final long serialVersionUID = 1L;

    private String message;
    private Integer status, count;
    private ValidationData validation;


    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ValidationData implements Serializable{

        private static final long serialVersionUID = 1L;

        private String id, reference, timestamp, status, nin, date_of_birth, name, nin_status, cost, currency;
        private String message, error_message;
    }
}
