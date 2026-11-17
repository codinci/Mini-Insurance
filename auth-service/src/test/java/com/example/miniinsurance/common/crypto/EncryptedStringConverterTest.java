package com.example.miniinsurance.common.crypto;

import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EncryptedStringConverterTest {

    private final SecretKey key = new SecretKeySpec(new byte[32], "AES");
    private final EncryptedStringConverter converter = new EncryptedStringConverter(key);

    @Test
    void roundTripsPlaintext() {
        String stored = converter.convertToDatabaseColumn("{\"dateOfBirth\":\"1990-01-01\"}");

        assertThat(converter.convertToEntityAttribute(stored))
                .isEqualTo("{\"dateOfBirth\":\"1990-01-01\"}");
    }

    @Test
    void ciphertextDoesNotContainPlaintextAndDiffersPerCall() {
        String first = converter.convertToDatabaseColumn("DIABETES");
        String second = converter.convertToDatabaseColumn("DIABETES");

        assertThat(first).doesNotContain("DIABETES");
        assertThat(first).isNotEqualTo(second);
    }

    @Test
    void nullPassesThroughUnchanged() {
        assertThat(converter.convertToDatabaseColumn(null)).isNull();
        assertThat(converter.convertToEntityAttribute(null)).isNull();
    }

    @Test
    void tamperedCiphertextIsRejected() {
        String stored = converter.convertToDatabaseColumn("smoker");
        byte[] bytes = Base64.getDecoder().decode(stored);
        bytes[bytes.length - 1] ^= 0x01;
        String tampered = Base64.getEncoder().encodeToString(bytes);

        assertThatThrownBy(() -> converter.convertToEntityAttribute(tampered))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageNotContaining("smoker");
    }

    @Test
    void missingKeyFailsClosedWithoutEchoingSecrets() {
        assertThatThrownBy(() -> EncryptedStringConverter.loadKey(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("DATA_ENCRYPTION_KEY is not set");
    }

    @Test
    void keyMustDecodeToAesLength() {
        String wrongLength = Base64.getEncoder().encodeToString(new byte[20]);

        assertThatThrownBy(() -> EncryptedStringConverter.loadKey(wrongLength))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("DATA_ENCRYPTION_KEY must decode to 16, 24 or 32 bytes");
    }

    @Test
    void keyMustBeBase64() {
        assertThatThrownBy(() -> EncryptedStringConverter.loadKey("not base64 !!!"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("DATA_ENCRYPTION_KEY must be Base64-encoded");
    }
}
