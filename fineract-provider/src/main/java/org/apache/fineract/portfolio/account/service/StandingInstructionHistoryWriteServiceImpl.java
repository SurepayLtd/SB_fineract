package org.apache.fineract.portfolio.account.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.portfolio.account.data.AccountTransferDTO;
import org.apache.fineract.portfolio.account.domain.AccountTransferStandingInstruction;
import org.apache.fineract.portfolio.account.domain.StandingInstructionHistory;
import org.apache.fineract.portfolio.account.domain.StandingInstructionHistoryRepository;
import org.apache.fineract.portfolio.account.domain.StandingInstructionRepository;
import org.apache.fineract.portfolio.account.exception.StandingInstructionNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Slf4j
public class StandingInstructionHistoryWriteServiceImpl implements StandingInstructionHistoryWriteService{

    private final StandingInstructionHistoryRepository standingInstructionHistoryRepository;
    private final AccountTransfersWritePlatformService accountTransfersWritePlatformService;
    private final StandingInstructionRepository standingInstructionRepository;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void transferFunds(final AccountTransferDTO accountTransferDTO, final Long standingInstructionId, final LocalDate transactionDate, final BigDecimal transferredAmount) {

        AccountTransferStandingInstruction instruction = this.standingInstructionRepository.findById(standingInstructionId)
                .orElseThrow(() -> new StandingInstructionNotFoundException(standingInstructionId));

        this.accountTransfersWritePlatformService.transferFunds(accountTransferDTO);

        StandingInstructionHistory history = StandingInstructionHistory.success(standingInstructionId, transferredAmount);
        this.standingInstructionHistoryRepository.saveAndFlush(history);

        instruction.setLatsRunDate(transactionDate);

        this.standingInstructionRepository.save(instruction);

    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailure(final Long standingInstructionId, final String errorLog,  final LocalDate transactionDate) {

        AccountTransferStandingInstruction instruction = this.standingInstructionRepository.findById(standingInstructionId)
                .orElseThrow(() -> new StandingInstructionNotFoundException(standingInstructionId));

        StandingInstructionHistory history = StandingInstructionHistory.failed(standingInstructionId, errorLog);
        this.standingInstructionHistoryRepository.saveAndFlush(history);

        instruction.setLatsRunDate(transactionDate);

        this.standingInstructionRepository.save(instruction);
    }
}
