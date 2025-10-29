package org.apache.fineract.infrastructure.security.service;

import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import lombok.Getter;
import org.springframework.stereotype.Component;

@Component
@Getter
public class KeyProvider {

    private final RSAPublicKey publicKey;
    private final RSAPrivateKey privateKey;

    public KeyProvider(RSAPublicKey publicKey, RSAPrivateKey privateKey) {
        this.publicKey = publicKey;
        this.privateKey = privateKey;
    }
}
