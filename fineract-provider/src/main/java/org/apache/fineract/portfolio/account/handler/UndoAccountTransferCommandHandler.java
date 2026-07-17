package org.apache.fineract.portfolio.account.handler;

import lombok.RequiredArgsConstructor;
import org.apache.fineract.commands.annotation.CommandType;
import org.apache.fineract.commands.handler.NewCommandSourceHandler;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.portfolio.account.service.AccountTransfersWritePlatformService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@CommandType(entity = "ACCOUNTTRANSFER", action = "ACTION_UNDO")
public class UndoAccountTransferCommandHandler implements NewCommandSourceHandler {

    private final AccountTransfersWritePlatformService writePlatformService;

    @Override
    @Transactional
    public CommandProcessingResult processCommand(JsonCommand command) {
        return writePlatformService.accountTransferReversal(command);
    }
}
