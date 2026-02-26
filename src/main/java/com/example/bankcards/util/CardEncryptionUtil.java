package com.example.bankcards.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

/**
 * Утилита для шифрования и маскирования номеров банковских карт.
 *
 * Номер карты никогда не хранится в открытом виде в базе данных.
 * При сохранении — шифруется (AES-256).
 * При отображении пользователю — маскируется: **** **** **** 1234.
 */
@Component
public class CardEncryptionUtil {

    private final SecretKeySpec secretKey;

    public CardEncryptionUtil(@Value("${app.encryption.key}") String key) {
        // Ключ должен быть ровно 32 символа для AES-256
        byte[] keyBytes = key.getBytes();
        this.secretKey = new SecretKeySpec(keyBytes, "AES");
    }

    /**
     * Шифрует номер карты алгоритмом AES.
     * Результат кодируется в Base64 для хранения в БД как строка.
     */
    public String encrypt(String cardNumber) {
        try {
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            byte[] encrypted = cipher.doFinal(cardNumber.getBytes());
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            throw new RuntimeException("Ошибка шифрования номера карты", e);
        }
    }

    /**
     * Расшифровывает номер карты из Base64 строки.
     * Используется только внутри сервиса перед маскированием.
     */
    public String decrypt(String encryptedCardNumber) {
        try {
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            byte[] decoded = Base64.getDecoder().decode(encryptedCardNumber);
            return new String(cipher.doFinal(decoded));
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
        String lastFour = digits.substring(digits.length() - 4);
        return "**** **** **** " + lastFour;
    }
}
