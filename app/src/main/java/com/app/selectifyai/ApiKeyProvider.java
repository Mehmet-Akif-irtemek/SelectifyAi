package com.app.selectifyai;

public class ApiKeyProvider {
    static {
        System.loadLibrary("native-lib");
    }
    public static native String getEncryptedGroqKey();
    public static native String getEncryptedWeatherKey();
    public static native String getEncryptedFirebaseKey();
}