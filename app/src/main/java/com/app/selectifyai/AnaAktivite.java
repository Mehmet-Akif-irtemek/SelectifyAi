// app/src/main/java/com/app/selectifyai/BaseActivity.java
package com.app.selectifyai;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Locale;

public class AnaAktivite extends AppCompatActivity {
    
    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(updateBaseContextLocale(newBase));
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Dil ayarını tekrar kontrol et
        applyLanguageSettings();
    }

    private Context updateBaseContextLocale(Context context) {
        try {
            SharedPreferences prefs = context.getSharedPreferences("ayarlar", MODE_PRIVATE);
            String dilKodu = prefs.getString("dil", null); // default null

            if (dilKodu == null) {
                // Cihaz dili neyse onu kullan
                return context;
            }

            Locale locale = new Locale(dilKodu);
            Locale.setDefault(locale);

            Configuration config = new Configuration();
            config.setLocale(locale);

            return context.createConfigurationContext(config);
        } catch (Exception e) {
            return context;
        }
    }
    private void applyLanguageSettings() {
        try {
            SharedPreferences prefs = getSharedPreferences("ayarlar", MODE_PRIVATE);
            String dilKodu = prefs.getString("dil", null);            Locale locale = new Locale(dilKodu);
            Locale.setDefault(locale);
            
            Configuration config = getResources().getConfiguration();
            config.setLocale(locale);
            
            getResources().updateConfiguration(config, getResources().getDisplayMetrics());
        } catch (Exception e) {
            // Hata durumunda varsayılan dili kullan
        }
    }
}