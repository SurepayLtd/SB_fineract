package org.apache.fineract.portfolio.loanaccount.data;

import java.math.BigDecimal;
import java.time.LocalDate;

public record LoanInstallmentReminderData(
        Long loanId,
        Long clientId,
        Long installmentId,
        Integer installmentNumber,
        LocalDate dueDate,
        Integer reminderDays,
        BigDecimal principal,
        BigDecimal interest,
        BigDecimal fee,
        BigDecimal penalty,
        BigDecimal totalDue,
        BigDecimal loanBalance,
        String mobileNo,
        String clientName
) {
}
