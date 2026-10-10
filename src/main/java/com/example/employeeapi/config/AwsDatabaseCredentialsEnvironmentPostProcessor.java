package com.example.employeeapi.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.boot.context.config.ConfigDataEnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;

import java.util.Map;
import java.util.function.Function;

public class AwsDatabaseCredentialsEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {
    private static final String PROVIDER_PROPERTY = "app.database-credentials.provider";
    private static final String SECRET_ID_PROPERTY = "app.database-credentials.secret-id";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Function<String, String> secretReader;

    public AwsDatabaseCredentialsEnvironmentPostProcessor() {
        this(AwsDatabaseCredentialsEnvironmentPostProcessor::readAwsSecret);
    }

    AwsDatabaseCredentialsEnvironmentPostProcessor(Function<String, String> secretReader) {
        this.secretReader = secretReader;
    }

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String provider = environment.getProperty(PROVIDER_PROPERTY, "environment");
        if ("environment".equals(provider)) {
            return;
        }
        if (!"aws-secrets-manager".equals(provider)) {
            throw new IllegalArgumentException("Unsupported database credentials provider: " + provider);
        }

        String secretId = environment.getProperty(SECRET_ID_PROPERTY, "");
        if (secretId.isBlank()) {
            throw new IllegalArgumentException(SECRET_ID_PROPERTY + " is required when AWS Secrets Manager is enabled");
        }

        try {
            String secret = secretReader.apply(secretId);
            JsonNode credentials = objectMapper.readTree(secret);
            if (credentials == null || !credentials.isObject()) {
                throw new IllegalStateException("AWS database credentials secret must be a JSON object");
            }
            String username = requiredCredential(credentials, "username");
            String password = requiredCredential(credentials, "password");
            environment.getPropertySources().addFirst(new MapPropertySource("awsDatabaseCredentials", Map.of(
                    "spring.datasource.username", username,
                    "spring.datasource.password", password
            )));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("AWS database credentials secret must be a JSON object with username and password", exception);
        }
    }

    private static String readAwsSecret(String secretId) {
        try (SecretsManagerClient client = SecretsManagerClient.create()) {
            return client.getSecretValue(GetSecretValueRequest.builder().secretId(secretId).build()).secretString();
        }
    }

    private String requiredCredential(JsonNode credentials, String name) {
        JsonNode value = credentials.get(name);
        if (value == null || !value.isTextual() || value.textValue().isBlank()) {
            throw new IllegalStateException("AWS database credentials secret is missing a non-empty " + name + " field");
        }
        return value.textValue();
    }

    @Override
    public int getOrder() {
        return ConfigDataEnvironmentPostProcessor.ORDER + 1;
    }
}
