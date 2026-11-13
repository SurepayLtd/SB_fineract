package org.apache.fineract.portfolio.crb.service;

import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.creditbureau.service.CreditBureauIntegrationWritePlatformService;


public interface GnugridCreditReportIntegrationWritePlatformService extends CreditBureauIntegrationWritePlatformService {

    CommandProcessingResult creditReports(Long loanId, JsonCommand command);
    String getCreditReportPayload(Long loanId);

}
