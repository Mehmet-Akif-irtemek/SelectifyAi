package com.app.selectifyai;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.SwitchCompat;

import com.google.firebase.auth.FirebaseAuth;

public class Ayarlar extends AnaAktivite {

    private SwitchCompat switchTema;
    private Spinner spinnerDil;
    private Button btnLogout;
    private String[] diller;
    private String seciliKod = "tr";

    private final String[] dilKodlari = {
            "tr", "en", "de", "es", "fr",
            "ru", "zh", "ja", "ko", "pt",
            "it", "hi"
    };

    private void setLocale(android.content.Context context, String langCode) {
        java.util.Locale locale = new java.util.Locale(langCode);
        java.util.Locale.setDefault(locale);
        android.content.res.Configuration config = new android.content.res.Configuration();
        config.setLocale(locale);
        context.getResources().updateConfiguration(config, context.getResources().getDisplayMetrics());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ayarlar);
        hideSystemUI();

        switchTema = findViewById(R.id.switchTema);
        spinnerDil = findViewById(R.id.spinnerDil);
        btnLogout = findViewById(R.id.btnLogout);

        // Dil listesi
        diller = new String[]{
                getString(R.string.language_turkish),
                getString(R.string.language_english),
                getString(R.string.language_german),
                getString(R.string.language_spanish),
                getString(R.string.language_french),
                getString(R.string.language_russian),
                getString(R.string.language_chinese),
                getString(R.string.language_japanese),
                getString(R.string.language_korean),
                getString(R.string.language_portuguese),
                getString(R.string.language_italian),
                getString(R.string.language_hindi)
        };

        SharedPreferences prefs = getSharedPreferences(getString(R.string.ayarlar_prefs), MODE_PRIVATE);
        boolean koyuMod = prefs.getBoolean(getString(R.string.koyu_mod), false);
        switchTema.setChecked(koyuMod);

        switchTema.setOnCheckedChangeListener((buttonView, isChecked) -> {
            AppCompatDelegate.setDefaultNightMode(
                    isChecked ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
            );
            prefs.edit().putBoolean(getString(R.string.koyu_mod), isChecked).apply();
        });

        // Spinner adaptörü (beyaz textli)
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                this,
                android.R.layout.simple_spinner_item,
                diller
        ) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                ((TextView) view).setTextColor(android.graphics.Color.WHITE); // Seçilen
                return view;
            }

            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                View view = super.getDropDownView(position, convertView, parent);
                ((TextView) view).setTextColor(android.graphics.Color.WHITE); // Açılır liste
                view.setBackgroundColor(android.graphics.Color.parseColor("#6372F2"));
                return view;
            }
        };
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerDil.setAdapter(adapter);

        // Mevcut dil seçimini ayarla
        seciliKod = prefs.getString(getString(R.string.dil), "tr");
        setLocale(this, seciliKod); // Dil değişikliği için setLocale çağrısı
        for (int i = 0; i < dilKodlari.length; i++) {
            if (dilKodlari[i].equals(seciliKod)) {
                spinnerDil.setSelection(i, false);
                break;
            }
        }

        spinnerDil.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                String yeniKod = dilKodlari[position];
                if (!yeniKod.equals(seciliKod)) {
                    seciliKod = yeniKod;
                    dilDegistir(yeniKod);
                }
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });

        btnLogout.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Toast.makeText(this, getString(R.string.logout_success), Toast.LENGTH_SHORT).show();
            startActivity(new Intent(Ayarlar.this, AnaEkran.class));
            finish();
        });
    }

    private void dilDegistir(String dilKodu) {
        SharedPreferences prefs = getSharedPreferences("ayarlar", MODE_PRIVATE);
        prefs.edit().putString("dil", dilKodu).apply();

        // Dil değişikliğini doğrudan uygula
        setLocale(this, dilKodu);

        // Activity'yi yeniden oluştur
        recreate();

        // Ana ekrana git ve tüm activity stack'i temizle
        Intent intent = new Intent(this, AnaEkran.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    private void hideSystemUI() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            getWindow().setDecorFitsSystemWindows(false);
            final WindowInsetsController insetsController = getWindow().getInsetsController();
            if (insetsController != null) {
                insetsController.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
                insetsController.setSystemBarsBehavior(
                        WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                );
            }
        } else {
            View decorView = getWindow().getDecorView();
            decorView.setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY |
                            View.SYSTEM_UI_FLAG_FULLSCREEN |
                            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            );
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
    }
}