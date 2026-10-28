package org.apache.fineract.portfolio.crb.data;


import lombok.Getter;
import lombok.Setter;
import org.apache.fineract.infrastructure.core.data.EnumOptionData;
import org.apache.fineract.portfolio.client.domain.CRBStatus;
import org.apache.fineract.portfolio.crb.domain.ConsentStatus;
import org.apache.fineract.portfolio.crb.domain.CrbRequestType;
import org.apache.fineract.portfolio.crb.domain.CrbValidationType;

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

    private final Collection<EnumOptionData> requestTypes;

    private final Collection<EnumOptionData> validationTypes;


    private CrbCheckData(Long id, Long clientId, String clientName, EnumOptionData validationType, EnumOptionData requestType, EnumOptionData status, EnumOptionData consentStatus, String providerValidationId,
                        String providerReference, BigDecimal cost, String currency, String message, String errorMessage, String responsePayload, Map<String, Object> responseDetails, LocalDateTime createdAt, LocalDateTime completedAt) {
        this.id = id;
        this.clientId = clientId;
        this.clientName = clientName;
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
        this.requestTypes = null;
        this.validationTypes = null;
    }

    public CrbCheckData(Collection<EnumOptionData> requestTypes, Collection<EnumOptionData> validationTypes) {
        this.requestTypes = requestTypes;
        this.validationTypes = validationTypes;
        this.id = null;
        this.clientId = null;
        this.clientName = null;
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
    }

    public static CrbCheckData display(Long id, Long clientId, String clientName, Integer validationType, Integer requestType, Integer status, Integer consentStatus, String providerValidationId,
                                       String providerReference, BigDecimal cost, String currency, String message, String errorMessage, String responsePayload, Map<String, Object> responseDetails, LocalDateTime createdAt, LocalDateTime completedAt){
        return new CrbCheckData(id, clientId, clientName, getValidationType(validationType), getRequestType(requestType), getStatus(status), getConsentStatus(consentStatus), providerValidationId,
                providerReference, cost, currency, message, errorMessage, responsePayload, responseDetails, createdAt, completedAt);
    }

    public static CrbCheckData dropdown(Collection<EnumOptionData> requestTypes, Collection<EnumOptionData> validationTypes){
        return new CrbCheckData(requestTypes, validationTypes);
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
        CrbValidationType type = CrbValidationType.fromInt(value);
        return new EnumOptionData(type.getValue().longValue(), type.getCode(), type.getDescription());
    }

}
