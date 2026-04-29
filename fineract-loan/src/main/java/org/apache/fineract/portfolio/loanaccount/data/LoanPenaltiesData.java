package org.apache.fineract.portfolio.loanaccount.data;

import lombok.Getter;
import org.apache.fineract.organisation.monetary.data.CurrencyData;

import java.math.BigDecimal;

@Getter
public final class LoanPenaltiesData {

    private final Long loanId;

    private final BigDecimal totalPenaltiesDerived;

    private final BigDecimal totalPenaltiesPaid;

    private final BigDecimal totalPenaltiesWaived;

    private final BigDecimal totalPenaltiesWrittenOff;

    private final BigDecimal totalPenaltiesOutstanding;

    private final CurrencyData currency;


    public LoanPenaltiesData(Long loanId, BigDecimal totalPenaltiesDerived, BigDecimal totalPenaltiesPaid, BigDecimal totalPenaltiesWaived,
                             BigDecimal totalPenaltiesWrittenOff, BigDecimal totalPenaltiesOutstanding, CurrencyData currency) {
        this.loanId = loanId;
        this.totalPenaltiesDerived = totalPenaltiesDerived;
        this.totalPenaltiesPaid = totalPenaltiesPaid;
        this.totalPenaltiesWaived = totalPenaltiesWaived;
        this.totalPenaltiesWrittenOff = totalPenaltiesWrittenOff;
        this.totalPenaltiesOutstanding = totalPenaltiesOutstanding;
        this.currency = currency;
    }
}
