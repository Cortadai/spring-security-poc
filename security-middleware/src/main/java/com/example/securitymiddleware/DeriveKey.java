package com.example.securitymiddleware;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.spec.KeySpec;
import java.util.Base64;

public class DeriveKey {

    public static void main(String[] args) throws Exception {
        String password = "mi-contraseña-segura";
        byte[] salt = "un-salt-aleatorio".getBytes();

        KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, 65536, 256); // 256 bits = 32 bytes
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        byte[] key = factory.generateSecret(spec).getEncoded();

        if (key.length != 32) {
            System.out.println("❌ Error: La clave no tiene 32 bytes. Longitud actual: " + key.length);
        } else {
            System.out.println("✅ Correcto: La clave tiene exactamente 32 bytes.");
        }

        String encodedKey = Base64.getEncoder().encodeToString(key);
        System.out.println("🔐 Clave Base64 para application.properties: " + encodedKey);
    }
}
