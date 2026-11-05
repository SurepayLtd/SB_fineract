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
public class KycReportRequestData implements Serializable {

    private static final long serialVersionUID = 1L;

    private String enquiry_id, format;
    private Integer individual_id, non_individual_id;
}
