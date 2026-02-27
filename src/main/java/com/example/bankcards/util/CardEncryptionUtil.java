package com.example.bankcards.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Утилита для шифрования и маскирования номеров банковских карт.
 *
 * Номер карты никогда не хранится в открытом виде в базе данных.
 * При сохранении — шифруется AES-256-GCM с уникальным IV.
 * При отображении пользователю — маскируется: **** **** **** 1234.
 *
 * Формат хранения в БД: base64(iv) + ":" + base64(зашифрованные данные)
 * GCM также гарантирует целостность: если данные подделаны — расшифровка упадёт с ошибкой.
 */
@Component
public class CardEncryptionUtil {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH = 128;
    private static final int GCM_IV_LENGTH = 12;

    private final SecretKeySpec secretKey;

    public CardEncryptionUtil(@Value("${app.encryption.key}") String key) {
        byte[] keyBytes = key.getBytes(StandardCharsets.UTF_8);
        this.secretKey = new SecretKeySpec(keyBytes, "AES");
    }

    /**
     * Шифрует номер карты алгоритмом AES-256-GCM.
     * Для каждого вызова генерируется новый случайный IV (12 байт).
     * Результат: base64(iv) + ":" + base64(ciphertext+authTag)
     */
    public String encrypt(String cardNumber) {
        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            new SecureRandom().nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_LENGTH, iv));

            byte[] encrypted = cipher.doFinal(cardNumber.getBytes(StandardCharsets.UTF_8));

            String ivBase64 = Base64.getEncoder().encodeToString(iv);
            String dataBase64 = Base64.getEncoder().encodeToString(encrypted);
            return ivBase64 + ":" + dataBase64;
        } catch (Exception e) {
            throw new RuntimeException("Ошибка шифрования номера карты", e);
        }
    }

    /**
     * Расшифровывает номер карты.
     * Ожидает формат base64(iv) + ":" + base64(ciphertext+authTag).
     * Используется только внутри сервиса перед маскированием.
     */
    public String decrypt(String encryptedCardNumber) {
        try {
            String[] parts = encryptedCardNumber.split(":");
            if (parts.length != 2) {
                throw new IllegalArgumentException("Неверный формат зашифрованного номера карты");
            }

            byte[] iv = Base64.getDecoder().decode(parts[0]);
            byte[] encrypted = Base64.getDecoder().decode(parts[1]);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_LENGTH, iv));

            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Ошибка расшифровки номера карты", e);
        }
    }

    /**
     * Маскирует номер карты — показывает только последние 4 цифры.
     * Пример: "1234567890123456" → "**** **** **** 3456"
     */
    public String mask(String cardNumber) {
        String digits = cardNumber.replaceAll("\\s+", "");
        if (digits.length() < 4) {
            throw new IllegalArgumentException("Номер карты слишком короткий для маскирования");
        }
        String lastFour = digits.substring(digits.length() - 4);
        return "**** **** **** " + lastFour;
    }
}
