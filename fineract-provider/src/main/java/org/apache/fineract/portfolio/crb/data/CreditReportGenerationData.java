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
public class CreditReportGenerationData implements Serializable {

    private static final long serialVersionUID = 1L;

    private String enquiryId;

    private Long individualId;

    private Long nonIndividualId;

    private String accountId;

    private String reference;

    private Long timestamp;

    private String token;

    private String action;
}
