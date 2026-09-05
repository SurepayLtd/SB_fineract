/**
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 *
 * <p>http://www.apache.org/licenses/LICENSE-2.0
 *
 * <p>Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.fineract.selfservice.security.service;

import java.lang.reflect.Field;
import org.apache.fineract.commands.domain.CommandWrapper;
import org.apache.fineract.infrastructure.configuration.domain.ConfigurationDomainService;
import org.apache.fineract.infrastructure.security.service.SpringSecurityPlatformSecurityContext;
import org.apache.fineract.selfservice.useradministration.domain.AppSelfServiceUser;
import org.apache.fineract.useradministration.domain.AppUser;
import org.apache.fineract.useradministration.domain.AppUserRepository;
import org.springframework.core.env.Environment;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;

/**
 * Extends the core {@link SpringSecurityPlatformSecurityContext} to handle both {@link AppUser} and
 * {@link AppSelfServiceUser} principals.
 *
 * <p>Overrides {@code authenticatedUser()} and {@code getAuthenticatedUserIfPresent()} so that when
 * the principal is an {@link AppSelfServiceUser}, a minimal {@link AppUser} stub is returned,
 * allowing core read services to pass their guard checks.
 *
 * <p>{@code authenticatedUser(CommandWrapper)} is handled differently: it is the value that command
 * processing persists as {@code m_portfolio_command_source.maker_id}, a real foreign key into
 * {@code m_appuser}. {@link AppSelfServiceUser} rows live in a wholly separate table with an
 * unrelated id sequence, so the stub can never satisfy that constraint - it is resolved to a real,
 * configured {@link AppUser} instead (see {@link #resolveAuditUser()}).
 */
public class SelfServiceCompatibleSecurityContext extends SpringSecurityPlatformSecurityContext {

  private static final String AUDIT_USER_PROPERTY = "fineract.selfservice.audit-user";
  private static final String DEFAULT_AUDIT_USERNAME = "mifos";

  private final AppUserRepository appUserRepository;
  private final Environment environment;

  public SelfServiceCompatibleSecurityContext(
      ConfigurationDomainService configurationDomainService,
      AppUserRepository appUserRepository,
      Environment environment) {
    super(configurationDomainService);
    this.appUserRepository = appUserRepository;
    this.environment = environment;
  }

  /**
   * Retrieves the authenticated user, wrapping self-service users in a stub.
   *
   * @return the authenticated AppUser
   */
  @Override
  public AppUser authenticatedUser() {
    final Object principal = extractPrincipal();

    if (principal instanceof AppSelfServiceUser selfServiceUser) {
      return toAppUserStub(selfServiceUser);
    }

    return super.authenticatedUser();
  }

  /**
   * Retrieves the authenticated user from the context for a specific command.
   *
   * <p>For a self-service principal this deliberately does NOT return {@link #toAppUserStub}: the
   * result here is persisted as {@code CommandSource.maker}, a real FK to {@code m_appuser}, which
   * a self-service stub id can never satisfy. A real, configured {@link AppUser} is used instead.
   *
   * @param commandWrapper the command wrapper contextualizing the request
   * @return the authenticated AppUser
   */
  @Override
  public AppUser authenticatedUser(final CommandWrapper commandWrapper) {
    final Object principal = extractPrincipal();

    if (principal instanceof AppSelfServiceUser) {
      return resolveAuditUser();
    }

    return super.authenticatedUser(commandWrapper);
  }

  /**
   * Retrieves the authenticated user if one is currently present in the security context.
   *
   * @return the authenticated AppUser, or null if none
   */
  @Override
  public AppUser getAuthenticatedUserIfPresent() {
    final Object principal = extractPrincipal();

    if (principal instanceof AppSelfServiceUser selfServiceUser) {
      return toAppUserStub(selfServiceUser);
    }

    return super.getAuthenticatedUserIfPresent();
  }

  private Object extractPrincipal() {
    final SecurityContext context = SecurityContextHolder.getContext();
    if (context != null) {
      final Authentication auth = context.getAuthentication();
      if (auth != null) {
        return auth.getPrincipal();
      }
    }
    return null;
  }

  private AppUser toAppUserStub(AppSelfServiceUser selfServiceUser) {
    final User springUser =
        new User(
            selfServiceUser.getUsername(),
            selfServiceUser.getPassword(),
            selfServiceUser.isEnabled(),
            selfServiceUser.isAccountNonExpired(),
            selfServiceUser.isCredentialsNonExpired(),
            selfServiceUser.isAccountNonLocked(),
            selfServiceUser.getAuthorities());
    final AppUser stub =
        new AppUser(
            selfServiceUser.getOffice(),
            springUser,
            selfServiceUser.getRoles(),
            selfServiceUser.getEmail(),
            selfServiceUser.getFirstname(),
            selfServiceUser.getLastname(),
            null,
            true,
            false,
            null,
            false,
            false);
    setId(stub, selfServiceUser.getId());
    return stub;
  }

  /**
   * Resolves the real, managed {@link AppUser} recorded as the maker of commands raised by
   * self-service users, configurable via {@value #AUDIT_USER_PROPERTY} (defaults to {@value
   * #DEFAULT_AUDIT_USERNAME}).
   */
  private AppUser resolveAuditUser() {
    final String auditUsername = environment.getProperty(AUDIT_USER_PROPERTY, DEFAULT_AUDIT_USERNAME);
    final AppUser auditUser = appUserRepository.findAppUserByName(auditUsername);
    if (auditUser == null) {
      throw new IllegalStateException(
          "Configured self-service audit user '"
              + auditUsername
              + "' does not exist. Set "
              + AUDIT_USER_PROPERTY
              + " to a valid m_appuser username.");
    }
    return auditUser;
  }

  private void setId(AppUser stub, Long id) {
    try {
      Field idField = AppUser.class.getSuperclass().getDeclaredField("id");
      idField.setAccessible(true);
      idField.set(stub, id);
    } catch (NoSuchFieldException | IllegalAccessException e) {
      throw new IllegalStateException("Failed to set id on AppUser stub", e);
    }
  }
}
