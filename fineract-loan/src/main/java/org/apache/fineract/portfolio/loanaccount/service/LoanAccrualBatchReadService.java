package org.apache.fineract.portfolio.loanaccount.service;

import org.apache.fineract.portfolio.loanaccount.data.LoanAccrualData;
import org.apache.fineract.portfolio.loanaccount.data.LoanAccrualPartition;

import java.time.LocalDate;
import java.util.List;

public interface LoanAccrualBatchReadService {

    List<LoanAccrualPartition> retrieveLoanAccrualPartitions(int partitionSize, Integer accountingType, LocalDate tillDate, boolean futureCharges);

    List<LoanAccrualData> retrieveLoanAccrualPage(Long minAccountKey, Long maxAccountKey, Long afterLoanId, int pageSize, Integer accountingType,
            LocalDate tillDate, boolean futureCharges);
}
