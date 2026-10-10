package com.example.employeeapi.crypto;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SalaryEncryptionServiceTest {
    private final SalaryEncryptionService encryptionService = new SalaryEncryptionService(
            new LocalKeyEncryptionProvider("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA="));

    @Test
    void encryptsAndDecryptsSalaryWithFreshNonces() {
        BigDecimal salary = new BigDecimal("12345.67");

        String first = encryptionService.encrypt(salary);
        String second = encryptionService.encrypt(salary);

        assertNotEquals(salary.toPlainString(), first);
        assertNotEquals(first, second);
        assertEquals(salary, encryptionService.decrypt(first));
    }

    @Test
    void rejectsInvalidEncryptedSalaryAndWrongLocalKeyProvider() {
        assertThrows(IllegalArgumentException.class, () -> encryptionService.decrypt("not-a-v1-record"));
        assertThrows(IllegalStateException.class,
                () -> encryptionService.decrypt("v1.d3Jvbmc.AAAAAAAAAAAAAAAA.AAAAAAAAAAAAAAAA"));
        assertThrows(IllegalArgumentException.class,
                () -> new LocalKeyEncryptionProvider("not-base64"));
        assertThrows(IllegalArgumentException.class,
                () -> new LocalKeyEncryptionProvider("AA=="));
    }

    @Test
    void detectsCiphertextTampering() {
        String[] encrypted = encryptionService.encrypt(BigDecimal.TEN).split("\\.");
        encrypted[3] = encrypted[3].substring(0, encrypted[3].length() - 1)
                + (encrypted[3].endsWith("A") ? "B" : "A");
        assertThrows(IllegalStateException.class, () -> encryptionService.decrypt(String.join(".", encrypted)));
    }
}
