package com.example.employeeapi.crypto;

public interface KeyEncryptionProvider {
    DataKey generateDataKey();

    byte[] decryptDataKey(String wrappedKey);
}
