package org.apache.fineract.portfolio.crb.data;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class CrbApiConstants {

    public static final String CRB_RESOURCE_NAME = "CRBCONSENT";

    public static final String CRB_CHECK_RESOURCE_NAME = "CRBCHECK";

    public static final String localeParamName = "locale";
    public static final String dateFormatParamName = "dateFormat";
    public static final String consentTextParamName = "consentText";
    public static final String consentChannelParamName = "consentChannel";
    public static final String crbIdParamName = "id";
    public static final String crbClientIdParamName = "clientId";
    public static final String crbClientNameParamName = "clientName";
    public static final String consentStatusParamName = "consentStatus";
    public static final String phoneParamName = "phoneNumber";
    public static final String consentVersionParamName = "consentVersion";
    public static final String consentedAtParamName = "consentedAt";
    public static final String revokedAtParamName = "revokedAt";
    public static final String consentedByParamName = "consentedBy";
    public static final String verifiedByParamName = "verifiedBy";
    public static final String revokedByParamName = "revokedBy";
    public static final String otpCodeParamName = "otpCode";
    public static final String crbValidationTypeParamName= "crbValidationType";
    public static final String crbRequestTypeParamName= "crbRequestType";



    public static final String CRB_REVOKE_REASON = "CrbRevokeReason";
    public static final String revokeReasonIdParamName = "revokeReasonId";




    public static final Set<String> CRB_CREATE_CONSENT_REQUEST_DATA_PARAMETERS = new HashSet<>(Arrays.asList(localeParamName, dateFormatParamName, consentTextParamName, consentChannelParamName));

    public static final Set<String> CRB_CREATE_CHECK_REQUEST_DATA_PARAMETERS = new HashSet<>(Arrays.asList(localeParamName, dateFormatParamName, crbValidationTypeParamName, crbRequestTypeParamName));

    public static final Set<String> VALIDATE_OTP_REQUEST_DATA_PARAMETERS = new HashSet<>(Arrays.asList(localeParamName, dateFormatParamName, otpCodeParamName));

    public static final Set<String> CRB_CONSENT_RESPONSE_DATA_PARAMETERS =
            new HashSet<>(Arrays.asList(
                    crbIdParamName, crbClientIdParamName, crbClientNameParamName, consentChannelParamName,
                    consentStatusParamName, phoneParamName, consentVersionParamName, consentedAtParamName,
                    revokedAtParamName, consentedByParamName, verifiedByParamName, revokedByParamName
            ));

    public static final Set<String> CRB_CONSENT_TEMPLATE_RESPONSE_DATA_PARAMETERS = new HashSet<>(Arrays.asList("consentChannels"));

    public static final Set<String> CRB_CHECK_TEMPLATE_RESPONSE_DATA_PARAMETERS = new HashSet<>(Arrays.asList("types"));


}
