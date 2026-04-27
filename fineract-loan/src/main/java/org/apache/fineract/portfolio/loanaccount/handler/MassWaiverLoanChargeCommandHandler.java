package org.apache.fineract.portfolio.loanaccount.handler;

import lombok.RequiredArgsConstructor;
import org.apache.fineract.commands.annotation.CommandType;
import org.apache.fineract.commands.handler.NewCommandSourceHandler;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.portfolio.loanaccount.service.LoanChargeWritePlatformService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@CommandType(entity = "LOANCHARGE", action = "BULKWAIVER")
public class MassWaiverLoanChargeCommandHandler implements NewCommandSourceHandler {

    private final LoanChargeWritePlatformService loanChargeWritePlatformService;

    @Override
    @Transactional
    public CommandProcessingResult processCommand(JsonCommand command) {
        return this.loanChargeWritePlatformService.massWaiveLoanCharge(command.getLoanId(), command);
    }
}
