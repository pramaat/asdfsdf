package com.example.employeeapi.crypto;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

@Component
public class SalaryEncryptionService {
    private static final String FORMAT_VERSION = "v1";
    private static final int NONCE_LENGTH = 12;
    private static final int GCM_TAG_LENGTH_BITS = 128;
    private static final String CIPHER = "AES/GCM/NoPadding";
    private static final String AAD = "employee-salary:v1";

    private final KeyEncryptionProvider keyProvider;
    private final SecureRandom secureRandom;

    @Autowired
    public SalaryEncryptionService(KeyEncryptionProvider keyProvider) {
        this(keyProvider, new SecureRandom());
    }

    SalaryEncryptionService(KeyEncryptionProvider keyProvider, SecureRandom secureRandom) {
        this.keyProvider = keyProvider;
        this.secureRandom = secureRandom;
    }

    public String encrypt(BigDecimal salary) {
        DataKey dataKey = keyProvider.generateDataKey();
        byte[] nonce = new byte[NONCE_LENGTH];
        secureRandom.nextBytes(nonce);
        try {
            Cipher cipher = Cipher.getInstance(CIPHER);
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(dataKey.plaintext(), "AES"),
                    new GCMParameterSpec(GCM_TAG_LENGTH_BITS, nonce));
            cipher.updateAAD(AAD.getBytes(StandardCharsets.UTF_8));
            byte[] ciphertext = cipher.doFinal(salary.toPlainString().getBytes(StandardCharsets.UTF_8));
            return String.join(".", FORMAT_VERSION, dataKey.wrapped(), encode(nonce), encode(ciphertext));
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Could not encrypt employee salary", exception);
        }
    }

    public BigDecimal decrypt(String encryptedSalary) {
        String[] parts = encryptedSalary.split("\\.", -1);
        if (parts.length != 4 || !FORMAT_VERSION.equals(parts[0])) {
            throw new IllegalArgumentException("Unsupported encrypted salary format");
        }
        try {
            byte[] key = keyProvider.decryptDataKey(parts[1]);
            byte[] nonce = Base64.getUrlDecoder().decode(parts[2]);
            byte[] ciphertext = Base64.getUrlDecoder().decode(parts[3]);
            if (nonce.length != NONCE_LENGTH) {
                throw new IllegalArgumentException("Invalid encrypted salary nonce");
            }
            Cipher cipher = Cipher.getInstance(CIPHER);
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"),
                    new GCMParameterSpec(GCM_TAG_LENGTH_BITS, nonce));
            cipher.updateAAD(AAD.getBytes(StandardCharsets.UTF_8));
            return new BigDecimal(new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8));
        } catch (GeneralSecurityException | IllegalArgumentException exception) {
            throw new IllegalStateException("Could not decrypt employee salary", exception);
        }
    }

    private String encode(byte[] value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value);
    }
}
