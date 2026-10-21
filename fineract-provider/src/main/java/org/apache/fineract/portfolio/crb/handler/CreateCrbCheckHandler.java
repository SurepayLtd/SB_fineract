package org.apache.fineract.portfolio.crb.handler;

import lombok.RequiredArgsConstructor;
import org.apache.fineract.commands.annotation.CommandType;
import org.apache.fineract.commands.handler.NewCommandSourceHandler;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.portfolio.crb.service.CrbCheckWritePlatformService;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@CommandType(entity = "CRBCHECK", action = "CREATE")
public class CreateCrbCheckHandler implements NewCommandSourceHandler {

    private final CrbCheckWritePlatformService crbCheckWritePlatformService;

    @Override
    public CommandProcessingResult processCommand(JsonCommand command) {
        return crbCheckWritePlatformService.createInitialCheck(command.getClientId(), command);
    }
}
