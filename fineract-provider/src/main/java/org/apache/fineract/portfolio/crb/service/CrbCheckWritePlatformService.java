package org.apache.fineract.portfolio.crb.service;


import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;

public interface CrbCheckWritePlatformService {

    CommandProcessingResult createInitialCheck(final Long clientId, final JsonCommand command);

    CommandProcessingResult reCheck(final Long clientId, final JsonCommand command);
}
