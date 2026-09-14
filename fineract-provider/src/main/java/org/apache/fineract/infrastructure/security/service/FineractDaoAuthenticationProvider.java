package org.apache.fineract.infrastructure.security.service;


import org.apache.fineract.infrastructure.configuration.domain.ConfigurationDomainService;
import org.apache.fineract.notification.data.SmsTypeEnum;
import org.apache.fineract.notification.service.SmsNotificationWritePlatformService;
import org.apache.fineract.useradministration.domain.AppUser;
import org.apache.fineract.useradministration.domain.AppUserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;

public class FineractDaoAuthenticationProvider extends DaoAuthenticationProvider {

    private final AppUserRepository appUserRepository;
    private final ConfigurationDomainService configurationDomainService;
    private final SmsNotificationWritePlatformService smsNotificationWritePlatformService;

    public FineractDaoAuthenticationProvider(AppUserRepository appUserRepository, ConfigurationDomainService configurationDomainService,SmsNotificationWritePlatformService smsNotificationWritePlatformService,
            UserDetailsService userDetailsService, PasswordEncoder passwordEncoder) {

        this.appUserRepository = appUserRepository;
        this.configurationDomainService = configurationDomainService;
        this.smsNotificationWritePlatformService = smsNotificationWritePlatformService;

        setUserDetailsService(userDetailsService);
        setPasswordEncoder(passwordEncoder);
    }

    @Override
    protected void additionalAuthenticationChecks(UserDetails userDetails, UsernamePasswordAuthenticationToken authentication) throws AuthenticationException {

        try {

            super.additionalAuthenticationChecks(userDetails, authentication);

        } catch (BadCredentialsException exception) {

            AppUser user = (AppUser) userDetails;

            Integer maxAttempts = configurationDomainService.retrieveMaxLoginAttempts();

            user.handleMaxLoginAttempts(maxAttempts);

            appUserRepository.save(user);

            if (!user.isAccountNonLocked() && user.getUserBlockedAt() != null) {
                this.smsNotificationWritePlatformService.processAppUserSms(user, SmsTypeEnum.USER_BLOCKED);
                throw new BadCredentialsException("Your account has been blocked after " + maxAttempts + " failed login attempts.");
            }

            throw exception;
        }
    }

    @Override
    protected Authentication createSuccessAuthentication(Object principal, Authentication authentication, UserDetails user) {

        AppUser appUser = (AppUser) user;

        if (appUser.getLoginAttempts() != null && appUser.getLoginAttempts() > 0) {

            appUser.resetLoginAttempts();

            this.appUserRepository.save(appUser);
        }

        return super.createSuccessAuthentication(principal, authentication, user);
    }
}
