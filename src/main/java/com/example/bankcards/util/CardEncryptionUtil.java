package com.example.bankcards.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

@Component
public class CardEncryptionUtil {

    private final SecretKeySpec secretKey;

    public CardEncryptionUtil(@Value("${app.encryption.key}") String key) {
        byte[] keyBytes = key.getBytes();
        this.secretKey = new SecretKeySpec(keyBytes, "AES");
    }

    public String encrypt(String cardNumber) {
        try {
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            byte[] encrypted = cipher.doFinal(cardNumber.getBytes());
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            throw new RuntimeException("Failed to encrypt card number", e);
        }
    }

    public String decrypt(String encryptedCardNumber) {
        try {
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            byte[] decoded = Base64.getDecoder().decode(encryptedCardNumber);
            return new String(cipher.doFinal(decoded));
        } catch (Exception e) {
            throw new RuntimeException("Failed to decrypt card number", e);
        }
    }

    public String mask(String cardNumber) {
        String digits = cardNumber.replaceAll("\\s+", "");
        String lastFour = digits.substring(digits.length() - 4);
        return "**** **** **** " + lastFour;
    }
}
