package org.apache.fineract.portfolio.loanaccount.jobs.paymentdueremider;

import org.springframework.batch.core.step.skip.SkipPolicy;

/**
 * Skips any installment whose execution failed, without a limit.
 *
 **/
public class LoanInstallmentSkipPolicy implements SkipPolicy {

    @Override
    public boolean shouldSkip(final Throwable t, final long skipCount) {
        return t instanceof Exception;
    }
}
