package com.example.miniinsurance.common.crypto;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

/**
 * AES-GCM encryption for sensitive columns. Stored form is Base64(IV || ciphertext+tag).
 *
 * <p>The key is read from the {@code DATA_ENCRYPTION_KEY} environment variable (Base64, 16/24/32 bytes)
 * on first use, so the application still starts without it; encrypting or decrypting fails closed.
 * Error messages never include key or plaintext material.
 */
@Converter
public class EncryptedStringConverter implements AttributeConverter<String, String> {

    static final String KEY_ENV_VAR = "DATA_ENCRYPTION_KEY";

    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int IV_LENGTH_BYTES = 12;
    private static final int TAG_LENGTH_BITS = 128;
    private static final SecureRandom RANDOM = new SecureRandom();

    private volatile SecretKey key;

    /** Used by JPA and by Spring's bean container. */
    public EncryptedStringConverter() {
    }

    /** Test seam: supply a key directly. */
    EncryptedStringConverter(SecretKey key) {
        this.key = key;
    }

    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null) {
            return null;
        }
        try {
            byte[] iv = new byte[IV_LENGTH_BYTES];
            RANDOM.nextBytes(iv);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, currentKey(), new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            byte[] ciphertext = cipher.doFinal(attribute.getBytes(StandardCharsets.UTF_8));
            byte[] combined = ByteBuffer.allocate(iv.length + ciphertext.length).put(iv).put(ciphertext).array();
            return Base64.getEncoder().encodeToString(combined);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Failed to encrypt sensitive data", e);
        }
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        try {
            byte[] combined = Base64.getDecoder().decode(dbData);
            if (combined.length <= IV_LENGTH_BYTES) {
                throw new IllegalStateException("Encrypted value is malformed");
            }
            byte[] iv = Arrays.copyOfRange(combined, 0, IV_LENGTH_BYTES);
            byte[] ciphertext = Arrays.copyOfRange(combined, IV_LENGTH_BYTES, combined.length);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, currentKey(), new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            return new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            throw new IllegalStateException("Failed to decrypt sensitive data", e);
        }
    }

    private SecretKey currentKey() {
        SecretKey current = key;
        if (current == null) {
            current = loadKey(System.getenv(KEY_ENV_VAR));
            key = current;
        }
        return current;
    }

    static SecretKey loadKey(String base64) {
        if (base64 == null || base64.isBlank()) {
            throw new IllegalStateException(KEY_ENV_VAR + " is not set");
        }
        byte[] raw;
        try {
            raw = Base64.getDecoder().decode(base64.trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException(KEY_ENV_VAR + " must be Base64-encoded");
        }
        if (raw.length != 16 && raw.length != 24 && raw.length != 32) {
            throw new IllegalStateException(KEY_ENV_VAR + " must decode to 16, 24 or 32 bytes");
        }
        return new SecretKeySpec(raw, "AES");
    }
}
