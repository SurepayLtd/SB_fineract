package org.apache.fineract.portfolio.loanaccount.data;


/**
 * @param minAccountKey lowest source-account key in the partition (inclusive)
 * @param maxAccountKey highest source-account key in the partition (inclusive)
 * @param pageNumber zero-based ordinal of the partition, used to build its name
 * @param partitionCount number of due payments/accounts/savings the partition covers, for logging only
 */
public record JobPartition(
        Long minAccountKey,
        Long maxAccountKey,
        Long pageNumber,
        Long partitionCount
) {
}
