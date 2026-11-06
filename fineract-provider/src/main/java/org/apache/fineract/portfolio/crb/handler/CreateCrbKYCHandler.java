package org.apache.fineract.portfolio.crb.handler;

import lombok.RequiredArgsConstructor;
import org.apache.fineract.commands.annotation.CommandType;
import org.apache.fineract.commands.handler.NewCommandSourceHandler;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.portfolio.crb.service.CrbCreditCheckWriteService;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@CommandType(entity = "CRBKYC", action = "CREATE")
public class CreateCrbKYCHandler implements NewCommandSourceHandler {

    private final CrbCreditCheckWriteService crbCreditCheckWriteService;

    @Override
    public CommandProcessingResult processCommand(JsonCommand command) {
        return crbCreditCheckWriteService.kycCheck(command.getClientId(), command);
    }

}
