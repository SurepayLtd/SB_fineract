package org.apache.fineract.portfolio.crb.service;

import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;

public interface CrbConsentWriteService {

    CommandProcessingResult createConsent(final Long clientId, JsonCommand command);

    CommandProcessingResult validateOtp(final Long consentId, JsonCommand command);

    CommandProcessingResult revokeConsent(final Long consentId, JsonCommand command);

    CommandProcessingResult reSendOtp(final Long consentId);

    CommandProcessingResult reactivateConsent(final Long consentId);
}
