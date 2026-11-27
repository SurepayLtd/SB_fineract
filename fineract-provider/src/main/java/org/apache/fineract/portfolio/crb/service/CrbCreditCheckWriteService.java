package org.apache.fineract.portfolio.crb.service;

import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;

public interface CrbCreditCheckWriteService {

    CommandProcessingResult crbCredit(final Long loanId, final JsonCommand command);
    CommandProcessingResult kycCheck(final Long clientId, final JsonCommand command);
}
