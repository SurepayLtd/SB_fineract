package org.apache.fineract.portfolio.crb.data;

import lombok.*;
import lombok.experimental.Accessors;

import java.io.Serializable;

@Data
@NoArgsConstructor
@Accessors(chain = true)
public class ValidatePersonalInformationRequestData implements Serializable {

    private static final long serialVersionUID = 1L;

    private String nin, surname, given_name, other_name, date_of_birth;
    private Integer document_id;

    public ValidatePersonalInformationRequestData(String nin, String surname, String given_name, String other_name,
                                                  String date_of_birth, Integer document_id) {
        this.nin = nin;
        this.surname = surname;
        this.given_name = given_name;
        this.other_name = other_name;
        this.date_of_birth = date_of_birth;
        this.document_id = document_id;
    }
}
