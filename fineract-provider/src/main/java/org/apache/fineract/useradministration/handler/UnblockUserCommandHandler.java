package org.apache.fineract.useradministration.handler;

import lombok.RequiredArgsConstructor;
import org.apache.fineract.commands.annotation.CommandType;
import org.apache.fineract.commands.handler.NewCommandSourceHandler;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.useradministration.service.AppUserWritePlatformService;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@CommandType(entity = "USER", action = "UNBLOCK")
public class UnblockUserCommandHandler implements NewCommandSourceHandler {
    private final AppUserWritePlatformService appUserWritePlatformService;

    @Override
    public CommandProcessingResult processCommand(JsonCommand command) {
        return appUserWritePlatformService.unBlockUser(command.entityId());
    }
}
