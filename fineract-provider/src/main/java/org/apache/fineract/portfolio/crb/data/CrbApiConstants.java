package org.apache.fineract.portfolio.crb.data;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class CrbApiConstants {

    public static final String CRB_RESOURCE_NAME = "crb";

    public static final String localeParamName = "locale";
    public static final String dateFormatParamName = "dateFormat";
    public static final String consentTextParamName = "consentTextHash";
    public static final String consentChannelParamName = "consentChannel";
    public static final String otpCodeParamName = "otpCode";

    public static final String CRB_REVOKE_REASON = "CrbRevokeReason";



    protected static final Set<String> CRB_CREATE_REQUEST_DATA_PARAMETERS = new HashSet<>(Arrays.asList(localeParamName, dateFormatParamName, consentTextParamName, consentChannelParamName));

    protected static final Set<String> VALIDATE_OTP_REQUEST_DATA_PARAMETERS = new HashSet<>(Arrays.asList(localeParamName, dateFormatParamName, otpCodeParamName));

}
