package com.example.employeeapi.crypto;

import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.kms.model.DecryptRequest;
import software.amazon.awssdk.services.kms.model.DecryptResponse;
import software.amazon.awssdk.services.kms.model.GenerateDataKeyRequest;
import software.amazon.awssdk.services.kms.model.GenerateDataKeyResponse;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AwsKmsKeyEncryptionProviderTest {
    @Test
    void generatesAndDecryptsEnvelopeDataKeysUsingPurposeContext() {
        byte[] plaintext = new byte[32];
        byte[] ciphertext = new byte[]{3, 4, 5};
        AtomicReference<GenerateDataKeyRequest> generateRequest = new AtomicReference<>();
        AtomicReference<DecryptRequest> decryptRequest = new AtomicReference<>();
        AwsKmsKeyEncryptionProvider provider = new AwsKmsKeyEncryptionProvider(request -> {
            generateRequest.set(request);
            return GenerateDataKeyResponse.builder()
                        .plaintext(SdkBytes.fromByteArray(plaintext))
                        .ciphertextBlob(SdkBytes.fromByteArray(ciphertext))
                        .build();
        }, request -> {
            decryptRequest.set(request);
            return DecryptResponse.builder().plaintext(SdkBytes.fromByteArray(plaintext)).build();
        }, "kms-key-id");

        DataKey generated = provider.generateDataKey();
        byte[] decrypted = provider.decryptDataKey(generated.wrapped());

        assertArrayEquals(plaintext, generated.plaintext());
        assertArrayEquals(plaintext, decrypted);
        assertEquals("kms-key-id", generateRequest.get().keyId());
        assertEquals("AES_256", generateRequest.get().keySpecAsString());
        assertEquals("employee-salary", generateRequest.get().encryptionContext().get("purpose"));
        assertEquals("employee-salary", decryptRequest.get().encryptionContext().get("purpose"));
        assertArrayEquals(ciphertext, decryptRequest.get().ciphertextBlob().asByteArray());
    }

    @Test
    void requiresKmsKeyId() {
        assertThrows(IllegalArgumentException.class, () ->
                new AwsKmsKeyEncryptionProvider(request -> null, request -> null, " "));
    }
}
