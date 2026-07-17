package org.apache.fineract.portfolio.loanaccount.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.fineract.accounting.journalentry.service.JournalEntryWritePlatformService;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.ApiParameterError;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResultBuilder;
import org.apache.fineract.infrastructure.core.data.DataValidatorBuilder;
import org.apache.fineract.infrastructure.core.domain.ExternalId;
import org.apache.fineract.infrastructure.core.exception.GeneralPlatformDomainRuleException;
import org.apache.fineract.infrastructure.core.exception.PlatformApiDataValidationException;
import org.apache.fineract.infrastructure.core.exception.PlatformServiceUnavailableException;
import org.apache.fineract.infrastructure.core.service.DateUtils;
import org.apache.fineract.infrastructure.core.service.ExternalIdFactory;
import org.apache.fineract.infrastructure.event.business.domain.loan.LoanAdjustTransactionBusinessEvent;
import org.apache.fineract.infrastructure.event.business.domain.loan.LoanBalanceChangedBusinessEvent;
import org.apache.fineract.infrastructure.event.business.service.BusinessEventNotifierService;
import org.apache.fineract.organisation.monetary.domain.MonetaryCurrency;
import org.apache.fineract.organisation.monetary.domain.Money;
import org.apache.fineract.portfolio.client.domain.Client;
import org.apache.fineract.portfolio.client.exception.ClientNotActiveException;
import org.apache.fineract.portfolio.group.domain.Group;
import org.apache.fineract.portfolio.group.exception.GroupNotActiveException;
import org.apache.fineract.portfolio.loanaccount.api.LoanApiConstants;
import org.apache.fineract.portfolio.loanaccount.data.HolidayDetailDTO;
import org.apache.fineract.portfolio.loanaccount.data.ScheduleGeneratorDTO;
import org.apache.fineract.portfolio.loanaccount.domain.Loan;
import org.apache.fineract.portfolio.loanaccount.domain.LoanTransaction;
import org.apache.fineract.portfolio.loanaccount.domain.LoanRepositoryWrapper;
import org.apache.fineract.portfolio.loanaccount.domain.LoanEvent;
import org.apache.fineract.portfolio.loanaccount.domain.LoanAccountDomainService;
import org.apache.fineract.portfolio.loanaccount.domain.LoanTransactionRepository;
import org.apache.fineract.portfolio.loanaccount.domain.LoanRepaymentScheduleInstallmentRepository;
import org.apache.fineract.portfolio.loanaccount.domain.LoanLifecycleStateMachine;
import org.apache.fineract.portfolio.loanaccount.domain.LoanSubStatus;
import org.apache.fineract.portfolio.loanaccount.domain.LoanChargePaidBy;
import org.apache.fineract.portfolio.loanaccount.domain.LoanCharge;
import org.apache.fineract.portfolio.loanaccount.domain.LoanTransactionType;
import org.apache.fineract.portfolio.loanaccount.domain.LoanTransactionRelationTypeEnum;
import org.apache.fineract.portfolio.loanaccount.exception.InvalidLoanTransactionTypeException;
import org.apache.fineract.portfolio.loanaccount.exception.LoanForeclosureException;
import org.apache.fineract.portfolio.loanaccount.exception.LoanTransactionNotFoundException;
import org.apache.fineract.portfolio.loanaccount.loanschedule.domain.LoanScheduleType;
import org.apache.fineract.portfolio.loanaccount.serialization.LoanChargeValidator;
import org.apache.fineract.portfolio.loanaccount.serialization.LoanTransactionValidator;
import org.apache.fineract.portfolio.note.domain.Note;
import org.apache.fineract.portfolio.note.domain.NoteRepository;
import org.apache.fineract.portfolio.paymentdetail.domain.PaymentDetail;
import org.apache.fineract.portfolio.paymentdetail.service.PaymentDetailWritePlatformService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.jpa.JpaSystemException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.ArrayList;




@Service
@RequiredArgsConstructor
@Slf4j
public class LoanAccountTransferReversalServiceImpl implements LoanAccountTransferReversalService{

    private final LoanTransactionValidator loanTransactionValidator;
    private final LoanRepositoryWrapper loanRepositoryWrapper;
    private final LoanAccountDomainService loanAccountDomainService;
    private final NoteRepository noteRepository;
    private final LoanTransactionRepository loanTransactionRepository;
    private final JournalEntryWritePlatformService journalEntryWritePlatformService;
    private final PaymentDetailWritePlatformService paymentDetailWritePlatformService;
    private final BusinessEventNotifierService businessEventNotifierService;
    private final LoanUtilService loanUtilService;
    private final LoanRepaymentScheduleInstallmentRepository loanRepaymentScheduleInstallmentRepository;
    private final LoanLifecycleStateMachine loanLifecycleStateMachine;
    private final ExternalIdFactory externalIdFactory;
    private final LoanAccrualTransactionBusinessEventService loanAccrualTransactionBusinessEventService;
    private final LoanDownPaymentHandlerService loanDownPaymentHandlerService;
    private final LoanAccrualsProcessingService loanAccrualsProcessingService;
    private final LoanChargeValidator loanChargeValidator;
    private final LoanAssembler loanAssembler;



    @Transactional
    @Override
    public CommandProcessingResult adjustLoanTransaction(final Long loanId, final Long transactionId, JsonCommand command) {

        this.loanTransactionValidator.validateTransaction(command.json());
        LoanTransaction transactionToAdjust = this.loanTransactionRepository.findByIdAndLoanId(transactionId, loanId)
                .orElseThrow(() -> new LoanTransactionNotFoundException(transactionId, loanId));

        if (transactionToAdjust.getPaymentDetail() != null && transactionToAdjust.getPaymentDetail().getPaymentType() != null
                && transactionToAdjust.getPaymentDetail().getPaymentType().getId() == 100) {
            throw new GeneralPlatformDomainRuleException("error.mgs.undo.loan.repayment.is.not.supported.for.mobile.money.repayment",
                    "Undo Repayment is not supported for mobile money transaction/s " );
        }

        Loan loan = this.loanAssembler.assembleFrom(loanId);
        if (loan.getStatus().isClosed() && loan.getLoanSubStatus() != null
                && loan.getLoanSubStatus().equals(LoanSubStatus.FORECLOSED.getValue())) {
            final String defaultUserMessage = "The loan cannot reopened as it is foreclosed.";
            throw new LoanForeclosureException("loan.cannot.be.reopened.as.it.is.foreclosured", defaultUserMessage, loan.getId());
        }
        checkClientOrGroupActive(loan);

        businessEventNotifierService.notifyPreBusinessEvent(
                new LoanAdjustTransactionBusinessEvent(new LoanAdjustTransactionBusinessEvent.Data(transactionToAdjust)));

        if (loan.isClosedWrittenOff()) {
            throw new PlatformServiceUnavailableException("error.msg.loan.written.off.update.not.allowed",
                    "Loan transaction: " + transactionId + " update not allowed as loan status is written off", transactionId);
        }

        if (transactionToAdjust.hasChargebackLoanTransactionRelations()) {
            throw new PlatformServiceUnavailableException("error.msg.loan.transaction.update.not.allowed",
                    "Loan transaction: " + transactionId + " update not allowed as loan transaction is linked to other transactions",
                    transactionId);
        }

        if (transactionToAdjust.isInterestRefund()) {
            throw new GeneralPlatformDomainRuleException("error.msg.loan.transaction.update.not.allowed",
                    "Interest refund transaction: " + transactionId + " cannot be reversed or adjusted directly", transactionId);
        }

        final LocalDate transactionDate = command.localDateValueOfParameterNamed("transactionDate");
        final BigDecimal transactionAmount = command.bigDecimalValueOfParameterNamed("transactionAmount");
        final ExternalId txnExternalId = transactionToAdjust.getExternalId();

        final boolean isAdjustCommand = (transactionAmount.compareTo(BigDecimal.ZERO) > 0);
        if (isAdjustCommand && !transactionToAdjust.isEditable()) {
            final String errorMessage = "Loan transaction: " + transactionId + " update not allowed as loan transaction is a "
                    + transactionToAdjust.getTypeOf().getCode();
            throw new InvalidLoanTransactionTypeException("transaction", "error.msg.loan.transaction.update.not.allowed", errorMessage);
        }

        final ExternalId reversalTxnExternalId = externalIdFactory.create();

        final Map<String, Object> changes = new LinkedHashMap<>();
        changes.put("transactionDate", command.localDateValueOfParameterNamed("transactionDate"));
        changes.put("transactionAmount", command.bigDecimalValueOfParameterNamed("transactionAmount"));
        changes.put("locale", command.locale());
        changes.put("dateFormat", command.dateFormat());
        changes.put("paymentTypeId", command.longValueOfParameterNamed("paymentTypeId"));

        final List<Long> existingTransactionIds = new ArrayList<>();
        final List<Long> existingReversedTransactionIds = new ArrayList<>();

        final Money transactionAmountAsMoney = Money.of(loan.getCurrency(), transactionAmount);
        final PaymentDetail paymentDetail = this.paymentDetailWritePlatformService.createPaymentDetail(command, changes);
        LoanTransaction newTransactionDetail = LoanTransaction.repaymentType(transactionToAdjust.getTypeOf(), loan.getOffice(),
                transactionAmountAsMoney, paymentDetail, transactionDate, txnExternalId, transactionToAdjust.getChargeRefundChargeType());
        if (transactionToAdjust.isInterestWaiver()) {
            Money unrecognizedIncome = transactionAmountAsMoney.zero();
            Money interestComponent = transactionAmountAsMoney;
            if (loan.isPeriodicAccrualAccountingEnabledOnLoanProduct()) {
                Money receivableInterest = loan.getReceivableInterest(transactionDate);
                if (transactionAmountAsMoney.isGreaterThan(receivableInterest)) {
                    interestComponent = receivableInterest;
                    unrecognizedIncome = transactionAmountAsMoney.minus(receivableInterest);
                }
            }
            newTransactionDetail = LoanTransaction.waiver(loan.getOffice(), loan, transactionAmountAsMoney, transactionDate,
                    interestComponent, unrecognizedIncome, txnExternalId);
        }
        if (transactionToAdjust.isChargesWaiver()) {
            for (LoanChargePaidBy paidBy: transactionToAdjust.getLoanChargesPaid()) {

                LoanCharge loanCharge = paidBy.getLoanCharge();
                MonetaryCurrency currency = loanCharge.getLoan().getCurrency();

                Integer installmentNumber = paidBy.getInstallmentNumber();

                loanCharge.undoWaive(currency, installmentNumber);
            }

        }

        LocalDate recalculateFrom = null;

        if (loan.isInterestBearingAndInterestRecalculationEnabled()) {
            recalculateFrom = DateUtils.isAfter(transactionToAdjust.getTransactionDate(), transactionDate) ? transactionDate
                    : transactionToAdjust.getTransactionDate();
        }

        ScheduleGeneratorDTO scheduleGeneratorDTO = this.loanUtilService.buildScheduleGeneratorDTO(loan, recalculateFrom);

        HolidayDetailDTO holidayDetailDTO = scheduleGeneratorDTO.getHolidayDetailDTO();
        if (loan.getLoanRepaymentScheduleDetail().getLoanScheduleType().equals(LoanScheduleType.CUMULATIVE)) {
            // validate cumulative
            loanTransactionValidator.validateActivityNotBeforeLastTransactionDate(loan, transactionToAdjust.getTransactionDate(),
                    LoanEvent.LOAN_REPAYMENT_OR_WAIVER);
        }
        // common validations
        loanTransactionValidator.validateRepaymentDateIsOnHoliday(newTransactionDetail.getTransactionDate(),
                holidayDetailDTO.isAllowTransactionsOnHoliday(), holidayDetailDTO.getHolidays());
        loanTransactionValidator.validateRepaymentDateIsOnNonWorkingDay(newTransactionDetail.getTransactionDate(),
                holidayDetailDTO.getWorkingDays(), holidayDetailDTO.isAllowTransactionsOnNonWorkingDay());

        adjustExistingTransaction(loan, newTransactionDetail, transactionToAdjust, existingTransactionIds, existingReversedTransactionIds,
                scheduleGeneratorDTO, reversalTxnExternalId);

        loanAccrualsProcessingService.reprocessExistingAccruals(loan);
        if (loan.isInterestBearingAndInterestRecalculationEnabled()) {
            loanAccrualsProcessingService.processIncomePostingAndAccruals(loan);
        }

        boolean thereIsNewTransaction = newTransactionDetail.isGreaterThanZero();
        if (thereIsNewTransaction) {
            if (paymentDetail != null) {
                this.paymentDetailWritePlatformService.persistPaymentDetail(paymentDetail);
            }
            this.loanTransactionRepository.saveAndFlush(newTransactionDetail);
        }

        saveAndFlushLoanWithDataIntegrityViolationChecks(loan);

        final String noteText = command.stringValueOfParameterNamed("note");
        if (StringUtils.isNotBlank(noteText)) {
            changes.put("note", noteText);
            Note note;
            /**
             * If a new transaction is not created, associate note with the transaction to be adjusted
             **/
            if (thereIsNewTransaction) {
                note = Note.loanTransactionNote(loan, newTransactionDetail, noteText);
            } else {
                note = Note.loanTransactionNote(loan, transactionToAdjust, noteText);
            }
            this.noteRepository.save(note);
        }

        loanLifecycleStateMachine.transition(LoanEvent.LOAN_ADJUST_TRANSACTION, loan);

        loanAccrualsProcessingService.processAccrualsOnInterestRecalculation(loan, loan.isInterestBearingAndInterestRecalculationEnabled(),
                false);

        this.loanAccountDomainService.setLoanDelinquencyTag(loan, DateUtils.getBusinessLocalDate());

        LoanAdjustTransactionBusinessEvent.Data eventData = new LoanAdjustTransactionBusinessEvent.Data(transactionToAdjust);
        if (newTransactionDetail.isRepaymentLikeType() && thereIsNewTransaction) {
            eventData.setNewTransactionDetail(newTransactionDetail);
        }
        Long entityId = transactionToAdjust.getId();
        ExternalId entityExternalId = transactionToAdjust.getExternalId();

        if (thereIsNewTransaction) {
            entityId = newTransactionDetail.getId();
            entityExternalId = newTransactionDetail.getExternalId();
        }
        businessEventNotifierService.notifyPostBusinessEvent(new LoanBalanceChangedBusinessEvent(loan));
        businessEventNotifierService.notifyPostBusinessEvent(new LoanAdjustTransactionBusinessEvent(eventData));

        postJournalEntries(loan, existingTransactionIds, existingReversedTransactionIds);
        loanAccrualTransactionBusinessEventService.raiseBusinessEventForAccrualTransactions(loan, existingTransactionIds);

        return new CommandProcessingResultBuilder() //
                .withCommandId(command.commandId()) //
                .withEntityId(entityId) //
                .withEntityExternalId(entityExternalId) //
                .withOfficeId(loan.getOfficeId()) //
                .withClientId(loan.getClientId()) //
                .withGroupId(loan.getGroupId()) //
                .withLoanId(loan.getId()) //
                .with(changes).build();
    }

    public void adjustExistingTransaction(final Loan loan, final LoanTransaction newTransactionDetail, final LoanTransaction transactionForAdjustment,
                                                              final List<Long> existingTransactionIds, final List<Long> existingReversedTransactionIds,
                                                              final ScheduleGeneratorDTO scheduleGeneratorDTO, final ExternalId reversalExternalId) {


        existingTransactionIds.addAll(loan.findExistingTransactionIds());
        existingReversedTransactionIds.addAll(loan.findExistingReversedTransactionIds());

        loanTransactionValidator.validateActivityNotBeforeClientOrGroupTransferDate(loan, LoanEvent.LOAN_REPAYMENT_OR_WAIVER,
                transactionForAdjustment.getTransactionDate());

        if (!transactionForAdjustment.isAccrualRelated() && transactionForAdjustment.isNotRepaymentLikeType()
                && transactionForAdjustment.isNotWaiver() && transactionForAdjustment.isNotCreditBalanceRefund()
                ) {
            final String errorMessage = "Only (non-reversed) transactions of type repayment, waiver, accrual, credit balance refund, capitalized income, capitalized income adjustment, buy down fee or buy down fee adjustment can be adjusted.";
            throw new InvalidLoanTransactionTypeException("transaction",
                    "adjustment.is.only.allowed.to.repayment.or.waiver.or.creditbalancerefund.or.capitalizedIncome.or.capitalizedIncomeAdjustment.or.buyDownFee.or.buyDownFeeAdjustment.transactions",
                    errorMessage);
        }


        loanChargeValidator.validateRepaymentTypeTransactionNotBeforeAChargeRefund(transactionForAdjustment.getLoan(),
                transactionForAdjustment, "reversed");
        transactionForAdjustment.reverse(reversalExternalId);
        transactionForAdjustment.manuallyAdjustedOrReversed();

        if (transactionForAdjustment.getTypeOf().equals(LoanTransactionType.MERCHANT_ISSUED_REFUND)
                || transactionForAdjustment.getTypeOf().equals(LoanTransactionType.PAYOUT_REFUND)) {
            loan.getLoanTransactions().stream() //
                    .filter(LoanTransaction::isNotReversed)
                    .filter(loanTransaction -> loanTransaction.getLoanTransactionRelations().stream()
                            .anyMatch(relation -> relation.getRelationType().equals(LoanTransactionRelationTypeEnum.RELATED)
                                    && relation.getToTransaction().getId().equals(transactionForAdjustment.getId())))
                    .forEach(loanTransaction -> {
                        loanChargeValidator.validateRepaymentTypeTransactionNotBeforeAChargeRefund(loanTransaction.getLoan(),
                                loanTransaction, "reversed");
                        loanTransaction.reverse();
                        loanTransaction.manuallyAdjustedOrReversed();
                        LoanAdjustTransactionBusinessEvent.Data eventData = new LoanAdjustTransactionBusinessEvent.Data(loanTransaction);
                        businessEventNotifierService.notifyPostBusinessEvent(new LoanAdjustTransactionBusinessEvent(eventData));
                    });
        }

        if (loan.isClosedWrittenOff()) {
            // find write off transaction and reverse it
            final LoanTransaction writeOffTransaction = loan.findWriteOffTransaction();
            loanChargeValidator.validateRepaymentTypeTransactionNotBeforeAChargeRefund(writeOffTransaction.getLoan(), writeOffTransaction,
                    "reversed");
            writeOffTransaction.reverse();
        }


        if (newTransactionDetail.isRepaymentLikeType() || newTransactionDetail.isInterestWaiver()) {
            loanDownPaymentHandlerService.handleRepaymentOrRecoveryOrWaiverTransaction(loan,newTransactionDetail,loanLifecycleStateMachine, transactionForAdjustment, scheduleGeneratorDTO);
        }

    }

    private Loan saveAndFlushLoanWithDataIntegrityViolationChecks(final Loan loan) {
        /*
         * Due to the "saveAndFlushLoanWithDataIntegrityViolationChecks" method the loan is saved and flushed in the
         * middle of the transaction. EclipseLink is in some situations are saving inconsistently the newly created
         * associations, like the newly created repayment schedule installments. The save and flush cannot be removed
         * safely till any native queries are used as part of this transaction either. See:
         * this.loanAccountDomainService.recalculateAccruals(loan);
         */
        try {
            loanRepaymentScheduleInstallmentRepository.saveAll(loan.getRepaymentScheduleInstallments());
            return this.loanRepositoryWrapper.saveAndFlush(loan);
        } catch (final JpaSystemException | DataIntegrityViolationException e) {
            final Throwable realCause = e.getCause();
            final List<ApiParameterError> dataValidationErrors = new ArrayList<>();
            final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors).resource("loan.transaction");
            if (realCause.getMessage().toLowerCase().contains("external_id_unique")) {
                baseDataValidator.reset().parameter(LoanApiConstants.externalIdParameterName).failWithCode("value.must.be.unique");
            }
            if (!dataValidationErrors.isEmpty()) {
                throw new PlatformApiDataValidationException("validation.msg.validation.errors.exist", "Validation errors exist.",
                        dataValidationErrors, e);
            }
            throw e;
        }
    }

    private void checkClientOrGroupActive(final Loan loan) {
        final Client client = loan.client();
        if (client != null && client.isNotActive()) {
            throw new ClientNotActiveException(client.getId());
        }
        final Group group = loan.group();
        if (group != null && group.isNotActive()) {
            throw new GroupNotActiveException(group.getId());
        }
    }

    private void postJournalEntries(final Loan loan, final List<Long> existingTransactionIds,
                                    final List<Long> existingReversedTransactionIds) {

        final MonetaryCurrency currency = loan.getCurrency();
        boolean isAccountTransfer = false;
        List<Map<String, Object>> accountingBridgeData = new ArrayList<>();
        if (loan.isChargedOff()) {
            accountingBridgeData = loan.deriveAccountingBridgeDataForChargeOff(currency.getCode(), existingTransactionIds,
                    existingReversedTransactionIds, isAccountTransfer);
        } else {
            accountingBridgeData.add(loan.deriveAccountingBridgeData(currency.getCode(), existingTransactionIds,
                    existingReversedTransactionIds, isAccountTransfer));
        }
        for (Map<String, Object> accountingData : accountingBridgeData) {
            this.journalEntryWritePlatformService.createJournalEntriesForLoan(accountingData);
        }

    }
}
