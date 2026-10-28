package org.apache.fineract.portfolio.crb.service;

import org.apache.fineract.portfolio.crb.data.CrbCheckData;

import java.util.List;

public interface CrbCheckReadServicePlatform {

    List<CrbCheckData> retrieveChecks(Long clientId);
    CrbCheckData retrieveTypes();

}
