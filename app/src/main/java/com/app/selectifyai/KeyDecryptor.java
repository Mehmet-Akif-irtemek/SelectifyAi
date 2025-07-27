package com.app.selectifyai;

import android.util.Base64;
import java.nio.charset.StandardCharsets;

public class KeyDecryptor {
    private static final byte XOR_KEY = 'K';

    public static String decodeEncryptedKey(String encryptedBase64) {
        try {
            byte[] decoded = Base64.decode(encryptedBase64, Base64.NO_WRAP);
            for (int i = 0; i < decoded.length; i++) {
                decoded[i] ^= XOR_KEY;
            }
            return new String(decoded, StandardCharsets.UTF_8);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String getGroqKey() {
        return decodeEncryptedKey(ApiKeyProvider.getEncryptedGroqKey());
    }

    public static String getWeatherKey() {
        return decodeEncryptedKey(ApiKeyProvider.getEncryptedWeatherKey());
    }

    public static String getFirebaseKey() {
        return decodeEncryptedKey(ApiKeyProvider.getEncryptedFirebaseKey());
    }
}