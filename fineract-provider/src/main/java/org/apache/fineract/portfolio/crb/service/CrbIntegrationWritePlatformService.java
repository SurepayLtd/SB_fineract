package org.apache.fineract.portfolio.crb.service;


import org.apache.fineract.portfolio.crb.domain.CrbValidationType;

public interface CrbIntegrationWritePlatformService {

    void createInitialCheck(final CrbValidationType type, final Long clientId);

    void reCheck(final CrbValidationType type, final Long clientId);
}
