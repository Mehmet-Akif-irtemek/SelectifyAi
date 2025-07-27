package com.app.selectifyai;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Arrays;

public class OnboardingActivity extends AnaAktivite {
    private static final String ONBOARDING_PREFS = "onboarding_prefs";
    private static final String ONBOARDING_TAMAMLANDI = "onboarding_tamamlandi";

    private ViewPager2 sayfaGecis;
    private Button ileriButon;
    private OnboardingAdapter adaptorum;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding);

        sayfaGecis = findViewById(R.id.sayfaGecis);
        ileriButon = findViewById(R.id.ileriButon);

        adaptorum = new OnboardingAdapter(this, Arrays.asList(
                new SorumlulukFragment(),
                new YasFragment(),
                new KonumIzniFragment()
        ));
        sayfaGecis.setAdapter(adaptorum);

        // Başlangıçta buton pasif
        ileriButon.setEnabled(false);
        ileriButon.setAlpha(0.5f);

        ileriButon.setOnClickListener(v -> {
            int idx = sayfaGecis.getCurrentItem();
            if (idx < adaptorum.getItemCount() - 1) {
                sayfaGecis.setCurrentItem(idx + 1, true);
                ileriButon.setAlpha(0.5f);
            } else {
                onboardingTamamlandiOlarakIsaretle();
                startActivity(new Intent(this, ilkekran.class));
                finish();
            }
        });
    }

    // Fragment’ten çağrılır
    public void ileriButonAktifMi(boolean aktif) {
        ileriButon.setEnabled(aktif);
        ileriButon.setAlpha(aktif ? 1f : 0.5f);
    }

    private void onboardingTamamlandiOlarakIsaretle() {
        SharedPreferences prefs = getSharedPreferences(ONBOARDING_PREFS, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(ONBOARDING_TAMAMLANDI, true).apply();

        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseFirestore.getInstance()
                .collection("kullanicilar")
                .document(uid)
                .update("onboardingTamamlandi", true);
    }
}