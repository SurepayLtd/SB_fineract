/**
 * Copyright since 2026 Mifos Initiative
 *
 * <p>This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy
 * of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.apache.fineract.selfservice.kyc.service;

import org.apache.fineract.selfservice.security.data.SelfServiceAuthenticatedUserKycData;
import org.springframework.stereotype.Service;

@Service
public class KycFeatureStatusReadService {

  public KycFeatureStatusReadService() {
  }

  public SelfServiceAuthenticatedUserKycData getKycFeatureStatus(final Long clientId) {
    return defaultData();
  }

  public SelfServiceAuthenticatedUserKycData getApprovedKycFeatureStatus(final Long clientId) {
    return defaultData();
  }

  private SelfServiceAuthenticatedUserKycData defaultData() {
    return new SelfServiceAuthenticatedUserKycData(
        Boolean.TRUE, Boolean.TRUE, Boolean.TRUE, Boolean.TRUE, "Approved");
  }
}
