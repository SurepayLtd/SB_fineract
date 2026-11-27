package org.apache.fineract.infrastructure.creditbureau.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.fineract.infrastructure.core.exception.GeneralPlatformDomainRuleException;
import org.springframework.stereotype.Component;

@Component
public class CreditBureauIntegrationFactory {

    private final Map<String, CreditBureauIntegrationWritePlatformService> integrations;

    public CreditBureauIntegrationFactory(List<CreditBureauIntegrationWritePlatformService> integrationServices) {

        Map<String, CreditBureauIntegrationWritePlatformService> integrationMap = new HashMap<>();

        for (CreditBureauIntegrationWritePlatformService integration : integrationServices) {
            String implementationKey = integration.getImplementationKey();

            if (implementationKey != null && !implementationKey.isBlank()) {
                integrationMap.put(implementationKey.toUpperCase(), integration);
            }
        }

        this.integrations = Map.copyOf(integrationMap);
    }

    public CreditBureauIntegrationWritePlatformService getIntegration(String implementationKey) {

        if (implementationKey == null || implementationKey.isBlank()) {
            throw new GeneralPlatformDomainRuleException("error.msg.credit.bureau.implementation.key.required",
                    "Credit bureau implementation key is required."
            );
        }

        CreditBureauIntegrationWritePlatformService integration = integrations.get(implementationKey.toUpperCase());

        if (integration == null) {
            throw new GeneralPlatformDomainRuleException("error.msg.credit.bureau.integration.not.found",
                    "No integration is registered for credit bureau implementation: " + implementationKey
            );
        }

        return integration;
    }
}