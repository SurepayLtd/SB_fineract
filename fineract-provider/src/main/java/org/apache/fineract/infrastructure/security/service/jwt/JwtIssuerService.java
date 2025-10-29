package org.apache.fineract.infrastructure.security.service.jwt;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.Payload;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.apache.fineract.infrastructure.security.service.JwtConfiguration;
import org.apache.fineract.infrastructure.security.service.KeyProvider;
import org.apache.fineract.useradministration.domain.AppUser;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class JwtIssuerService {

    private final JwtConfiguration jwtConfiguration;
    private final KeyProvider keyProvider;

    public String issueToken(AppUser user) {
        try {
            final var now = Instant.now();
            final var claimsSet = new JWTClaimsSet.Builder()
                    .issuer(jwtConfiguration.getIssuer())
                    .audience(jwtConfiguration.getAudience())
                    .subject(user.getUsername())
                    .claim("userId", user.getId())
                    .claim("roles", user.getRoles().stream().map(role -> role.getName()).collect(Collectors.toList()))
                    .expirationTime(Date.from(now.plus(jwtConfiguration.getExpirationTime())))
                    .issueTime(Date.from(now))
                    .jwtID(UUID.randomUUID().toString())
                    .build();
            final var jwsObject = new JWSObject(new JWSHeader.Builder(JWSAlgorithm.RS256).build(), new Payload(claimsSet.toJSONObject()));
            jwsObject.sign(new RSASSASigner(keyProvider.getPrivateKey()));
            return jwsObject.serialize();
        } catch (JOSEException e) {
            throw new RuntimeException(e);
        }
    }
}
