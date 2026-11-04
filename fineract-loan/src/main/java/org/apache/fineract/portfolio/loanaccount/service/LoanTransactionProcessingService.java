package org.apache.fineract.portfolio.loanaccount.service;

import lombok.RequiredArgsConstructor;
import org.apache.fineract.infrastructure.core.service.DateUtils;
import org.apache.fineract.organisation.monetary.domain.Money;
import org.apache.fineract.portfolio.loanaccount.data.ScheduleGeneratorDTO;
import org.apache.fineract.portfolio.loanaccount.domain.Loan;
import org.apache.fineract.portfolio.loanaccount.domain.LoanLifecycleStateMachine;
import org.apache.fineract.portfolio.loanaccount.domain.LoanTransaction;
import org.apache.fineract.portfolio.loanaccount.domain.LoanRepaymentScheduleTransactionProcessorFactory;
import org.apache.fineract.portfolio.loanaccount.domain.LoanRepaymentScheduleInstallment;
import org.apache.fineract.portfolio.loanaccount.domain.LoanEvent;
import org.apache.fineract.portfolio.loanaccount.domain.transactionprocessor.LoanRepaymentScheduleTransactionProcessor;
import org.apache.fineract.portfolio.loanaccount.domain.transactionprocessor.MoneyHolder;
import org.apache.fineract.portfolio.loanaccount.domain.transactionprocessor.TransactionCtx;
import org.apache.fineract.portfolio.loanaccount.exception.InvalidLoanStateTransitionException;
import org.apache.fineract.portfolio.loanaccount.exception.InvalidLoanTransactionTypeException;
import org.apache.fineract.portfolio.loanaccount.loanschedule.domain.LoanScheduleType;
import org.apache.fineract.portfolio.loanaccount.serialization.LoanRefundValidator;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class LoanTransactionProcessingService {

    private final LoanScheduleService loanScheduleService;
    private final LoanRefundService loanRefundService;
    private final LoanRefundValidator loanRefundValidator;
    private final LoanRepaymentScheduleTransactionProcessorFactory transactionProcessorFactory;


    public void handleRepaymentOrRecoveryOrWaiverReversalTransaction(String transactionProcessingStrategyCode, final Loan loan, LoanLifecycleStateMachine loanLifecycleStateMachine, final LoanTransaction loanTransaction, final LoanTransaction adjustedTransaction, final ScheduleGeneratorDTO scheduleGeneratorDTO) {
        if (loanTransaction.isRecoveryRepayment()) {
            loanLifecycleStateMachine.transition(LoanEvent.LOAN_RECOVERY_PAYMENT, loan);
        }

        if (loanTransaction.isRecoveryRepayment()
                && loanTransaction.getAmount(loan.getCurrency()).getAmount().compareTo(loan.getSummary().getTotalWrittenOff()) > 0) {
            final String errorMessage = "The transaction amount cannot greater than the remaining written off amount.";
            throw new InvalidLoanStateTransitionException("transaction", "cannot.be.greater.than.total.written.off", errorMessage);
        }

        loanTransaction.updateLoan(loan);

        final boolean isTransactionChronologicallyLatest = loan.isChronologicallyLatestRepaymentOrWaiver(loanTransaction);


        if (loanTransaction.isNotZero()) {
            loan.addLoanTransaction(loanTransaction);
        }

        if (loanTransaction.isNotRepaymentLikeType() && loanTransaction.isNotWaiver() && loanTransaction.isNotRecoveryRepayment()) {
            final String errorMessage = "A transaction of type repayment or recovery repayment or waiver was expected but not received.";
            throw new InvalidLoanTransactionTypeException("transaction", "is.not.a.repayment.or.waiver.or.recovery.transaction",
                    errorMessage);
        }

        final LocalDate loanTransactionDate = loanRefundService.extractTransactionDate(loan, loanTransaction);

        if (DateUtils.isDateInTheFuture(loanTransactionDate)) {
            final String errorMessage = "The transaction date cannot be in the future.";
            throw new InvalidLoanStateTransitionException("transaction", "cannot.be.a.future.date", errorMessage, loanTransactionDate);
        }

        if (loanTransaction.isInterestWaiver()) {
            Money totalInterestOutstandingOnLoan = loan.getTotalInterestOutstandingOnLoan();
            if (adjustedTransaction != null) {
                totalInterestOutstandingOnLoan = totalInterestOutstandingOnLoan.plus(adjustedTransaction.getAmount(loan.loanCurrency()));
            }
            if (loanTransaction.getAmount(loan.getCurrency()).isGreaterThan(totalInterestOutstandingOnLoan)) {
                final String errorMessage = "The amount of interest to waive cannot be greater than total interest outstanding on loan.";
                throw new InvalidLoanStateTransitionException("waive.interest", "amount.exceeds.total.outstanding.interest", errorMessage,
                        loanTransaction.getAmount(loan.getCurrency()), totalInterestOutstandingOnLoan.getAmount());
            }
        }

        loanRefundValidator.validateTransactionAmountThreshold(loan, adjustedTransaction);

        final LoanRepaymentScheduleInstallment currentInstallment = loan
                .fetchLoanRepaymentScheduleInstallmentByDueDate(loanTransaction.getTransactionDate());

        final LoanRepaymentScheduleTransactionProcessor loanRepaymentScheduleTransactionProcessor = getTransactionProcessor(transactionProcessingStrategyCode);


        boolean reprocess = loan.isForeclosure() || !isTransactionChronologicallyLatest || adjustedTransaction != null
                || !DateUtils.isEqualBusinessDate(loanTransaction.getTransactionDate()) || currentInstallment == null
                || !currentInstallment.getTotalOutstanding(loan.getCurrency()).isEqualTo(loanTransaction.getAmount(loan.getCurrency()));

        if (isTransactionChronologicallyLatest && adjustedTransaction == null
                && (!reprocess || !loan.isInterestBearingAndInterestRecalculationEnabled()) && !loan.isForeclosure()) {
            loanRepaymentScheduleTransactionProcessor.processLatestTransaction(loanTransaction,
                    new TransactionCtx(loan.getCurrency(), loan.getRepaymentScheduleInstallments(), loan.getActiveCharges(),
                            new MoneyHolder(loan.getTotalOverpaidAsMoney()), null));
            reprocess = false;
            if (loan.isInterestBearingAndInterestRecalculationEnabled()) {
                if (currentInstallment == null || currentInstallment.isNotFullyPaidOff()) {
                    reprocess = true;
                } else {
                    final LoanRepaymentScheduleInstallment nextInstallment = loan
                            .fetchRepaymentScheduleInstallment(currentInstallment.getInstallmentNumber() + 1);
                    if (nextInstallment != null && nextInstallment.getTotalPaidInAdvance(loan.getCurrency()).isGreaterThanZero()) {
                        reprocess = true;
                    }
                }
            }
        }
        if (reprocess) {
            if (loan.isInterestBearingAndInterestRecalculationEnabled()
                    && !loan.getLoanProductRelatedDetail().getLoanScheduleType().equals(LoanScheduleType.PROGRESSIVE)) {
                loanScheduleService.regenerateRepaymentScheduleWithInterestRecalculation(loan, scheduleGeneratorDTO);
            } else if (loan.getLoanProductRelatedDetail() != null
                    && loan.getLoanProductRelatedDetail().getLoanScheduleType().equals(LoanScheduleType.PROGRESSIVE)
                    && loan.getLoanTransactions().stream().anyMatch(LoanTransaction::isChargeOff)) {
                loanScheduleService.regenerateRepaymentSchedule(loan, scheduleGeneratorDTO);
            }
            loan.reprocessTransactions();
        }

        loan.updateLoanSummaryDerivedFields();

        /**
         * FIXME: Vishwas, skipping post loan transaction checks for Loan recoveries
         **/
        if (loanTransaction.isNotRecoveryRepayment()) {
            loan.doPostLoanTransactionChecks(loanTransaction.getTransactionDate(), loanLifecycleStateMachine);
        }

        if (loan.getLoanProduct().isMultiDisburseLoan()) {
            final BigDecimal totalDisbursed = loan.getDisbursedAmount();
            final BigDecimal totalPrincipalAdjusted = loan.getSummary().getTotalPrincipalAdjustments();
            final BigDecimal totalPrincipalCredited = totalDisbursed.add(totalPrincipalAdjusted);
            if (totalPrincipalCredited.compareTo(loan.getSummary().getTotalPrincipalRepaid()) < 0
                    && loan.repaymentScheduleDetail().getPrincipal().minus(totalDisbursed).isGreaterThanZero()) {
                final String errorMessage = "The transaction amount cannot exceed threshold.";
                throw new InvalidLoanStateTransitionException("transaction", "amount.exceeds.threshold", errorMessage);
            }
        }
    }

    private LoanRepaymentScheduleTransactionProcessor getTransactionProcessor(String transactionProcessingStrategyCode) {
        return transactionProcessorFactory.determineProcessor(transactionProcessingStrategyCode);
    }
}
