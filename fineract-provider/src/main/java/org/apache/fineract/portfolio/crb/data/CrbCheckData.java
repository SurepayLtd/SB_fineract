package org.apache.fineract.portfolio.crb.data;


import lombok.Getter;
import lombok.Setter;
import org.apache.fineract.infrastructure.core.data.EnumOptionData;
import org.apache.fineract.infrastructure.core.exception.GeneralPlatformDomainRuleException;
import org.apache.fineract.portfolio.client.domain.CRBStatus;
import org.apache.fineract.portfolio.crb.domain.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Map;

@Getter
@Setter
public final class CrbCheckData {
    private final Long id;
    private final Long clientId;
    private final String clientName;
    private final String clientAccount;

    private final Long loanId;
    private final String loanProductName;
    private final String loanAccount;
    private final BigDecimal proposedPrincipal;

    private final EnumOptionData validationType;
    private final EnumOptionData requestType;
    private final EnumOptionData status;
    private final EnumOptionData consentStatus;

    private final String providerValidationId;
    private final String providerReference;

    private final BigDecimal cost;
    private final String currency;

    private final String message;
    private final String errorMessage;

    private final String responsePayload;

    private final LocalDateTime createdAt;
    private final LocalDateTime completedAt;

    private final Map<String, Object> responseDetails;

    private final String reportUrl;

    private final Collection<EnumOptionData> requestTypes;

    private final Collection<EnumOptionData> validationTypes;

    private final Collection<EnumOptionData> creditReportActionTypes;

    private final Collection<EnumOptionData> creditReportFormatTypes;

    private final Collection<EnumOptionData> creditReportTypes;

    private final Collection<EnumOptionData> creditValidationTypes;

    private final Collection<EnumOptionData> creditScoreTypes;





    private CrbCheckData(Long id, Long clientId, String clientName, String clientAccount, Long loanId, String loanProductName, String loanAccount, BigDecimal proposedPrincipal, EnumOptionData validationType, EnumOptionData requestType, EnumOptionData status, EnumOptionData consentStatus, String providerValidationId,
                        String providerReference, BigDecimal cost, String currency, String message, String errorMessage, String responsePayload, Map<String, Object> responseDetails, LocalDateTime createdAt, LocalDateTime completedAt, String reportUrl) {
        this.id = id;
        this.clientId = clientId;
        this.clientName = clientName;
        this.clientAccount = clientAccount;
        this.loanId = loanId;
        this.loanProductName = loanProductName;
        this.loanAccount = loanAccount;
        this.proposedPrincipal = proposedPrincipal;
        this.validationType = validationType;
        this.requestType = requestType;
        this.status = status;
        this.consentStatus = consentStatus;
        this.providerValidationId = providerValidationId;
        this.providerReference = providerReference;
        this.cost = cost;
        this.currency = currency;
        this.message = message;
        this.errorMessage = errorMessage;
        this.responsePayload = responsePayload;
        this.createdAt = createdAt;
        this.completedAt = completedAt;
        this.responseDetails = responseDetails;
        this.reportUrl = reportUrl;
        this.requestTypes = null;
        this.validationTypes = null;
        this.creditReportTypes = null;
        this.creditValidationTypes = null;
        this.creditReportActionTypes = null;
        this.creditReportFormatTypes = null;
        this.creditScoreTypes = null;

    }

    private CrbCheckData(Collection<EnumOptionData> requestTypes, Collection<EnumOptionData> validationTypes, Collection<EnumOptionData> creditReportActionTypes, Collection<EnumOptionData> creditReportFormatTypes,
                        Collection<EnumOptionData> creditReportTypes, Collection<EnumOptionData> creditValidationTypes, Collection<EnumOptionData> creditScoreTypes) {
        this.requestTypes = requestTypes;
        this.validationTypes = validationTypes;
        this.creditReportFormatTypes = creditReportFormatTypes;
        this.creditReportActionTypes = creditReportActionTypes;
        this.creditValidationTypes = creditValidationTypes;
        this.creditReportTypes = creditReportTypes;
        this.creditScoreTypes = creditScoreTypes;
        this.id = null;
        this.clientId = null;
        this.clientName = null;
        this.clientAccount = null;
        this.loanId = null;
        this.loanAccount = null;
        this.loanProductName = null;
        this.proposedPrincipal = null;
        this.validationType = null;
        this.requestType = null;
        this.status = null;
        this.consentStatus = null;
        this.providerValidationId = null;
        this.providerReference = null;
        this.cost = null;
        this.currency = null;
        this.message = null;
        this.errorMessage = null;
        this.responsePayload = null;
        this.createdAt = null;
        this.completedAt = null;
        this.responseDetails = null;
        this.reportUrl = null;
    }

    public static CrbCheckData display(Long id, Long clientId, String clientName, String clientAccount, Long loanId, String loanProductName, String loanAccount, BigDecimal proposedPrincipal, Integer validationType, Integer requestType, Integer status, Integer consentStatus, String providerValidationId,
                                       String providerReference, BigDecimal cost, String currency, String message, String errorMessage, String responsePayload, Map<String, Object> responseDetails, LocalDateTime createdAt, LocalDateTime completedAt, String reportUrl){
        return new CrbCheckData(id, clientId, clientName, clientAccount, loanId, loanProductName, loanAccount, proposedPrincipal, getValidationType(validationType), getRequestType(requestType), getStatus(status), getConsentStatus(consentStatus), providerValidationId,
                providerReference, cost, currency, message, errorMessage, responsePayload, responseDetails, createdAt, completedAt, reportUrl);
    }

    public static CrbCheckData dropdown(Collection<EnumOptionData> requestTypes, Collection<EnumOptionData> validationTypes, Collection<EnumOptionData> creditReportActionTypes, Collection<EnumOptionData> creditReportFormatTypes,
                                        Collection<EnumOptionData> creditReportTypes, Collection<EnumOptionData> creditValidationTypes, Collection<EnumOptionData> creditScoreTypes){
        return new CrbCheckData(requestTypes, validationTypes, creditReportActionTypes, creditReportFormatTypes, creditReportTypes, creditValidationTypes, creditScoreTypes);
    }

    public static EnumOptionData getRequestType(Integer value) {
        CrbRequestType type = CrbRequestType.fromInt(value);
        return new EnumOptionData(type.getValue().longValue(), type.getCode(), type.getDescription());
    }

    public static EnumOptionData getConsentStatus(Integer value) {
        ConsentStatus status1  = ConsentStatus.fromInt(value);
        return new EnumOptionData(status1.getValue().longValue(), status1.getCode(), status1.getDescription());
    }

    public static EnumOptionData getStatus(Integer value) {
        CRBStatus crbStatus = CRBStatus.fromInt(value);
        return new EnumOptionData(crbStatus.getValue().longValue(), crbStatus.getCode(), crbStatus.getDescription());
    }

    public static EnumOptionData getValidationType(Integer value) {

        if (value == null) {
            return null;
        }

        for (CrbValidationType type : CrbValidationType.values()) {
            if (type.getValue().equals(value)) {
                return new EnumOptionData(type.getValue().longValue(), type.getCode(), type.getDescription());
            }
        }

        for (CreditValidationType type : CreditValidationType.values()) {
            if (type.getValue().equals(value)) {
                return new EnumOptionData(type.getValue().longValue(), type.getCode(), type.getDescription());
            }
        }

        for (CreditReports type : CreditReports.values()) {
            if (type.getValue().equals(value)) {
                return new EnumOptionData(type.getValue().longValue(), type.getCode(), type.getDescription());
            }
        }

        throw new GeneralPlatformDomainRuleException("error.msg.invalid.crb.validation.value",
                "Invalid CRB validation value: " + value
        );
    }

}
