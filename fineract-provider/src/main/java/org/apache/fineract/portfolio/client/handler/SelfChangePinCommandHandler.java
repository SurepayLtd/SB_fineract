package org.apache.fineract.portfolio.client.handler;

import lombok.RequiredArgsConstructor;
import org.apache.fineract.commands.annotation.CommandType;
import org.apache.fineract.commands.handler.NewCommandSourceHandler;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.portfolio.client.service.ClientWritePlatformService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@CommandType(entity = "CLIENT", action = "UPDATECLIENTPIN")
@RequiredArgsConstructor
public class SelfChangePinCommandHandler implements NewCommandSourceHandler {

    private final ClientWritePlatformService clientWritePlatformService;

    @Transactional
    @Override
    public CommandProcessingResult processCommand(JsonCommand command) {
        return clientWritePlatformService.selfServiceChangePin(command.entityId(), command);
    }
}
