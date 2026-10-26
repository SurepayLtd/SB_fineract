package org.apache.fineract.portfolio.crb.service;

import org.apache.fineract.portfolio.crb.data.CrbConsentData;

import java.util.Collection;

public interface CrbConsentReadService {
    CrbConsentData retrieveConsent(Long clientId);
    CrbConsentData retrieveChannelTemplate();
    CrbConsentData retrieveAllNarrations(String consentNarrations);
}
