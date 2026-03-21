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
package org.apache.fineract.useradministration.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import org.apache.fineract.infrastructure.core.exception.PlatformApiDataValidationException;
import org.apache.fineract.infrastructure.core.serialization.FromJsonHelper;
import org.apache.fineract.useradministration.domain.AppUser;
import org.apache.fineract.useradministration.domain.PasswordValidationPolicy;
import org.apache.fineract.useradministration.domain.PasswordValidationPolicyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserDataValidatorTest {

    @Mock
    private PasswordValidationPolicyRepository passwordValidationPolicyRepository;

    @Mock
    private PasswordValidationPolicy passwordValidationPolicy;

    @Mock
    private AppUser authenticatedUser;

    private UserDataValidator validator;
    private FromJsonHelper fromJsonHelper;

    @BeforeEach
    void setUp() {
        fromJsonHelper = new FromJsonHelper();
        validator = new UserDataValidator(fromJsonHelper, passwordValidationPolicyRepository);
    }

    @Test
    void shouldAcceptBypassTwoFactorTrueInCreateRequest() {
        // Given
        when(passwordValidationPolicyRepository.findActivePasswordValidationPolicy()).thenReturn(passwordValidationPolicy);
        when(passwordValidationPolicy.getRegex()).thenReturn("^.{8,}$");
        when(passwordValidationPolicy.getDescription()).thenReturn("Password must be at least 8 characters");

        String json = """
                {
                    "username": "serviceuser",
                    "firstname": "Service",
                    "lastname": "User",
                    "email": "service@example.com",
                    "officeId": 1,
                    "roles": ["1"],
                    "sendPasswordToEmail": false,
                    "password": "Password123!",
                    "repeatPassword": "Password123!",
                    "bypassTwoFactor": true
                }
                """;

        // When/Then
        assertDoesNotThrow(() -> validator.validateForCreate(json));
    }

    @Test
    void shouldAcceptBypassTwoFactorFalseInCreateRequest() {
        // Given
        when(passwordValidationPolicyRepository.findActivePasswordValidationPolicy()).thenReturn(passwordValidationPolicy);
        when(passwordValidationPolicy.getRegex()).thenReturn("^.{8,}$");
        when(passwordValidationPolicy.getDescription()).thenReturn("Password must be at least 8 characters");

        String json = """
                {
                    "username": "normaluser",
                    "firstname": "Normal",
                    "lastname": "User",
                    "email": "normal@example.com",
                    "officeId": 1,
                    "roles": ["1"],
                    "sendPasswordToEmail": false,
                    "password": "Password123!",
                    "repeatPassword": "Password123!",
                    "bypassTwoFactor": false
                }
                """;

        // When/Then
        assertDoesNotThrow(() -> validator.validateForCreate(json));
    }

    @Test
    void shouldAcceptBypassTwoFactorInUpdateRequest() {
        // Given
        when(authenticatedUser.hasAnyPermission("ALL_FUNCTIONS", "UPDATE_USER")).thenReturn(true);

        String json = """
                {
                    "bypassTwoFactor": true
                }
                """;

        // When/Then
        assertDoesNotThrow(() -> validator.validateForUpdate(json, authenticatedUser));
    }

    @Test
    void shouldRejectInvalidBypassTwoFactorValueInCreateRequest() {
        // Given
        when(passwordValidationPolicyRepository.findActivePasswordValidationPolicy()).thenReturn(passwordValidationPolicy);
        when(passwordValidationPolicy.getRegex()).thenReturn("^.{8,}$");
        when(passwordValidationPolicy.getDescription()).thenReturn("Password must be at least 8 characters");

        String json = """
                {
                    "username": "serviceuser",
                    "firstname": "Service",
                    "lastname": "User",
                    "email": "service@example.com",
                    "officeId": 1,
                    "roles": ["1"],
                    "sendPasswordToEmail": false,
                    "password": "Password123!",
                    "repeatPassword": "Password123!",
                    "bypassTwoFactor": "invalid"
                }
                """;

        // When/Then
        assertThrows(PlatformApiDataValidationException.class, () -> validator.validateForCreate(json));
    }
}

