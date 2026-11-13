package org.apache.fineract.portfolio.crb.service;

import org.apache.fineract.portfolio.crb.data.CrbCheckData;

import java.util.List;

public interface CrbCheckReadServicePlatform {

    List<CrbCheckData> retrieveClientChecks(Long clientId);
    List<CrbCheckData> retrieveCreditChecks(Long loanId, Integer type);
    CrbCheckData retrieveTypes();
    List<CrbCheckData> retrieveKycChecks(Long clientId);

}
