package org.apache.fineract.portfolio.crb.domain;

import org.apache.fineract.infrastructure.core.exception.GeneralPlatformDomainRuleException;

public enum CrbIdentificationType {

    Registration_Certificate_Number(1, "ii_registration_certificate_number", "CRB"),
    Tax_Identification_Number(2, "ii_tax_identification_number", "CRB"),
    Value_Added_Tax_Number(3, "ii_value_added_tax_number", "CRB"),
    Financial_Card_System_Number(4, "ii_fcs_number", "CRB"),
    Passport_Number(5, "ii_passport_number", "CRB"),
    Driver_License_ID_Number(6, "ii_drivers_license_permit_number", "CRB,MNO"),
    Voters_Personal_Identification_Number(7, "ii_voters_perno", "CRB"),
    Driver_License_Permit_Number(8, "ii_drivers_licence_id_number", "CRB"),
    NSSF_Number(9, "ii_nssf_number", "CRB"),
    Country_ID_Number(10, "ii_country_id", "CRB"),
    Police_ID_Number(11, "ii_police_id_number", "CRB,MNO"),
    UPDF_Number(12, "ii_updf_number", "CRB"),
    KACITA_License_Number(13, "ii_kasita_license_number", "CRB"),
    Public_Service_Pension_Number(14, "ii_public_service_pension_number", "CRB"),
    Teacher_Registration_Number(15, "ii_teacher_registration_number", "CRB"),
    Refugee_Number(16, "ii_refugee_number", "CRB"),
    Work_Permit_Number(17, "ii_work_permit_number", "CRB,MNO");

    private final Integer value;
    private final String requestValue;
    private final String supportedScores;

    CrbIdentificationType(final Integer value, final String requestValue, final String supportedScores) {
        this.value = value;
        this.requestValue = requestValue;
        this.supportedScores = supportedScores;
    }

    public static CrbIdentificationType fromInt(Integer value){
        for (CrbIdentificationType type: values()){
            if (type.value.equals(value)){
                return type;
            }
        }
        throw new GeneralPlatformDomainRuleException("error.msg.invalid.credit.score.value", "Invalid Credit Score value: " + value);
    }

    public Integer getValue() {
        return value;
    }

    public String getRequestValue() {
        return requestValue;
    }

    public String getSupportedScores() {
        return supportedScores;
    }
}
