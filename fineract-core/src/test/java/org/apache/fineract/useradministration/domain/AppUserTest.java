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
package org.apache.fineract.useradministration.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.security.service.PlatformPasswordEncoder;
import org.apache.fineract.organisation.office.domain.Office;
import org.apache.fineract.useradministration.service.AppUserConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.User;

@ExtendWith(MockitoExtension.class)
class AppUserTest {

    @Mock
    private Office office;

    @Mock
    private PlatformPasswordEncoder passwordEncoder;

    private AppUser appUser;

    @BeforeEach
    void setUp() {
        Set<Role> roles = new HashSet<>();
        User springUser = new User("testuser", "password123", true, true, true, true, new HashSet<>());
        appUser = new AppUser(office, springUser, roles, "test@example.com", "Test", "User", null, false, false, null,
                false, false);
    }

    @Test
    void shouldReturnFalseForBypassTwoFactorByDefault() {
        // Given - a new user with default bypassTwoFactor

        // Then
        assertFalse(appUser.isBypassTwoFactor());
    }

    @Test
    void shouldReturnTrueForBypassTwoFactorWhenSetTrue() {
        // Given
        Set<Role> roles = new HashSet<>();
        User springUser = new User("testuser", "password123", true, true, true, true, new HashSet<>());
        AppUser userWithBypass = new AppUser(office, springUser, roles, "test@example.com", "Test", "User", null, false,
                false, null, false, true);

        // Then
        assertTrue(userWithBypass.isBypassTwoFactor());
    }

    @Test
    void shouldUpdateBypassTwoFactorFromFalseToTrue() {
        // Given
        JsonCommand command = mock(JsonCommand.class);
        when(command.hasParameter(AppUserConstants.BYPASS_TWO_FACTOR)).thenReturn(true);
        when(command.isChangeInBooleanParameterNamed(AppUserConstants.BYPASS_TWO_FACTOR, false)).thenReturn(true);
        when(command.booleanPrimitiveValueOfParameterNamed(AppUserConstants.BYPASS_TWO_FACTOR)).thenReturn(true);

        // When
        Map<String, Object> changes = appUser.update(command, passwordEncoder, null);

        // Then
        assertTrue(changes.containsKey(AppUserConstants.BYPASS_TWO_FACTOR));
        assertEquals(true, changes.get(AppUserConstants.BYPASS_TWO_FACTOR));
        assertTrue(appUser.isBypassTwoFactor());
    }

    @Test
    void shouldNotUpdateBypassTwoFactorWhenValueUnchanged() {
        // Given - user with bypassTwoFactor = false
        JsonCommand command = mock(JsonCommand.class);
        when(command.hasParameter(AppUserConstants.BYPASS_TWO_FACTOR)).thenReturn(true);
        when(command.isChangeInBooleanParameterNamed(AppUserConstants.BYPASS_TWO_FACTOR, false)).thenReturn(false);

        // When
        Map<String, Object> changes = appUser.update(command, passwordEncoder, null);

        // Then
        assertFalse(changes.containsKey(AppUserConstants.BYPASS_TWO_FACTOR));
        assertFalse(appUser.isBypassTwoFactor());
    }

    @Test
    void shouldNotIncludeBypassTwoFactorInChangesWhenNotProvided() {
        // Given
        JsonCommand command = mock(JsonCommand.class);
        when(command.hasParameter(AppUserConstants.BYPASS_TWO_FACTOR)).thenReturn(false);

        // When
        Map<String, Object> changes = appUser.update(command, passwordEncoder, null);

        // Then
        assertFalse(changes.containsKey(AppUserConstants.BYPASS_TWO_FACTOR));
    }
}

