package org.apache.fineract.portfolio.crb.starter;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.fineract.infrastructure.codes.domain.CodeValueRepositoryWrapper;
import org.apache.fineract.infrastructure.codes.service.CodeValueReadPlatformService;
import org.apache.fineract.infrastructure.configuration.domain.ConfigurationDomainService;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.fineract.notification.service.SMSNotificationWritePlatformServiceImpl;
import org.apache.fineract.portfolio.client.domain.ClientNonPersonRepositoryWrapper;
import org.apache.fineract.portfolio.client.domain.ClientRepositoryWrapper;
import org.apache.fineract.portfolio.crb.domain.CrbCheckRepository;
import org.apache.fineract.portfolio.crb.domain.CrbConsentRepository;
import org.apache.fineract.portfolio.crb.serializer.CrbDataValidator;
import org.apache.fineract.portfolio.crb.service.*;
import org.apache.fineract.portfolio.loanaccount.domain.LoanRepositoryWrapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;

import java.security.SecureRandom;


@Configuration
public class CrbConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public CrbCheckReadServicePlatform crbCheckReadServicePlatform(JdbcTemplate jdbcTemplate, PlatformSecurityContext context, ObjectMapper objectMapper) {
        return new CrbCheckReadServicePlatformImpl(jdbcTemplate, context, objectMapper);
    }

    @Bean
    @ConditionalOnMissingBean(SecureRandom.class)
    public SecureRandom secureRandom() {
        return new SecureRandom();
    }

    @Bean
    @ConditionalOnMissingBean
    public CrbConsentReadService crbConsentReadService(JdbcTemplate jdbcTemplate, PlatformSecurityContext context, CodeValueReadPlatformService codeValueReadPlatformService) {
        return new CrbConsentReadServiceImpl(context, jdbcTemplate, codeValueReadPlatformService);
    }

    @Bean
    @ConditionalOnMissingBean
    public CrbCheckWritePlatformService crbCheckWritePlatformService(ClientRepositoryWrapper clientRepositoryWrapper, GnugridNotificationWritePlatformService gnugridNotificationWritePlatformService,
                                                                     CrbCheckRepository crbCheckRepository, ObjectMapper objectMapper, CrbConsentRepository crbConsentRepository, CrbDataValidator fromApiJsonDeserializer){
        return new CrbCheckWritePlatformServiceImpl(clientRepositoryWrapper, gnugridNotificationWritePlatformService, crbCheckRepository, objectMapper, crbConsentRepository, fromApiJsonDeserializer);
    }

    @Bean
    @ConditionalOnMissingBean
    public CrbConsentWriteService crbConsentWriteService(CrbConsentRepository crbConsentRepository, ConfigurationDomainService configurationDomainService, ClientRepositoryWrapper clientRepositoryWrapper, PlatformSecurityContext platformSecurityContext,
                                                         SecureRandom secureRandom, CrbDataValidator fromApiJsonDeserializer, SMSNotificationWritePlatformServiceImpl smsNotificationWritePlatformService, CodeValueRepositoryWrapper codeValueRepository){
        return new CrbConsentWriteServiceImpl(crbConsentRepository, configurationDomainService, clientRepositoryWrapper, platformSecurityContext, secureRandom, fromApiJsonDeserializer, smsNotificationWritePlatformService, codeValueRepository);
    }

    @Bean
    @ConditionalOnMissingBean
    public CrbCreditCheckWriteService crbCreditCheckWriteService(ClientRepositoryWrapper clientRepositoryWrapper, ClientNonPersonRepositoryWrapper clientNonPersonRepositoryWrapper, GnugridNotificationWritePlatformService gnugridNotificationWritePlatformService,
                                                                 CrbCheckRepository crbCheckRepository, ObjectMapper objectMapper, CrbConsentRepository crbConsentRepository, CrbDataValidator fromApiJsonDeserializer, Environment environment, CodeValueRepositoryWrapper codeValueRepository, LoanRepositoryWrapper loanRepositoryWrapper){
        return new CrbCreditCheckWriteServiceImpl(clientRepositoryWrapper, clientNonPersonRepositoryWrapper, gnugridNotificationWritePlatformService, crbCheckRepository, objectMapper, crbConsentRepository, fromApiJsonDeserializer,environment, codeValueRepository, loanRepositoryWrapper);
    }
}
