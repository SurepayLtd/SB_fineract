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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDate;
import java.util.HashMap;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.fineract.infrastructure.businessdate.domain.BusinessDateType;
import org.apache.fineract.infrastructure.businessdate.service.BusinessDateReadPlatformService;
import org.apache.fineract.infrastructure.cache.domain.CacheType;
import org.apache.fineract.infrastructure.cache.service.CacheWritePlatformService;
import org.apache.fineract.infrastructure.configuration.domain.ConfigurationDomainService;
import org.apache.fineract.infrastructure.core.domain.FineractPlatformTenant;
import org.apache.fineract.infrastructure.core.serialization.ToApiJsonSerializer;
import org.apache.fineract.infrastructure.core.service.ThreadLocalContextUtil;
import org.apache.fineract.infrastructure.security.data.PlatformRequestLog;
import org.apache.fineract.infrastructure.security.exception.InvalidTenantIdentifierException;
import org.apache.fineract.infrastructure.security.service.BasicAuthTenantDetailsService;
import org.apache.fineract.infrastructure.security.service.JwtHelper;
import org.apache.fineract.infrastructure.security.service.TenantAwareJpaPlatformUserDetailsService;
import org.apache.fineract.notification.service.UserNotificationService;
import org.apache.fineract.useradministration.domain.AppUser;
import org.apache.fineract.useradministration.domain.AppUserRepositoryWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.util.matcher.AnyRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;


/**
 * A customised version of spring security's {@link BasicAuthenticationFilter}.
 *
 * This filter is responsible for extracting multi-tenant and basic auth credentials from the request and checking that
 * the details provided are valid.
 *
 * If multi-tenant and basic auth credentials are valid, the details of the tenant are stored in
 * {@link FineractPlatformTenant} and stored in a {@link ThreadLocal} variable for this request using
 * {@link ThreadLocalContextUtil}.
 *
 * If multi-tenant and basic auth credentials are invalid, a http error response is returned.
 */

@Slf4j
public class TenantAwareBasicAuthenticationFilter extends OncePerRequestFilter {

    private static boolean FIRST_REQUEST_PROCESSED = false;
    private static final String TENANT_ID_REQUEST_HEADER = "Fineract-Platform-TenantId";
    private static final boolean EXCEPTION_IF_HEADER_MISSING = true;
    private static final Logger LOG = LoggerFactory.getLogger(TenantAwareBasicAuthenticationFilter.class);
    private final ToApiJsonSerializer<PlatformRequestLog> toApiJsonSerializer;
    private final ConfigurationDomainService configurationDomainService;
    private final CacheWritePlatformService cacheWritePlatformService;
    private final UserNotificationService userNotificationService;
    private final BasicAuthTenantDetailsService basicAuthTenantDetailsService;
    private final BusinessDateReadPlatformService businessDateReadPlatformService;
    private final JwtHelper jwtTokenUtil;
    private final TenantAwareJpaPlatformUserDetailsService jwtUserDetailsService;
    private final AppUserRepositoryWrapper userRepository;

    @Setter
    private RequestMatcher requestMatcher = AnyRequestMatcher.INSTANCE;

    public TenantAwareBasicAuthenticationFilter(final AuthenticationManager authenticationManager,
            final AuthenticationEntryPoint authenticationEntryPoint, ToApiJsonSerializer<PlatformRequestLog> toApiJsonSerializer,
            ConfigurationDomainService configurationDomainService, CacheWritePlatformService cacheWritePlatformService,
            UserNotificationService userNotificationService, BasicAuthTenantDetailsService basicAuthTenantDetailsService,
            BusinessDateReadPlatformService businessDateReadPlatformService, final JwtHelper jwtTokenUtil,
            final TenantAwareJpaPlatformUserDetailsService jwtUserDetailsService, final AppUserRepositoryWrapper userRepository) {
        super();
        this.toApiJsonSerializer = toApiJsonSerializer;
        this.configurationDomainService = configurationDomainService;
        this.cacheWritePlatformService = cacheWritePlatformService;
        this.userNotificationService = userNotificationService;
        this.basicAuthTenantDetailsService = basicAuthTenantDetailsService;
        this.businessDateReadPlatformService = businessDateReadPlatformService;
        this.jwtTokenUtil = jwtTokenUtil;
        this.jwtUserDetailsService = jwtUserDetailsService;
        this.userRepository = userRepository;
    }

    @Override
    @SuppressFBWarnings("SLF4J_SIGN_ONLY_FORMAT")
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        Exception exception = null;
        final StopWatch task = new StopWatch();
        task.start();

        try {
            ThreadLocalContextUtil.reset();
            if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
                // ignore to allow 'preflight' requests from AJAX applications
                // in different origin (domain name)
                filterChain.doFilter(request, response);
            } else {
                if (requestMatcher.matches(request)) {
                    String tenantIdentifier = request.getHeader(TENANT_ID_REQUEST_HEADER);

                    if (org.apache.commons.lang3.StringUtils.isBlank(tenantIdentifier)) {
                        tenantIdentifier = request.getParameter("tenantIdentifier");
                    }

                    if (tenantIdentifier == null && EXCEPTION_IF_HEADER_MISSING) {
                        throw new InvalidTenantIdentifierException("No tenant identifier found: Add request header of '"
                                + TENANT_ID_REQUEST_HEADER + "' or add the parameter 'tenantIdentifier' to query string of request URL.");
                    }

                    String pathInfo = request.getRequestURI();
                    boolean isReportRequest = false;
                    if (pathInfo != null && pathInfo.contains("report")) {
                        isReportRequest = true;
                    }
                    final FineractPlatformTenant tenant = basicAuthTenantDetailsService.loadTenantById(tenantIdentifier, isReportRequest);
                    ThreadLocalContextUtil.setTenant(tenant);
                    HashMap<BusinessDateType, LocalDate> businessDates = businessDateReadPlatformService.getBusinessDates();
                    ThreadLocalContextUtil.setBusinessDates(businessDates);

                    // set JWT token if present

                    final String requestTokenHeader = request.getHeader("Authorization");
                    String pathURL = request.getRequestURI();

                    String username = null;

                    String jwtToken = null;

                    if (requestTokenHeader != null && requestTokenHeader.startsWith("Bearer ")) {
                        jwtToken = requestTokenHeader.substring(7);
                        try {
                            username = jwtTokenUtil.getUsernameFromToken(jwtToken);
                            ThreadLocalContextUtil.setAuthToken(jwtToken);
                        } catch (IllegalArgumentException e) {
                            onAuthenticationFailureJwt(response, "Unable to get JWT Token");
                        } catch (ExpiredJwtException e) {
                            onAuthenticationFailureJwt(response, "JWT Token has expired");
                        }
                    } else {
                        // WhiteList Authentication API
                        if (!pathURL.contains("/fineract-provider/api/v1/authentication")) {
                            onAuthenticationFailureJwt(response, "JWT Token does not begin with Bearer String");
                        }
                    }

                    if (!FIRST_REQUEST_PROCESSED) {
                        final String baseUrl = request.getRequestURL().toString().replace(request.getPathInfo(), "/");
                        System.setProperty("baseUrl", baseUrl);

                        final boolean ehcacheEnabled = configurationDomainService.isEhcacheEnabled();
                        if (ehcacheEnabled) {
                            cacheWritePlatformService.switchToCache(CacheType.SINGLE_NODE);
                        } else {
                            cacheWritePlatformService.switchToCache(CacheType.NO_CACHE);
                        }
                        TenantAwareBasicAuthenticationFilter.FIRST_REQUEST_PROCESSED = true;
                    }

                    if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                        UserDetails userDetails = this.jwtUserDetailsService.loadUserByUsername(username);
                        AppUser user = this.userRepository.fetchUserByUsername(username);
                        if (jwtTokenUtil.validateToken(jwtToken, userDetails) ) {

                            UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken = new UsernamePasswordAuthenticationToken(
                                    userDetails, null, userDetails.getAuthorities());

                            usernamePasswordAuthenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                            SecurityContextHolder.getContext().setAuthentication(usernamePasswordAuthenticationToken);

                            // Deal with Notification and Refresh Token
                            onSuccessfulAuthentication(request, response);

                        } else {
                            onAuthenticationFailureJwt(response, "JWT Token not authorized !!");
                        }

                    }

                }

                filterChain.doFilter(request, response);
            }
        } catch (final InvalidTenantIdentifierException e) {
            // deal with exception at low level
            SecurityContextHolder.getContext().setAuthentication(null);

            response.addHeader("WWW-Authenticate", "Basic realm=\"" + "Fineract Platform API" + "\"");
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        } catch (final Exception e) {
        exception = e;
        } finally {
            ThreadLocalContextUtil.reset();
            task.stop();
            final PlatformRequestLog msg = PlatformRequestLog.from(task, request);
            log.debug("{}", toApiJsonSerializer.serialize(msg));

            if (exception != null) {
                try {
                    throw exception;
                } catch (Exception e) {
                    LOG.error(e.getMessage());
                }
            }
        }
    }

    private void onSuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response) {
        AppUser user = this.userRepository.fetchSystemUser();

        if (userNotificationService.hasUnreadUserNotifications(user.getId())) {
            response.addHeader("X-Notification-Refresh", "true");
        } else {
            response.addHeader("X-Notification-Refresh", "false");
        }
        String pathURL = request.getRequestURI();
        boolean isSelfServiceRequest = pathURL != null && pathURL.contains("/self/");
        boolean notAllowed = (isSelfServiceRequest && !user.isSelfServiceUser()) || (!isSelfServiceRequest && user.isSelfServiceUser());
        if (notAllowed) {
            throw new BadCredentialsException("User not authorised to use the requested resource.");
        }
    }

    public void onAuthenticationFailureJwt(HttpServletResponse response, String errorMessage) throws IOException {
        LOG.error(errorMessage);
        response.sendError(HttpServletResponse.SC_UNAUTHORIZED, errorMessage);
    }
}
