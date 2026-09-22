package org.apache.fineract.portfolio.loanaccount.data;

public record LoanAccrualPartition(
        Long minAccountKey,
        Long maxAccountKey,
        Long pageNumber,
        Long partitionCount
) {
}
