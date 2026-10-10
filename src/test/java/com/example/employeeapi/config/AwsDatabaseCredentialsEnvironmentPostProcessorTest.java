package com.example.employeeapi.config;

import org.junit.jupiter.api.Test;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AwsDatabaseCredentialsEnvironmentPostProcessorTest {
    @Test
    void loadsAwsDatabaseCredentialsAsHighestPriorityProperties() {
        StandardEnvironment environment = environment(Map.of(
                "app.database-credentials.provider", "aws-secrets-manager",
                "app.database-credentials.secret-id", "employee-db",
                "spring.datasource.username", "environment-user",
                "spring.datasource.password", "environment-password"
        ));
        AwsDatabaseCredentialsEnvironmentPostProcessor processor =
                new AwsDatabaseCredentialsEnvironmentPostProcessor(
                        secretId -> "{\"username\":\"aws-user\",\"password\":\"aws-password\"}");

        processor.postProcessEnvironment(environment, null);

        assertEquals("aws-user", environment.getProperty("spring.datasource.username"));
        assertEquals("aws-password", environment.getProperty("spring.datasource.password"));
    }

    @Test
    void doesNotLoadSecretsForEnvironmentProvider() {
        StandardEnvironment environment = environment(Map.of("app.database-credentials.provider", "environment"));
        AwsDatabaseCredentialsEnvironmentPostProcessor processor =
                new AwsDatabaseCredentialsEnvironmentPostProcessor(secretId -> {
                    throw new AssertionError("Secret reader must not be called");
                });

        processor.postProcessEnvironment(environment, null);

        assertEquals("environment", environment.getProperty("app.database-credentials.provider"));
    }

    @Test
    void rejectsUnknownProvidersMissingSecretIdsAndMalformedSecrets() {
        AwsDatabaseCredentialsEnvironmentPostProcessor processor =
                new AwsDatabaseCredentialsEnvironmentPostProcessor(secretId -> "not-json");
        assertThrows(IllegalArgumentException.class, () ->
                processor.postProcessEnvironment(environment(Map.of("app.database-credentials.provider", "unknown")), null));
        assertThrows(IllegalArgumentException.class, () ->
                processor.postProcessEnvironment(environment(Map.of("app.database-credentials.provider", "aws-secrets-manager")), null));
        assertThrows(IllegalStateException.class, () ->
                processor.postProcessEnvironment(environment(Map.of(
                        "app.database-credentials.provider", "aws-secrets-manager",
                        "app.database-credentials.secret-id", "employee-db")), null));
    }

    @Test
    void rejectsSecretsWithMissingCredentials() {
        AwsDatabaseCredentialsEnvironmentPostProcessor processor =
                new AwsDatabaseCredentialsEnvironmentPostProcessor(secretId -> "{\"username\":\"\"}");
        StandardEnvironment environment = environment(Map.of(
                "app.database-credentials.provider", "aws-secrets-manager",
                "app.database-credentials.secret-id", "employee-db"));

        assertThrows(IllegalStateException.class, () -> processor.postProcessEnvironment(environment, null));
    }

    private StandardEnvironment environment(Map<String, Object> properties) {
        StandardEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().addFirst(new MapPropertySource("test", properties));
        return environment;
    }
}
