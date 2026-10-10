package com.example.employeeapi.crypto;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.services.kms.KmsClient;

@Configuration
@ConditionalOnProperty(name = "app.crypto.provider", havingValue = "aws-kms")
public class KmsConfiguration {
    @Bean
    KmsClient kmsClient() {
        return KmsClient.create();
    }
}
