package org.apache.fineract.infrastructure.security.service;

import java.time.Duration;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties("fineract.security.jwt")
@Getter
@Setter
public class JwtConfiguration {

    private String issuer;
    private String audience;
    private Duration expirationTime;
}
