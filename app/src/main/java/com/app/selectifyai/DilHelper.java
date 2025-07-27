package com.app.selectifyai;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;

import java.util.Locale;

public class DilHelper {
    public static Context wrap(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("ayarlar", Context.MODE_PRIVATE);
        String dilKodu = prefs.getString("dil", "tr");

        Locale locale = new Locale(dilKodu);
        Locale.setDefault(locale);

        Configuration config = new Configuration();
        config.setLocale(locale);

        return context.createConfigurationContext(config);
    }
}