package org.apache.fineract.portfolio.loanaccount.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.portfolio.loanaccount.data.LoanAccrualData;
import org.apache.fineract.portfolio.loanaccount.data.LoanAccrualPartition;
import org.apache.fineract.portfolio.loanaccount.domain.LoanRepository;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class LoanAccrualBatchReadServiceImpl implements LoanAccrualBatchReadService{

    private final LoanRepository loanRepository;

    @Override
    public List<LoanAccrualPartition> retrieveLoanAccrualPartitions(int partitionSize, Integer accountingType, LocalDate tillDate, boolean futureCharges) {
        final List<LoanAccrualPartition> partitions = new ArrayList<>();

        Long afterLoanId = 0L;
        long partitionNumber = 0L;

        while (true) {

            final Pageable pageable = PageRequest.of(0, partitionSize);

            final List<Long> loanIds = loanRepository.findLoanIdsForPeriodicAccrualPartition(accountingType, tillDate, futureCharges, afterLoanId, pageable);

            if (loanIds.isEmpty()) {
                break;
            }

            final Long minAccountKey = loanIds.get(0);
            final Long maxAccountKey = loanIds.get(loanIds.size()-1);

            partitionNumber++;

            partitions.add(new LoanAccrualPartition(minAccountKey, maxAccountKey, partitionNumber, (long) loanIds.size()));

            afterLoanId = maxAccountKey;

            if (loanIds.size() < partitionSize) {
                break;
            }
        }

        log.info("Created {} loan accrual partitions using partition size {}", partitions.size(), partitionSize);

        return partitions;
    }

    @Override
    public List<LoanAccrualData> retrieveLoanAccrualPage(Long minAccountKey, Long maxAccountKey, Long afterLoanId, int pageSize, Integer accountingType, LocalDate tillDate, boolean futureCharges) {
        final Pageable pageable = PageRequest.of(0, pageSize);

        List<Long> loanIds = loanRepository.findLoanIdsForPeriodicAccrualPage(accountingType, tillDate, futureCharges, minAccountKey, maxAccountKey, afterLoanId, pageable);

       return loanIds.stream()
                .map(LoanAccrualData::new)
                .toList();
    }

}
