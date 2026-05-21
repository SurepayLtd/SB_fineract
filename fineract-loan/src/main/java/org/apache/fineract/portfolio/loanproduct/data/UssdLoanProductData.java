package org.apache.fineract.portfolio.loanproduct.data;

import lombok.Getter;
import org.apache.fineract.infrastructure.core.data.EnumOptionData;
import org.apache.fineract.organisation.monetary.data.CurrencyData;
import org.apache.fineract.portfolio.charge.data.ChargeData;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;

@Getter
public class UssdLoanProductData implements Serializable {

    private final Long id;
    private final String name;
    private final String shortName;
    private final String description;
    private final LocalDate startDate;
    private final LocalDate endDate;
    private final String status;

    // terms
    private final CurrencyData currency;
    private final BigDecimal principal;
    private final BigDecimal minPrincipal;
    private final BigDecimal maxPrincipal;
    private final Integer numberOfRepayments;
    private final Integer minNumberOfRepayments;
    private final Integer maxNumberOfRepayments;
    private final Integer repaymentEvery;
    private final EnumOptionData repaymentFrequencyType;
    private final BigDecimal interestRatePerPeriod;
    private final BigDecimal minInterestRatePerPeriod;
    private final BigDecimal maxInterestRatePerPeriod;
    private final EnumOptionData interestRateFrequencyType;
    private final BigDecimal annualInterestRate;
    // settings
    private final EnumOptionData amortizationType;
    private final EnumOptionData interestType;
    private final EnumOptionData interestCalculationPeriodType;
    private final boolean isEqualAmortization;
    private final boolean interestRecognitionOnDisbursementDate;
    private final String transactionProcessingStrategyCode;
    private final boolean enableInstallmentLevelDelinquency;

    //charges
    private final Collection<ChargeData> charges;


    public UssdLoanProductData(Long id, String name, String shortName, String description, LocalDate startDate, LocalDate endDate,
                               String status, CurrencyData currency, BigDecimal principal, BigDecimal minPrincipal, BigDecimal maxPrincipal, Integer numberOfRepayments, Integer minNumberOfRepayments, Integer maxNumberOfRepayments, Integer repaymentEvery, EnumOptionData repaymentFrequencyType, BigDecimal interestRatePerPeriod, BigDecimal minInterestRatePerPeriod, BigDecimal maxInterestRatePerPeriod, EnumOptionData interestRateFrequencyType, BigDecimal annualInterestRate, EnumOptionData amortizationType, EnumOptionData interestType, EnumOptionData interestCalculationPeriodType, boolean isEqualAmortization, boolean interestRecognitionOnDisbursementDate, String transactionProcessingStrategyCode,
                               boolean enableInstallmentLevelDelinquency, Collection<ChargeData> charges) {
        this.id = id;
        this.name = name;
        this.shortName = shortName;
        this.description = description;
        this.startDate = startDate;
        this.endDate = endDate;
        this.status = status;
        this.currency = currency;
        this.principal = principal;
        this.minPrincipal = minPrincipal;
        this.maxPrincipal = maxPrincipal;
        this.numberOfRepayments = numberOfRepayments;
        this.minNumberOfRepayments = minNumberOfRepayments;
        this.maxNumberOfRepayments = maxNumberOfRepayments;
        this.repaymentEvery = repaymentEvery;
        this.repaymentFrequencyType = repaymentFrequencyType;
        this.interestRatePerPeriod = interestRatePerPeriod;
        this.minInterestRatePerPeriod = minInterestRatePerPeriod;
        this.maxInterestRatePerPeriod = maxInterestRatePerPeriod;
        this.interestRateFrequencyType = interestRateFrequencyType;
        this.annualInterestRate = annualInterestRate;
        this.amortizationType = amortizationType;
        this.interestType = interestType;
        this.interestCalculationPeriodType = interestCalculationPeriodType;
        this.isEqualAmortization = isEqualAmortization;
        this.interestRecognitionOnDisbursementDate = interestRecognitionOnDisbursementDate;
        this.transactionProcessingStrategyCode = transactionProcessingStrategyCode;
        this.enableInstallmentLevelDelinquency = enableInstallmentLevelDelinquency;
        this.charges = charges;
    }
}
