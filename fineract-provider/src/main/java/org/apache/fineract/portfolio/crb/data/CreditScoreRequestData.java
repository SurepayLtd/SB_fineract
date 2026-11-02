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

    private Integer entityType;

    private String entityTypeCategory;

    private String identificationType;

    private String phoneNumber;

    private String clientConsented;

    private String type;

    private String piCode;
}
