package org.apache.fineract.portfolio.loanproduct.data;


import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Data
@NoArgsConstructor
@Accessors(chain = true)
public class CreditBureauLoanProductMappingData {

    private long creditbureauLoanProductMappingId;

    private long organisationCreditBureauId;

    private String alias;

    private String creditbureauSummary;

    private String loanProductName;

    private long loanProductId;

    private boolean isCreditCheckMandatory;

    private boolean skipCrediCheckInFailure;

    private long stalePeriod;

    private boolean active;

    public static CreditBureauLoanProductMappingData instance(final long creditbureauLoanProductMappingId,
                                                              final long organisationCreditBureauId, final String alias, final String creditbureauSummary, final String loanProductName,
                                                              final long loanProductId, final boolean isCreditCheckMandatory, final boolean skipCrediCheckInFailure, final long stalePeriod,
                                                              final boolean active) {
        return new CreditBureauLoanProductMappingData().setCreditbureauLoanProductMappingId(creditbureauLoanProductMappingId)
                .setOrganisationCreditBureauId(organisationCreditBureauId).setAlias(alias).setCreditbureauSummary(creditbureauSummary)
                .setLoanProductName(loanProductName).setLoanProductId(loanProductId).setCreditCheckMandatory(isCreditCheckMandatory)
                .setSkipCrediCheckInFailure(skipCrediCheckInFailure).setStalePeriod(stalePeriod).setActive(active);
    }
}
