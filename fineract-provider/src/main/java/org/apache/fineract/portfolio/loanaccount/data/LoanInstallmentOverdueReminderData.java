package org.apache.fineract.portfolio.loanaccount.data;

import java.math.BigDecimal;
import java.time.LocalDate;

public record LoanInstallmentOverdueReminderData(
        Long loanId,
        Long clientId,
        Long installmentId,
        Integer installmentNumber,
        LocalDate dueDate,
        Integer overdueDays,
        BigDecimal principal,
        BigDecimal interest,
        BigDecimal fee,
        BigDecimal penalty,
        BigDecimal totalDue,
        String mobileNo,
        String clientName
) {
}
