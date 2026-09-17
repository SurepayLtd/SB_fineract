package org.apache.fineract.portfolio.loanaccount.loanschedule.service;

import org.apache.fineract.portfolio.loanaccount.data.LoanInstallmentReminderData;
import org.apache.fineract.portfolio.loanaccount.data.LoanInstallmentReminderPartition;

import java.time.LocalDate;
import java.util.List;

public interface LoanInstallmentReminderReadService {

    List<LoanInstallmentReminderPartition> retrieveDuePartitions(int partitionSize);
    List<LoanInstallmentReminderData> retrieveDuePage(Long minAccountKey, Long maxAccountKey, LocalDate afterDueDate, Long afterId, int limit);
}
