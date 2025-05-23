package com.example.securitymiddleware.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Base64;

@Component
@RequiredArgsConstructor
public class EncryptionService {

    @Value("${jwt.encryption.key}")
    private String base64EncodedKey;

    private SecretKeySpec keySpec;

    @PostConstruct
    public void init() {
        byte[] keyBytes = Base64.getDecoder().decode(base64EncodedKey);
        if (keyBytes.length != 32) {
            throw new IllegalArgumentException("La clave decodificada debe tener exactamente 32 bytes");
        }
        this.keySpec = new SecretKeySpec(keyBytes, "AES");
    }

    public String encrypt(String plainText) {
        try {
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            byte[] iv = new byte[16];
            new SecureRandom().nextBytes(iv);
            IvParameterSpec ivSpec = new IvParameterSpec(iv);

            cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);
            byte[] encrypted = cipher.doFinal(plainText.getBytes());

            byte[] encryptedIvAndText = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, encryptedIvAndText, 0, iv.length);
            System.arraycopy(encrypted, 0, encryptedIvAndText, iv.length, encrypted.length);

            return Base64.getEncoder().encodeToString(encryptedIvAndText);
        } catch (Exception e) {
            throw new RuntimeException("Error encriptando", e);
        }
    }

    public String decrypt(String encryptedIvText) {
        try {
            byte[] encryptedIvAndText = Base64.getDecoder().decode(encryptedIvText);
            byte[] iv = new byte[16];
            byte[] encrypted = new byte[encryptedIvAndText.length - 16];

            System.arraycopy(encryptedIvAndText, 0, iv, 0, 16);
            System.arraycopy(encryptedIvAndText, 16, encrypted, 0, encrypted.length);

            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, keySpec, new IvParameterSpec(iv));
            byte[] original = cipher.doFinal(encrypted);

            return new String(original);
        } catch (Exception e) {
            throw new RuntimeException("Error desencriptando", e);
        }
    }
}
