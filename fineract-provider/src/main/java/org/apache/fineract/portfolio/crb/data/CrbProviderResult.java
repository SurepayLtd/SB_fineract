package org.apache.fineract.portfolio.crb.data;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CrbProviderResult {

    private boolean verified;
    private String validationId;
    private String reference;
    private String status;
    private String message;
    private BigDecimal cost;
    private String currency;
    private String responsePayload;
    private String requestPayload;
}
