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
package org.apache.fineract.infrastructure.security.filter;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.Collection;
import org.apache.fineract.infrastructure.core.domain.FineractPlatformTenant;
import org.apache.fineract.infrastructure.core.service.ThreadLocalContextUtil;
import org.apache.fineract.infrastructure.security.constants.TwoFactorConstants;
import org.apache.fineract.infrastructure.security.domain.TFAccessToken;
import org.apache.fineract.infrastructure.security.service.TwoFactorService;
import org.apache.fineract.useradministration.domain.AppUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class TwoFactorAuthenticationFilterTest {

    @Mock
    private TwoFactorService twoFactorService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @Mock
    private AppUser appUser;

    private TwoFactorAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        filter = new TwoFactorAuthenticationFilter(twoFactorService);
        SecurityContextHolder.setContext(securityContext);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        ThreadLocalContextUtil.reset();
    }

    @Test
    void shouldBypassTwoFactorWhenUserHasBypassTwoFactorFlagTrue() throws Exception {
        // Given
        FineractPlatformTenant tenant = new FineractPlatformTenant(1L, "default", "Default", "UTC", null, true);
        ThreadLocalContextUtil.setTenant(tenant);

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(appUser);
        when(authentication.getAuthorities()).thenReturn(new ArrayList<>());
        when(appUser.hasSpecificPermissionTo(TwoFactorConstants.BYPASS_TWO_FACTOR_PERMISSION)).thenReturn(false);
        when(appUser.isBypassTwoFactor()).thenReturn(true);

        // When
        filter.doFilter(request, response, filterChain);

        // Then
        verify(filterChain).doFilter(request, response);
        verify(response, never()).sendError(eq(HttpServletResponse.SC_UNAUTHORIZED), any());
    }

    @Test
    void shouldBypassTwoFactorWhenUserHasBypassPermission() throws Exception {
        // Given
        FineractPlatformTenant tenant = new FineractPlatformTenant(1L, "default", "Default", "UTC", null, true);
        ThreadLocalContextUtil.setTenant(tenant);

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(appUser);
        when(authentication.getAuthorities()).thenReturn(new ArrayList<>());
        when(appUser.hasSpecificPermissionTo(TwoFactorConstants.BYPASS_TWO_FACTOR_PERMISSION)).thenReturn(true);

        // When
        filter.doFilter(request, response, filterChain);

        // Then
        verify(filterChain).doFilter(request, response);
        verify(response, never()).sendError(eq(HttpServletResponse.SC_UNAUTHORIZED), any());
    }

    @Test
    void shouldRequireTokenWhenUserCannotBypass() throws Exception {
        // Given
        FineractPlatformTenant tenant = new FineractPlatformTenant(1L, "default", "Default", "UTC", null, true);
        ThreadLocalContextUtil.setTenant(tenant);

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(appUser);
        when(appUser.hasSpecificPermissionTo(TwoFactorConstants.BYPASS_TWO_FACTOR_PERMISSION)).thenReturn(false);
        when(appUser.isBypassTwoFactor()).thenReturn(false);
        when(request.getHeader("Fineract-Platform-TFA-Token")).thenReturn(null);

        // When
        filter.doFilter(request, response, filterChain);

        // Then - should continue without TWOFACTOR_AUTHENTICATED authority
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldReturnUnauthorizedWhenInvalidTokenProvided() throws Exception {
        // Given
        FineractPlatformTenant tenant = new FineractPlatformTenant(1L, "default", "Default", "UTC", null, true);
        ThreadLocalContextUtil.setTenant(tenant);

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(appUser);
        when(appUser.hasSpecificPermissionTo(TwoFactorConstants.BYPASS_TWO_FACTOR_PERMISSION)).thenReturn(false);
        when(appUser.isBypassTwoFactor()).thenReturn(false);
        when(request.getHeader("Fineract-Platform-TFA-Token")).thenReturn("invalid-token");
        when(twoFactorService.fetchAccessTokenForUser(appUser, "invalid-token")).thenReturn(null);

        // When
        filter.doFilter(request, response, filterChain);

        // Then
        verify(response).sendError(eq(HttpServletResponse.SC_UNAUTHORIZED), any());
    }

    @Test
    void shouldBypassTwoFactorWhenTenantHasTwoFactorDisabled() throws Exception {
        // Given - tenant with 2FA disabled
        FineractPlatformTenant tenant = new FineractPlatformTenant(1L, "default", "Default", "UTC", null, false);
        ThreadLocalContextUtil.setTenant(tenant);

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(appUser);
        when(authentication.getAuthorities()).thenReturn(new ArrayList<>());

        // When
        filter.doFilter(request, response, filterChain);

        // Then - should proceed without checking bypass flags
        verify(filterChain).doFilter(request, response);
        verify(appUser, never()).isBypassTwoFactor();
        verify(appUser, never()).hasSpecificPermissionTo(any());
    }

    @Test
    void shouldAcceptValidTwoFactorToken() throws Exception {
        // Given
        FineractPlatformTenant tenant = new FineractPlatformTenant(1L, "default", "Default", "UTC", null, true);
        ThreadLocalContextUtil.setTenant(tenant);

        TFAccessToken validToken = mock(TFAccessToken.class);
        when(validToken.isValid()).thenReturn(true);

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(appUser);
        when(authentication.getAuthorities()).thenReturn(new ArrayList<>());
        when(appUser.hasSpecificPermissionTo(TwoFactorConstants.BYPASS_TWO_FACTOR_PERMISSION)).thenReturn(false);
        when(appUser.isBypassTwoFactor()).thenReturn(false);
        when(request.getHeader("Fineract-Platform-TFA-Token")).thenReturn("valid-token");
        when(twoFactorService.fetchAccessTokenForUser(appUser, "valid-token")).thenReturn(validToken);

        // When
        filter.doFilter(request, response, filterChain);

        // Then
        verify(filterChain).doFilter(request, response);
        verify(response, never()).sendError(eq(HttpServletResponse.SC_UNAUTHORIZED), any());
    }
}

