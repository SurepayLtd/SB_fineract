package org.apache.fineract.portfolio.crb.service;

import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.creditbureau.service.CreditBureauIntegrationWritePlatformService;

public interface GnugridKycReportIntegrationWritePlatformService extends CreditBureauIntegrationWritePlatformService {

    CommandProcessingResult kycReports(Long clientId, JsonCommand command);
    String getKycReportPayload(Long clientId);
}
