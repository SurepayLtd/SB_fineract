package org.apache.fineract.portfolio.crb.service;

import org.apache.fineract.portfolio.crb.data.CrbConsentData;

public interface CrbConsentReadService {
    CrbConsentData retrieveConsent(Long consentId);
    CrbConsentData retrieveChannelTemplate();
    CrbConsentData retrieveAllNarrations(String consentNarrations);
}
