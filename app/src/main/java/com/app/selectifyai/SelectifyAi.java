// app/src/main/java/com/app/selectifyai/SelectifyAi.java
package com.app.selectifyai;

import android.app.Application;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.util.Log;
import androidx.appcompat.app.AppCompatDelegate;
import com.google.android.gms.ads.MobileAds;
import com.google.firebase.FirebaseApp;
import java.util.Locale;

public class SelectifyAi extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        AppCompatDelegate.setDefaultNightMode(
            AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        );
        // Bu satır kesinlikle çalışmalı:
        uygulaKayitliAyarlar();
        FirebaseApp.initializeApp(this);
        Log.d("KEY TEST", "Groq: " + KeyDecryptor.getGroqKey());
        Log.d("KEY TEST", "Weather: " + KeyDecryptor.getWeatherKey());
        Log.d("KEY TEST", "Firebase: " + KeyDecryptor.getFirebaseKey());
    }

    private void uygulaKayitliAyarlar() {
        SharedPreferences prefs = getSharedPreferences("ayarlar", MODE_PRIVATE);

        // Tema uygula
        if (!prefs.contains("koyuMod")) {
            AppCompatDelegate.setDefaultNightMode(
                AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            );
        } else {
            boolean koyuMod = prefs.getBoolean("koyuMod", false);
            AppCompatDelegate.setDefaultNightMode(
                koyuMod
                    ? AppCompatDelegate.MODE_NIGHT_YES
                    : AppCompatDelegate.MODE_NIGHT_NO
            );
        }

        // Dil uygula

    }
}
