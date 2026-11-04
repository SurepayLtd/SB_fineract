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
public class KycEnquiryRequestData implements Serializable {

    private static final long serialVersionUID = 1L;

    private String identifier, entity_type_category, identification_type, phone_number, type, client_consented, pi_code;
    private Integer entity_type;

}
