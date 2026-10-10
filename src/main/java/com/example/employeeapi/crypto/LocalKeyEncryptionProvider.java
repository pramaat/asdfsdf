package com.example.employeeapi.crypto;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Base64;

@Component
@ConditionalOnProperty(name = "app.crypto.provider", havingValue = "local", matchIfMissing = true)
public class LocalKeyEncryptionProvider implements KeyEncryptionProvider {
    private static final String WRAPPED_KEY_MARKER = "local";

    private final SecretKey key;

    public LocalKeyEncryptionProvider(@Value("${app.crypto.local-key}") String encodedKey) {
        byte[] keyBytes;
        try {
            keyBytes = Base64.getDecoder().decode(encodedKey);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("app.crypto.local-key must be a base64-encoded 256-bit key", exception);
        }
        if (keyBytes.length != 32) {
            throw new IllegalArgumentException("app.crypto.local-key must be a base64-encoded 256-bit key");
        }
        this.key = new javax.crypto.spec.SecretKeySpec(keyBytes, "AES");
    }

    @Override
    public DataKey generateDataKey() {
        return new DataKey(key.getEncoded(), WRAPPED_KEY_MARKER);
    }

    @Override
    public byte[] decryptDataKey(String wrappedKey) {
        if (!WRAPPED_KEY_MARKER.equals(wrappedKey)) {
            throw new IllegalArgumentException("Encrypted salary uses a different key provider");
        }
        return key.getEncoded();
    }
}
