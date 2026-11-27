package org.apache.fineract.portfolio.loanaccount.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.exception.GeneralPlatformDomainRuleException;
import org.apache.fineract.infrastructure.core.service.ExternalIdFactory;
import org.apache.fineract.portfolio.loanaccount.domain.Loan;
import org.apache.fineract.portfolio.loanaccount.domain.LoanTransaction;
import org.apache.fineract.portfolio.loanaccount.domain.LoanTransactionRepository;
import org.apache.fineract.portfolio.loanaccount.exception.LoanTransactionNotFoundException;
import org.apache.fineract.portfolio.loanaccount.service.adjustment.LoanAdjustmentParameter;
import org.apache.fineract.portfolio.loanaccount.service.adjustment.LoanAdjustmentService;
import org.springframework.transaction.annotation.Transactional;

/**
 * SurePay: reverses the loan side of an account transfer (ACTION_UNDO). The adjustment itself is delegated to upstream's
 * {@link LoanAdjustmentService}, the same service upstream's own account transfer undo uses.
 */
@RequiredArgsConstructor
public class LoanAccountTransferReversalServiceImpl implements LoanAccountTransferReversalService {

    private static final long MOBILE_MONEY_PAYMENT_TYPE_ID = 100L;

    private final LoanTransactionRepository loanTransactionRepository;
    private final LoanAdjustmentService loanAdjustmentService;
    private final ExternalIdFactory externalIdFactory;

    @Transactional
    @Override
    public CommandProcessingResult adjustLoanTransaction(final Long loanId, final Long transactionId, final JsonCommand command) {
        final LoanTransaction transactionToAdjust = this.loanTransactionRepository.findByIdAndLoanId(transactionId, loanId)
                .orElseThrow(() -> new LoanTransactionNotFoundException(transactionId, loanId));

        if (transactionToAdjust.getPaymentDetail() != null && transactionToAdjust.getPaymentDetail().getPaymentType() != null
                && Long.valueOf(MOBILE_MONEY_PAYMENT_TYPE_ID).equals(transactionToAdjust.getPaymentDetail().getPaymentType().getId())) {
            throw new GeneralPlatformDomainRuleException("error.mgs.undo.loan.repayment.is.not.supported.for.mobile.money.repayment",
                    "Undo Repayment is not supported for mobile money transaction/s ");
        }

        final Loan loan = transactionToAdjust.getLoan();
        final BigDecimal requestedAmount = command.bigDecimalValueOfParameterNamed("transactionAmount");
        final BigDecimal transactionAmount = requestedAmount != null ? requestedAmount : BigDecimal.ZERO;
        final LocalDate requestedDate = command.localDateValueOfParameterNamed("transactionDate");
        final LocalDate transactionDate = requestedDate != null ? requestedDate : transactionToAdjust.getTransactionDate();

        final Map<String, Object> changes = new LinkedHashMap<>();
        changes.put("transactionDate", transactionDate);
        changes.put("transactionAmount", transactionAmount);

        final LoanAdjustmentParameter parameter = LoanAdjustmentParameter.builder().transactionAmount(transactionAmount)
                .paymentDetail(null).transactionDate(transactionDate).txnExternalId(transactionToAdjust.getExternalId())
                .reversalTxnExternalId(externalIdFactory.create()).noteText(command.stringValueOfParameterNamed("note")).build();

        return this.loanAdjustmentService.adjustLoanTransaction(loan, transactionToAdjust, parameter, command.commandId(), changes);
    }
}
