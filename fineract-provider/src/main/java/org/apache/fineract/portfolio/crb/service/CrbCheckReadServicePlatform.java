package org.apache.fineract.portfolio.crb.service;

import org.apache.fineract.portfolio.crb.data.CrbCheckData;

import java.util.List;

public interface CrbCheckReadServicePlatform {

    List<CrbCheckData> retrieveChecks(Long clientId);
    List<CrbCheckData> retrieveCreditChecks(Long clientId, Integer type);
    CrbCheckData retrieveTypes();

}
