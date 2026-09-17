package org.apache.fineract.portfolio.loanaccount.data;


/**
 * @param minAccountKey lowest source-account key in the partition (inclusive)
 * @param maxAccountKey highest source-account key in the partition (inclusive)
 * @param pageNumber zero-based ordinal of the partition, used to build its name
 * @param installmentCount number of due payments the partition covers, for logging only
 */
public record LoanInstallmentReminderPartition(
        Long minAccountKey,
        Long maxAccountKey,
        Long pageNumber,
        Long installmentCount
) {
}
