package org.apache.fineract.portfolio.savings.data;

import java.time.LocalDate;

public record SavingsDormancyReminderData(
        Long savingsId,
        Long clientId,
        String accountNumber,
        String mobileNo,
        String clientName,
        LocalDate lastTransactionDate,
        Long inactiveDays,
        Integer reminderDays
) {}
