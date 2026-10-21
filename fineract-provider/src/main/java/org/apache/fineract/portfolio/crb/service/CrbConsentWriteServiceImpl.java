package org.apache.fineract.portfolio.crb.service;

import jakarta.persistence.PersistenceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.fineract.infrastructure.codes.domain.CodeValue;
import org.apache.fineract.infrastructure.codes.domain.CodeValueRepositoryWrapper;
import org.apache.fineract.infrastructure.configuration.domain.ConfigurationDomainService;
import org.apache.fineract.infrastructure.configuration.exception.GlobalConfigurationException;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResultBuilder;
import org.apache.fineract.infrastructure.core.exception.ErrorHandler;
import org.apache.fineract.infrastructure.core.exception.GeneralPlatformDomainRuleException;
import org.apache.fineract.infrastructure.core.service.DateUtils;
import org.apache.fineract.infrastructure.security.service.HashingPasswordEncoder;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.fineract.notification.data.SmsTypeEnum;
import org.apache.fineract.notification.service.SMSNotificationWritePlatformServiceImpl;
import org.apache.fineract.portfolio.client.api.ClientApiConstants;
import org.apache.fineract.portfolio.client.domain.CRBStatus;
import org.apache.fineract.portfolio.client.domain.Client;
import org.apache.fineract.portfolio.client.domain.ClientRepositoryWrapper;
import org.apache.fineract.portfolio.crb.data.CrbApiConstants;
import org.apache.fineract.portfolio.crb.serializer.CrbDataValidator;
import org.apache.fineract.portfolio.crb.domain.ConsentChannel;
import org.apache.fineract.portfolio.crb.domain.ConsentStatus;
import org.apache.fineract.portfolio.crb.domain.CrbConsent;
import org.apache.fineract.portfolio.crb.domain.CrbConsentRepository;
import org.apache.fineract.useradministration.domain.AppUser;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.jpa.JpaSystemException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class CrbConsentWriteServiceImpl implements CrbConsentWriteService{

    private final CrbConsentRepository crbConsentRepository;
    private final ConfigurationDomainService configurationDomainService;
    private final ClientRepositoryWrapper clientRepositoryWrapper;
    private final PlatformSecurityContext platformSecurityContext;
    private final  SecureRandom secureRandom;
    private final CrbDataValidator fromApiJsonDeserializer;
    private final SMSNotificationWritePlatformServiceImpl smsNotificationWritePlatformService;
    private final CodeValueRepositoryWrapper codeValueRepository;

    @Transactional
    @Override
    public CommandProcessingResult createConsent(Long clientId, JsonCommand command) {

        try{

            final AppUser user = this.platformSecurityContext.authenticatedUser();
            this.fromApiJsonDeserializer.validateCrbConsent(command);

            boolean crbEnabled = configurationDomainService.isCRBEnabled();

            if (!crbEnabled){
                throw new GeneralPlatformDomainRuleException("error.msg.crb.consent.response",
                        "CRB not enabled for this Tenant."
                );
            }
            if (this.crbConsentRepository.existsByClientId(clientId)) {
                throw new GeneralPlatformDomainRuleException("error.msg.crb.consent.already.exists",
                        "A CRB consent record already exists for client ID " + clientId + ". Update the existing consent record instead.");
            }

            final Integer consentChannel = command.integerValueOfParameterNamed("consentChannel");
            final Client client = this.clientRepositoryWrapper.findOneWithNotFoundDetection(clientId);

            final String phone = client.mobileNo();

            String otp = null;
            String hashedOtp = null;
            LocalDateTime expiryTime = null;
            Integer consentStatus = null;
            LocalDateTime consentedAt = null;

            if (consentChannel.equals(ConsentChannel.OTP.getValue())){
                 otp = String.valueOf(secureRandom.nextInt( 90000) + 10000);
                final String salt = client.getId() + phone;
                hashedOtp = new HashingPasswordEncoder().encode(salt + otp);
                final Integer otpExpiryMinutes = this.configurationDomainService.retrieveMomoPaymentOtpExpiryMinutes();
                consentStatus = ConsentStatus.PENDING.getValue();
                expiryTime = LocalDateTime.now().plusMinutes(otpExpiryMinutes);
            } else if (consentChannel.equals(ConsentChannel.PAPER.getValue())) {
                consentStatus = ConsentStatus.VERIFIED.getValue();
                consentedAt = LocalDateTime.now();
                client.markCrbVerificationPending();
            }

            CrbConsent crbConsent = CrbConsent.fromJson(command, client, user, consentStatus, hashedOtp, expiryTime,consentedAt);
            crbConsent.setOtpUsed(false);
            this.crbConsentRepository.save(crbConsent);

            if (otp !=null) {
                this.smsNotificationWritePlatformService.processClientSmsNotification(client, SmsTypeEnum.OTP_CRB_CONSENT, Integer.valueOf(otp), null);
            }

            return new CommandProcessingResultBuilder()
                    .withEntityId(crbConsent.getId())
                    .withClientId(clientId)
                    .build();

        }catch (final JpaSystemException | DataIntegrityViolationException dve) {
            handleDataIntegrityIssues(dve.getMostSpecificCause(), dve);
            return CommandProcessingResult.empty();
        } catch (final PersistenceException dve) {
            Throwable throwable = ExceptionUtils.getRootCause(dve.getCause());
            handleDataIntegrityIssues(throwable, dve);
            return CommandProcessingResult.empty();
        }

    }

    @Transactional
    @Override
    public CommandProcessingResult validateOtp(final Long consentId, final JsonCommand command) {

        final AppUser user = this.platformSecurityContext.authenticatedUser();
        this.fromApiJsonDeserializer.validateOtpCode(command);

        try {
            final CrbConsent consent = this.crbConsentRepository.findById(consentId)
                    .orElseThrow(() -> new GeneralPlatformDomainRuleException("error.msg.crb.consent.not.found", "CRB consent record not found."));

            final Integer otpCode = command.integerValueOfParameterNamed(ClientApiConstants.otpCodeParamName);

            if (!ConsentChannel.OTP.getValue().equals(consent.getConsentChannel())) {
                throw new GeneralPlatformDomainRuleException("error.msg.crb.consent.channel.invalid",
                        "This consent was not initiated using OTP.");
            }

            if (!ConsentStatus.PENDING.getValue().equals(consent.getConsentStatus()) && !ConsentStatus.REACTIVATED.getValue().equals(consent.getConsentStatus())) {
                throw new GeneralPlatformDomainRuleException("error.msg.crb.consent.not.pending",
                        "CRB consent is not awaiting OTP verification.");
            }

            if (consent.isOtpUsed()) {
                throw new GeneralPlatformDomainRuleException("error.msg.crb.otp.already.used", "OTP has already been used.");
            }

            final LocalDateTime tenantDateTime = DateUtils.getLocalDateTimeOfTenant();

            if (consent.getOtpExpiresAt() == null || !tenantDateTime.isBefore(consent.getOtpExpiresAt())) {
                throw new GeneralPlatformDomainRuleException("error.msg.crb.otp.expired",
                        "OTP has expired.");
            }

            final Client client = consent.getClient();

            if (consent.getOtpHash() == null) {
                throw new GeneralPlatformDomainRuleException("error.msg.crb.otp.invalid", "Invalid OTP.");
            }

            final String salt = client.getId() + client.getMobileNo();
            final String otpToVerify = new HashingPasswordEncoder().encode(salt + otpCode);

            final boolean otpMatches = consent.getOtpHash().equals(otpToVerify);

            if (!otpMatches) {
                throw new GeneralPlatformDomainRuleException("error.msg.crb.otp.invalid", "Invalid OTP.");
            }

            consent.setOtpUsed(true);
            consent.setConsentStatus(ConsentStatus.VERIFIED.getValue());
            consent.setConsentedAt(tenantDateTime);
            consent.setVerifiedBy(user);
            consent.setOtpVerifiedAt(tenantDateTime);
            client.markCrbVerificationPending();

            this.crbConsentRepository.saveAndFlush(consent);

            return new CommandProcessingResultBuilder()
                    .withCommandId(command.commandId())
                    .withOfficeId(client.officeId())
                    .withClientId(client.getId())
                    .withEntityId(consentId)
                    .build();

        } catch (final JpaSystemException | DataIntegrityViolationException dve) {
            handleDataIntegrityIssues(dve.getMostSpecificCause(), dve);
            return CommandProcessingResult.empty();
        } catch (final PersistenceException dve) {
            final Throwable throwable = ExceptionUtils.getRootCause(dve.getCause());
            handleDataIntegrityIssues(throwable, dve);
            return CommandProcessingResult.empty();
        }
    }

    @Transactional
    @Override
    public CommandProcessingResult revokeConsent(Long consentId, JsonCommand command) {

        final AppUser user = this.platformSecurityContext.authenticatedUser();

        try {
            final CrbConsent consent = this.crbConsentRepository.findById(consentId)
                    .orElseThrow(() -> new GeneralPlatformDomainRuleException("error.msg.crb.consent.not.found", "CRB consent record not found."));

            final Long revokeReasonId = command.longValueOfParameterNamed(CrbApiConstants.revokeReasonIdParamName);

            final CodeValue rejectionReason = this.codeValueRepository.findOneByCodeNameAndIdWithNotFoundDetection(CrbApiConstants.CRB_REVOKE_REASON, revokeReasonId);

            final Client client = consent.getClient();

            if (!ConsentStatus.VERIFIED.getValue().equals(consent.getConsentStatus())) {
                throw new GeneralPlatformDomainRuleException("error.msg.crb.consent.not.verified",
                        "CRB consent is not verified.");
            }

            final LocalDateTime tenantDateTime = DateUtils.getLocalDateTimeOfTenant();


            consent.revoke(user, tenantDateTime, rejectionReason);
            client.setCrbVerificationStatus(CRBStatus.NOT_STARTED.getValue());

            this.crbConsentRepository.saveAndFlush(consent);

            return new CommandProcessingResultBuilder()
                    .withOfficeId(client.officeId())
                    .withClientId(client.getId())
                    .withEntityId(consentId)
                    .build();

        } catch (final JpaSystemException | DataIntegrityViolationException dve) {
            handleDataIntegrityIssues(dve.getMostSpecificCause(), dve);
            return CommandProcessingResult.empty();
        } catch (final PersistenceException dve) {
            final Throwable throwable = ExceptionUtils.getRootCause(dve.getCause());
            handleDataIntegrityIssues(throwable, dve);
            return CommandProcessingResult.empty();
        }
    }

    @Transactional
    @Override
    public CommandProcessingResult reSendOtp(final Long consentId) {
        try {
            this.platformSecurityContext.authenticatedUser();

            final CrbConsent consent = this.crbConsentRepository.findById(consentId)
                    .orElseThrow(() -> new GeneralPlatformDomainRuleException("error.msg.crb.consent.not.found",
                            "CRB consent with ID " + consentId + " was not found."));

            final Client client = consent.getClient();

            if (!Objects.equals(consent.getConsentChannel(), ConsentChannel.OTP.getValue())) {
                throw new GeneralPlatformDomainRuleException("error.msg.crb.consent.otp.resend.invalid.channel",
                        "OTP cannot be resent because this consent was not created using the OTP channel.");
            }

            if (Objects.equals(consent.getConsentStatus(), ConsentStatus.REVOKED.getValue())) {
                throw new GeneralPlatformDomainRuleException("error.msg.crb.consent.otp.resend.revoked",
                        "OTP cannot be resent because the CRB consent has been revoked. Create a new consent request.");
            }

            if (!Objects.equals(consent.getConsentStatus(), ConsentStatus.PENDING.getValue()) && !Objects.equals(consent.getConsentStatus(), ConsentStatus.REACTIVATED.getValue())) {
                throw new GeneralPlatformDomainRuleException("error.msg.crb.consent.otp.resend.invalid.status",
                        "OTP can only be resent when the CRB consent is pending or reactivated.");
            }

            if (consent.getConsentedAt() != null || consent.getOtpVerifiedAt() != null) {
                throw new GeneralPlatformDomainRuleException("error.msg.crb.consent.otp.resend.already.verified",
                        "OTP cannot be resent because the CRB consent has already been verified.");
            }

            final String phone = client.mobileNo();

            if (phone == null || phone.isBlank()) {
                throw new GeneralPlatformDomainRuleException("error.msg.crb.consent.otp.resend.phone.missing",
                        "OTP cannot be resent because the client has no registered phone number.");
            }

            final String otp = String.valueOf(this.secureRandom.nextInt(90000) + 10000);

            final String salt = client.getId() + phone;
            final String hashedOtp = new HashingPasswordEncoder().encode(salt + otp);

            final Integer otpExpiryMinutes = this.configurationDomainService.retrieveMomoPaymentOtpExpiryMinutes();

            if (otpExpiryMinutes == null || otpExpiryMinutes <= 0) {
                throw new GeneralPlatformDomainRuleException("error.msg.crb.consent.otp.invalid.expiry",
                        "The configured OTP expiry duration is invalid.");
            }

            final LocalDateTime expiryTime = LocalDateTime.now().plusMinutes(otpExpiryMinutes);

            consent.setOtpUsed(false);
            consent.setOtpHash(hashedOtp);
            consent.setOtpExpiresAt(expiryTime);

            this.crbConsentRepository.saveAndFlush(consent);

            this.smsNotificationWritePlatformService.processClientSmsNotification(client, SmsTypeEnum.OTP_CRB_CONSENT, Integer.valueOf(otp), null);

            return new CommandProcessingResultBuilder()
                    .withOfficeId(client.officeId())
                    .withClientId(client.getId())
                    .withEntityId(consentId)
                    .build();

        } catch (final JpaSystemException | DataIntegrityViolationException dve) {
            handleDataIntegrityIssues(dve.getMostSpecificCause(), dve);
            return CommandProcessingResult.empty();

        } catch (final PersistenceException dve) {
            final Throwable throwable = ExceptionUtils.getRootCause(dve);
            handleDataIntegrityIssues(throwable != null ? throwable : dve, dve);
            return CommandProcessingResult.empty();
        }
    }

    @Transactional
    @Override
    public CommandProcessingResult reactivateConsent(final Long consentId) {

        try {
            AppUser user = this.platformSecurityContext.authenticatedUser();

            final CrbConsent consent = this.crbConsentRepository.findById(consentId)
                    .orElseThrow(() -> new GeneralPlatformDomainRuleException("error.msg.crb.consent.not.found",
                            "CRB consent with ID " + consentId + " was not found."));

            final Client client = consent.getClient();
            final String phone = client.mobileNo();

            final LocalDateTime tenantDateTime = DateUtils.getLocalDateTimeOfTenant();


            String otp = null;

            if (!Objects.equals(consent.getConsentStatus(), ConsentStatus.REVOKED.getValue())) {
                throw new GeneralPlatformDomainRuleException("error.msg.crb.consent.reactivate.invalid.status",
                        "Only revoked CRB consent can be reactivated.");
            }

            if (Objects.equals(consent.getConsentChannel(), ConsentChannel.OTP.getValue())) {

                if (phone == null || phone.isBlank()) {
                    throw new GeneralPlatformDomainRuleException("error.msg.crb.consent.reactivate.phone.missing",
                            "The client must have a registered phone number to reactivate consent.");
                }

                final Integer otpExpiryMinutes = this.configurationDomainService.retrieveMomoPaymentOtpExpiryMinutes();

                if (otpExpiryMinutes == null || otpExpiryMinutes <= 0) {
                    throw new GeneralPlatformDomainRuleException("error.msg.crb.consent.otp.invalid.expiry",
                            "The configured OTP expiry duration is invalid.");
                }

                otp = String.valueOf(this.secureRandom.nextInt(90000) + 10000);
                final String salt = client.getId() + phone;
                final String hashedOtp = new HashingPasswordEncoder().encode(salt + otp);
                final LocalDateTime expiryTime = LocalDateTime.now().plusMinutes(otpExpiryMinutes);

                consent.setOtpHash(hashedOtp);
                consent.setOtpExpiresAt(expiryTime);
                consent.setOtpUsed(false);
                consent.setOtpVerifiedAt(null);
                consent.setConsentedAt(null);

                consent.reactivate(user, tenantDateTime);

            } else {
                consent.reactivate(user, tenantDateTime);
                consent.setConsentedAt(tenantDateTime);
            }

            this.crbConsentRepository.saveAndFlush(consent);

            if (otp !=null) {
                this.smsNotificationWritePlatformService.processClientSmsNotification(client, SmsTypeEnum.OTP_CRB_CONSENT, Integer.valueOf(otp), null);
            }


            return new CommandProcessingResultBuilder()
                    .withOfficeId(client.officeId())
                    .withClientId(client.getId())
                    .withEntityId(consentId)
                    .build();
        }catch (final JpaSystemException | DataIntegrityViolationException dve) {
            handleDataIntegrityIssues(dve.getMostSpecificCause(), dve);
            return CommandProcessingResult.empty();

        } catch (final PersistenceException dve) {
            final Throwable throwable = ExceptionUtils.getRootCause(dve);
            handleDataIntegrityIssues(throwable != null ? throwable : dve, dve);
            return CommandProcessingResult.empty();
        }
    }

    private void handleDataIntegrityIssues(final Throwable realCause, final Exception dve) {

        logAsErrorUnexpectedDataIntegrityException(dve);
        throw ErrorHandler.getMappable(dve, "error.msg.crbconsent.unknown.data.integrity.issue",
                "Unknown data integrity issue with resource: " + realCause.getMessage());
    }

    private void logAsErrorUnexpectedDataIntegrityException(final Exception dve) {
        log.error("Error occured.", dve);
    }
}
