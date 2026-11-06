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
public class CreditScoreRequestData implements Serializable {

    private static final long serialVersionUID = 1L;

    private String identifier;

    private Integer entity_type;

    private String entity_type_category;

    private String identification_type;

    private String phone_number;

    private String client_consented;

    private String type;

    private String pi_code;
}
