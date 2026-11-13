/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.fineract.infrastructure.creditbureau.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.apache.avro.data.Json;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.domain.AbstractPersistableCustom;
import org.apache.fineract.portfolio.loanproduct.domain.LoanProduct;

import java.util.HashMap;
import java.util.Map;

@SuppressWarnings({ "MemberName" })
@Entity
@Table(name = "m_creditbureau_loanproduct_mapping")
@Getter
@Setter
@NoArgsConstructor
@Accessors(chain = true)
public class CreditBureauLoanProductMapping extends AbstractPersistableCustom<Long> {

    @Column(name = "is_creditcheck_mandatory")
    private boolean creditCheckMandatory;

    @Column(name = "skip_creditcheck_in_failure")
    private boolean skipCreditCheckInFailure;

    @Column(name = "stale_period")
    private int stalePeriod;

    @Column(name = "is_active")
    private boolean active;

    @ManyToOne
    @JoinColumn(name = "organisation_creditbureau_id",  nullable = false)
    private OrganisationCreditBureau organisation_creditbureau;

    @OneToOne
    @JoinColumn(name = "loan_product_id", nullable = false)
    private LoanProduct loanProduct;

    public static CreditBureauLoanProductMapping fromJson(final JsonCommand command, OrganisationCreditBureau organisation_creditbureau,
            LoanProduct loanProduct) {
        Boolean isCreditCheckMandatory = false;
        Boolean skipCreditCheckInFailure = false;
        Integer stalePeriod = -1;
        Boolean isActive = false;
        if ((Boolean) command.booleanPrimitiveValueOfParameterNamed("isCreditcheckMandatory") != null) {
            isCreditCheckMandatory = command.booleanPrimitiveValueOfParameterNamed("isCreditcheckMandatory");
        }

        if ((Boolean) command.booleanPrimitiveValueOfParameterNamed("skipCreditcheckInFailure") != null) {
            skipCreditCheckInFailure = command.booleanPrimitiveValueOfParameterNamed("skipCreditcheckInFailure");
        }

        if (command.integerValueOfParameterNamed("stalePeriod") != null) {
            stalePeriod = command.integerValueOfParameterNamed("stalePeriod");
        }

        if ((Boolean) command.booleanPrimitiveValueOfParameterNamed("isActive")) {
            isActive = command.booleanPrimitiveValueOfParameterNamed("isActive");
        }

        return new CreditBureauLoanProductMapping().setCreditCheckMandatory(isCreditCheckMandatory)
                .setSkipCreditCheckInFailure(skipCreditCheckInFailure).setStalePeriod(stalePeriod).setActive(isActive)
                .setOrganisation_creditbureau(organisation_creditbureau).setLoanProduct(loanProduct);

    }

    public Map<String, Object> changes(final JsonCommand command) {
        final Map<String, Object> changes = new HashMap<>(4);

        if (command.isChangeInBooleanParameterNamed("isCreditcheckMandatory", this.creditCheckMandatory)) {
            final boolean newValue = command.booleanPrimitiveValueOfParameterNamed("isCreditcheckMandatory");
            this.creditCheckMandatory = newValue;
            changes.put("creditCheckMandatory", newValue);
        }

        if (command.isChangeInBooleanParameterNamed("skipCreditcheckInFailure", this.skipCreditCheckInFailure)) {
            final boolean newValue = command.booleanPrimitiveValueOfParameterNamed("skipCreditcheckInFailure");
            this.skipCreditCheckInFailure = newValue;
            changes.put("skipCreditcheckInFailure", newValue);
        }

        if (command.isChangeInIntegerParameterNamed("stalePeriod", this.stalePeriod)) {
            final int newValue = command.integerValueOfParameterNamed("stalePeriod");
            this.stalePeriod = newValue;
            changes.put("stalePeriod", newValue);
        }

        if (command.isChangeInBooleanParameterNamed("isActive", this.active)) {
            final boolean newValue = command.booleanPrimitiveValueOfParameterNamed("isActive");
            this.active = newValue;
            changes.put("active", newValue);
        }

        return changes;
    }
}
