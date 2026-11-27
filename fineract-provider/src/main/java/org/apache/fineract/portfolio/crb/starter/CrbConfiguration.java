package org.apache.fineract.portfolio.crb.starter;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.fineract.infrastructure.codes.domain.CodeValueRepositoryWrapper;
import org.apache.fineract.infrastructure.codes.service.CodeValueReadPlatformService;
import org.apache.fineract.infrastructure.configuration.domain.ConfigurationDomainService;
import org.apache.fineract.infrastructure.creditbureau.domain.CreditBureauLoanProductMappingRepository;
import org.apache.fineract.infrastructure.documentmanagement.service.DocumentWritePlatformService;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.fineract.notification.service.SMSNotificationWritePlatformServiceImpl;
import org.apache.fineract.portfolio.client.domain.ClientNonPersonRepositoryWrapper;
import org.apache.fineract.portfolio.client.domain.ClientRepositoryWrapper;
import org.apache.fineract.portfolio.crb.domain.CrbCheckRepository;
import org.apache.fineract.portfolio.crb.domain.CrbConsentRepository;
import org.apache.fineract.portfolio.crb.serializer.CrbDataValidator;
import org.apache.fineract.portfolio.crb.service.CrbCheckReadServicePlatform;
import org.apache.fineract.portfolio.crb.service.CrbConsentReadServiceImpl;
import org.apache.fineract.portfolio.crb.service.CrbCheckWritePlatformServiceImpl;
import org.apache.fineract.portfolio.crb.service.CrbCheckWritePlatformService;
import org.apache.fineract.portfolio.crb.service.CrbConsentReadService;
import org.apache.fineract.portfolio.crb.service.CrbCheckReadServicePlatformImpl;
import org.apache.fineract.portfolio.crb.service.GnugridCreditReportIntegrationWritePlatformServiceImpl;
import org.apache.fineract.portfolio.crb.service.GnugridCreditReportIntegrationWritePlatformService;
import org.apache.fineract.portfolio.crb.service.CrbCreditCheckWriteService;
import org.apache.fineract.portfolio.crb.service.CrbCreditCheckWriteServiceImpl;
import org.apache.fineract.portfolio.crb.service.GnugridNotificationWritePlatformService;
import org.apache.fineract.portfolio.crb.service.CrbConsentWriteService;
import org.apache.fineract.portfolio.crb.service.CrbConsentWriteServiceImpl;
import org.apache.fineract.portfolio.loanaccount.domain.LoanRepositoryWrapper;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.impl.client.HttpClients;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;

import java.security.SecureRandom;


@Configuration
public class CrbConfiguration {


    @Bean(destroyMethod = "close")
    public CloseableHttpClient closeableHttpClient() {
        return HttpClients.custom()
                .setDefaultRequestConfig(
                        RequestConfig.custom()
                                .setConnectTimeout(50_000)
                                .setConnectionRequestTimeout(20_000)
                                .setSocketTimeout(50_000)
                                .build()
                )
                .setMaxConnTotal(50)
                .setMaxConnPerRoute(20)
                .setRetryHandler(new DefaultHttpRequestRetryHandler(3, true))
                .build();
    }

    @Bean
    @ConditionalOnMissingBean
    public CrbCheckReadServicePlatform crbCheckReadServicePlatform(JdbcTemplate jdbcTemplate, PlatformSecurityContext context, ObjectMapper objectMapper, CrbCheckRepository crbCheckRepository, CloseableHttpClient httpClient) {
        return new CrbCheckReadServicePlatformImpl(jdbcTemplate, context, objectMapper, crbCheckRepository, httpClient);
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
                                                                     CrbCheckRepository crbCheckRepository, ObjectMapper objectMapper, CrbConsentRepository crbConsentRepository, CrbDataValidator fromApiJsonDeserializer, ConfigurationDomainService configurationDomainService){
        return new CrbCheckWritePlatformServiceImpl(clientRepositoryWrapper, gnugridNotificationWritePlatformService, crbCheckRepository, objectMapper, crbConsentRepository, fromApiJsonDeserializer, configurationDomainService);
    }

    @Bean
    @ConditionalOnMissingBean
    public CrbConsentWriteService crbConsentWriteService(CrbConsentRepository crbConsentRepository, ConfigurationDomainService configurationDomainService, ClientRepositoryWrapper clientRepositoryWrapper, PlatformSecurityContext platformSecurityContext,
                                                         SecureRandom secureRandom, CrbDataValidator fromApiJsonDeserializer, SMSNotificationWritePlatformServiceImpl smsNotificationWritePlatformService, CodeValueRepositoryWrapper codeValueRepository, DocumentWritePlatformService documentWritePlatformService){
        return new CrbConsentWriteServiceImpl(crbConsentRepository, configurationDomainService, clientRepositoryWrapper, platformSecurityContext, secureRandom, fromApiJsonDeserializer, smsNotificationWritePlatformService, codeValueRepository, documentWritePlatformService);
    }

    @Bean
    @ConditionalOnMissingBean
    public CrbCreditCheckWriteService crbCreditCheckWriteService(ClientRepositoryWrapper clientRepositoryWrapper, ClientNonPersonRepositoryWrapper clientNonPersonRepositoryWrapper, GnugridNotificationWritePlatformService gnugridNotificationWritePlatformService,
                                                                 CrbCheckRepository crbCheckRepository, ObjectMapper objectMapper, CrbConsentRepository crbConsentRepository, CrbDataValidator fromApiJsonDeserializer, CodeValueRepositoryWrapper codeValueRepository,
                                                                 LoanRepositoryWrapper loanRepositoryWrapper, CreditBureauLoanProductMappingRepository creditBureauLoanProductMappingRepository, ConfigurationDomainService configurationDomainService){
        return new CrbCreditCheckWriteServiceImpl(clientRepositoryWrapper, clientNonPersonRepositoryWrapper, gnugridNotificationWritePlatformService, crbCheckRepository, objectMapper, crbConsentRepository, fromApiJsonDeserializer, codeValueRepository, loanRepositoryWrapper, creditBureauLoanProductMappingRepository, configurationDomainService);
    }


    @Bean
    @ConditionalOnMissingBean
    public GnugridCreditReportIntegrationWritePlatformService gnugridCreditReportIntegrationWritePlatformService(GnugridNotificationWritePlatformService gnugridService, CrbCheckRepository crbCheckRepository, ObjectMapper objectMapper, Environment environment, CrbDataValidator fromApiJsonDeserializer,
                                                                                                                 LoanRepositoryWrapper loanRepositoryWrapper, CrbConsentRepository crbConsentRepository, CreditBureauLoanProductMappingRepository creditBureauLoanProductMappingRepository, ConfigurationDomainService configurationDomainService,  ClientRepositoryWrapper clientRepositoryWrapper){
        return new GnugridCreditReportIntegrationWritePlatformServiceImpl(gnugridService, crbCheckRepository, objectMapper, environment, fromApiJsonDeserializer, loanRepositoryWrapper, crbConsentRepository, creditBureauLoanProductMappingRepository, configurationDomainService, clientRepositoryWrapper);
    }
}
