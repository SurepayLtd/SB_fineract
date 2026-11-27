package org.apache.fineract.infrastructure.event.business.domain.workingcapitalloan.transaction;

import org.apache.fineract.portfolio.workingcapitalloan.domain.WorkingCapitalLoanTransaction;

public class WorkingCapitalLoanDiscountFeeAmortizationAdjustmentTransactionBusinessEvent extends WorkingCapitalLoanTransactionBusinessEvent {

    private static final String TYPE = "WorkingCapitalLoanDiscountFeeAmortizationAdjustmentTransactionBusinessEvent";

    public WorkingCapitalLoanDiscountFeeAmortizationAdjustmentTransactionBusinessEvent(final WorkingCapitalLoanTransaction value,
                                                                                       final Long aggregateRootId) {
        super(value, aggregateRootId);
    }

    @Override
    public String getType() {
        return TYPE;
    }
}

