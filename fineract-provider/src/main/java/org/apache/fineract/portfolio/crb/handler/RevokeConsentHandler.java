package org.apache.fineract.portfolio.crb.handler;

import lombok.RequiredArgsConstructor;
import org.apache.fineract.commands.annotation.CommandType;
import org.apache.fineract.commands.handler.NewCommandSourceHandler;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.portfolio.crb.service.CrbConsentWriteService;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
@CommandType(entity = "CRBCONSENT", action = "REVOKE")
public class RevokeConsentHandler implements NewCommandSourceHandler {

    private final CrbConsentWriteService crbConsentWriteService;

    @Override
    public CommandProcessingResult processCommand(JsonCommand command) {
        return this.crbConsentWriteService.revokeConsent(command.entityId(), command);
    }
}