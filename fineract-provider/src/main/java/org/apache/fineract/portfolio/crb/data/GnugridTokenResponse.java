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
public class GnugridTokenResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private String token_type, access_token;
    private Long expires_in;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ErrorResponse implements Serializable{
        private String error, error_description, message;
    }
}
