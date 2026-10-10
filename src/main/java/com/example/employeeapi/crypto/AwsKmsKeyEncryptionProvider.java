package com.example.employeeapi.crypto;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.kms.KmsClient;
import software.amazon.awssdk.services.kms.model.DataKeySpec;
import software.amazon.awssdk.services.kms.model.DecryptRequest;
import software.amazon.awssdk.services.kms.model.DecryptResponse;
import software.amazon.awssdk.services.kms.model.GenerateDataKeyRequest;
import software.amazon.awssdk.services.kms.model.GenerateDataKeyResponse;

import java.util.Base64;
import java.util.Map;
import java.util.function.Function;

@Component
@ConditionalOnProperty(name = "app.crypto.provider", havingValue = "aws-kms")
public class AwsKmsKeyEncryptionProvider implements KeyEncryptionProvider {
    private static final Map<String, String> ENCRYPTION_CONTEXT = Map.of("purpose", "employee-salary");

    private final String keyId;
    private final Function<GenerateDataKeyRequest, GenerateDataKeyResponse> generateDataKey;
    private final Function<DecryptRequest, DecryptResponse> decryptDataKey;

    @Autowired
    public AwsKmsKeyEncryptionProvider(
            KmsClient kmsClient,
            @Value("${app.crypto.aws-kms-key-id}") String keyId
    ) {
        this(kmsClient::generateDataKey, kmsClient::decrypt, keyId);
    }

    AwsKmsKeyEncryptionProvider(
            Function<GenerateDataKeyRequest, GenerateDataKeyResponse> generateDataKey,
            Function<DecryptRequest, DecryptResponse> decryptDataKey,
            String keyId
    ) {
        if (keyId.isBlank()) {
            throw new IllegalArgumentException("app.crypto.aws-kms-key-id is required when AWS KMS encryption is enabled");
        }
        this.generateDataKey = generateDataKey;
        this.decryptDataKey = decryptDataKey;
        this.keyId = keyId;
    }

    @Override
    public DataKey generateDataKey() {
        var result = generateDataKey.apply(GenerateDataKeyRequest.builder()
                .keyId(keyId)
                .keySpec(DataKeySpec.AES_256)
                .encryptionContext(ENCRYPTION_CONTEXT)
                .build());
        return new DataKey(result.plaintext().asByteArray(),
                Base64.getEncoder().encodeToString(result.ciphertextBlob().asByteArray()));
    }

    @Override
    public byte[] decryptDataKey(String wrappedKey) {
        return decryptDataKey.apply(DecryptRequest.builder()
                        .ciphertextBlob(SdkBytes.fromByteArray(Base64.getDecoder().decode(wrappedKey)))
                        .encryptionContext(ENCRYPTION_CONTEXT)
                        .build())
                .plaintext()
                .asByteArray();
    }
}
