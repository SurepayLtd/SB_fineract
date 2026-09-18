package org.apache.fineract.portfolio.loanaccount.loanschedule.service;

import org.apache.fineract.portfolio.loanaccount.data.LoanInstallmentOverdueReminderData;
import org.apache.fineract.portfolio.loanaccount.data.LoanInstallmentReminderData;
import org.apache.fineract.portfolio.loanaccount.data.JobPartition;
import org.apache.fineract.portfolio.savings.data.SavingsDormancyReminderData;

import java.time.LocalDate;
import java.util.List;

public interface BatchSmSReadService {

    List<JobPartition> retrieveLoanDuePartitions(int partitionSize);
    List<LoanInstallmentReminderData> retrieveLoanDuePage(Long minAccountKey, Long maxAccountKey, LocalDate afterDueDate, Long afterId, int limit);
    List<JobPartition> retrieveDormantPartitions(int partitionSize);
    List<SavingsDormancyReminderData> retrieveDormancyPage(Long minAccountKey, Long maxAccountKey, Integer afterReminderDays, Long afterSavingsId, int limit);
    List<JobPartition> retrieveLoanOverDuePartitions(int partitionSize);
    List<LoanInstallmentOverdueReminderData> retrieveLoanOverDuePage(Long minAccountKey, Long maxAccountKey, LocalDate afterDueDate, Long afterId, int limit);
}
