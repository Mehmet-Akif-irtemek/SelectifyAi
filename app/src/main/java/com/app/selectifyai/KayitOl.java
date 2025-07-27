package com.app.selectifyai;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class KayitOl extends AnaAktivite {
    private static final String ETIKET = KayitOl.class.getSimpleName();
    private static final String KULLANICILAR_KOLEKSIYONU = "kullanicilar";
    private static final String ONBOARDING_PREFS = "onboarding_prefs";
    private static final String ONBOARDING_TAMAMLANDI = "onboarding_tamamlandi";

    private ImageButton googleIleGirisButonu;    private ProgressBar yukleniyorBar;
    private TextView durumMetni;

    private FirebaseAuth kimlikDogrulama;
    private GoogleSignInClient googleGirisIstemcisi;
    private FirebaseFirestore veriTabani;

    private final ActivityResultLauncher<Intent> girisBaslatici = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            sonuc -> {
                yuklenmeyiGizle();
                if (sonuc.getResultCode() == RESULT_OK) {
                    googleGirisSonucunuIsle(sonuc.getData());
                } else {
                    hataGoster(getString(R.string.google_ile_giris_basarisiz));
                }
            }
    );

    @Override
    protected void onCreate(@Nullable Bundle kayitliVeri) {
        super.onCreate(kayitliVeri);
        setContentView(R.layout.register);
        hideSystemUI();

        arayuzleriHazirla();
        firebaseHazirla();
        clickDinleyicileriHazirla();

        mevcutKullaniciKontrol();
    }
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            hideSystemUI(); // Odak geri gelince tekrar uygula
        }
    }

    private void hideSystemUI() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            getWindow().setDecorFitsSystemWindows(false);
            final WindowInsetsController insetsController = getWindow().getInsetsController();
            if (insetsController != null) {
                insetsController.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
                insetsController.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        } else {
            View decorView = getWindow().getDecorView();
            decorView.setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            );
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
    }
    private void arayuzleriHazirla() {
        googleIleGirisButonu = findViewById(R.id.btnIcon);
        yukleniyorBar = findViewById(R.id.progressBar);
        durumMetni = findViewById(R.id.statusText);
    }

    private void firebaseHazirla() {
        kimlikDogrulama = FirebaseAuth.getInstance();
        veriTabani = FirebaseFirestore.getInstance();

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .requestProfile()
                .build();
        googleGirisIstemcisi = GoogleSignIn.getClient(this, gso);
    }

    private void clickDinleyicileriHazirla() {
        googleIleGirisButonu.setOnClickListener(v -> girisBaslat());
    }

    private void mevcutKullaniciKontrol() {
        FirebaseUser mevcutKullanici = kimlikDogrulama.getCurrentUser();
        if (mevcutKullanici != null) {
            // Kullanıcı daha önce giriş yapmış. Firestore'dan onboarding durumunu sorgula:
            yuklenmeyiGoster();
            veriTabani.collection(KULLANICILAR_KOLEKSIYONU)
                    .document(mevcutKullanici.getUid())
                    .get()
                    .addOnSuccessListener(document -> {
                        yuklenmeyiGizle();
                        if (document.exists()) {
                            Boolean onboardingDurumu = document.getBoolean("onboardingTamamlandi");
                            if (onboardingDurumu != null && onboardingDurumu) {
                                anaEkranaYonlendir();
                            } else {
                                onboardingEkraninaYonlendir();
                            }
                        } else {
                            // Kullanıcı hiç kaydolmamışsa onboarding göster
                            onboardingEkraninaYonlendir();
                        }
                    })
                    .addOnFailureListener(e -> {
                        yuklenmeyiGizle();
                        Log.e(ETIKET, "Kullanıcı kontrol hatası", e);
                        hataGoster(getString(R.string.kullanici_bilgileri_kontrol_edilemedi));
                    });
        }
    }

    private void girisBaslat() {
        if (!agBaglantisiVarMi()) {
            hataGoster(getString(R.string.internet_baglantisi_yok));
            return;
        }
        yuklenmeyiGoster();
        girisBaslatici.launch(googleGirisIstemcisi.getSignInIntent());
    }

    private void googleGirisSonucunuIsle(@Nullable Intent data) {
        Task<GoogleSignInAccount> gorev = GoogleSignIn.getSignedInAccountFromIntent(data);
        try {
            GoogleSignInAccount hesap = gorev.getResult(ApiException.class);
            if (hesap == null || hesap.getIdToken() == null || hesap.getEmail() == null) {
                hataGoster(getString(R.string.google_hesabi_bilgileri_eksik));
                return;
            }
            firebaseIleDogrula(hesap);
        } catch (ApiException e) {
            Log.w(ETIKET, "Google ile giriş başarısız.", e);
            hataGoster(getString(R.string.google_ile_giris_basarisiz));
        }
    }

    private void firebaseIleDogrula(@NonNull GoogleSignInAccount hesap) {
        yuklenmeyiGoster();
        AuthCredential kimlik = GoogleAuthProvider.getCredential(hesap.getIdToken(), null);

        kimlikDogrulama.signInWithCredential(kimlik)
                .addOnCompleteListener(this, gorev -> {
                    if (gorev.isSuccessful()) {
                        FirebaseUser kullanici = kimlikDogrulama.getCurrentUser();
                        if (kullanici != null) {
                            kullaniciProfiliniKaydet(kullanici, hesap);
                        } else {
                            yuklenmeyiGizle();
                            hataGoster(getString(R.string.firebase_kimlik_dogrulama_basarisiz));
                        }
                    } else {
                        yuklenmeyiGizle();
                        Log.w(ETIKET, "Kimlik doğrulamada hata", gorev.getException());
                        hataGoster(getString(R.string.firebase_kimlik_dogrulama_basarisiz));
                    }
                });
    }

    private void kullaniciProfiliniKaydet(@NonNull FirebaseUser kullanici, @NonNull GoogleSignInAccount hesap) {
        veriTabani.collection(KULLANICILAR_KOLEKSIYONU)
                .document(kullanici.getUid())
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        yuklenmeyiGizle();
                        Boolean onboardingDurumu = document.getBoolean("onboardingTamamlandi");
                        if (onboardingDurumu != null && onboardingDurumu) {
                            anaEkranaYonlendir();
                            SharedPreferences prefs = getSharedPreferences("login_prefs", MODE_PRIVATE);
                            prefs.edit().putBoolean("is_logged_in", true).apply();
                        } else {
                            onboardingEkraninaYonlendir();
                        }
                    } else {
                        yeniKullaniciKaydet(kullanici, hesap);
                    }
                })
                .addOnFailureListener(e -> {
                    yuklenmeyiGizle();
                    hataGoster(getString(R.string.kullanici_bilgileri_kontrol_edilemedi));
                });
    }

    private void yeniKullaniciKaydet(@NonNull FirebaseUser kullanici, @NonNull GoogleSignInAccount hesap) {
        Map<String, Object> kullaniciVerisi = kullaniciVerisiOlustur(kullanici, hesap);

        // Firestore'a onboarding tamamlandı alanını false olarak ekle!
        kullaniciVerisi.put(getString(R.string.onboarding_tamamlandi_field), false);

        veriTabani.collection(KULLANICILAR_KOLEKSIYONU)
                .document(kullanici.getUid())
                .set(kullaniciVerisi)
                .addOnSuccessListener(aVoid -> {
                    yuklenmeyiGizle();
                    onboardingEkraninaYonlendir();
                })
                .addOnFailureListener(e -> {
                    yuklenmeyiGizle();
                    tekrarDeneDialogGoster(() -> yeniKullaniciKaydet(kullanici, hesap));
                });
    }

    @NonNull
    private Map<String, Object> kullaniciVerisiOlustur(@NonNull FirebaseUser kullanici, @NonNull GoogleSignInAccount hesap) {
        Map<String, Object> veri = new HashMap<>();
        veri.put(getString(R.string.uid), kullanici.getUid());
        veri.put(getString(R.string.eposta), hesap.getEmail());
        veri.put(getString(R.string.kullanici_adi_field), hesap.getDisplayName() != null ? hesap.getDisplayName() : "");
        veri.put(getString(R.string.kayit_tarihi), System.currentTimeMillis());

        if (hesap.getPhotoUrl() != null) {
            veri.put(getString(R.string.profil_foto_url), hesap.getPhotoUrl().toString());
        }
        // Üyelik tipi ekle (şimdilik herkes free)
        veri.put("membershipType", "free");
        return veri;
    }

    private void yuklenmeyiGoster() {
        googleIleGirisButonu.setEnabled(false);
        yukleniyorBar.setVisibility(View.VISIBLE);
        durumMetni.setVisibility(View.VISIBLE);
    }

    private void yuklenmeyiGizle() {
        googleIleGirisButonu.setEnabled(true);
        yukleniyorBar.setVisibility(View.GONE);
        durumMetni.setVisibility(View.GONE);
    }

    private void hataGoster(String mesaj) {
        Toast.makeText(this, mesaj, Toast.LENGTH_SHORT).show();
    }

    private void tekrarDeneDialogGoster(Runnable tekrarDeneAksiyonu) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.hata)
                .setMessage(R.string.x)
                .setPositiveButton(R.string.dene, (dialog, which) -> tekrarDeneAksiyonu.run())
                .setNegativeButton(R.string.vazgeç, null)
                .setCancelable(false)
                .show();
    }

    private void onboardingEkraninaYonlendir() {
        startActivity(new Intent(this, OnboardingActivity.class));
        finish();
    }

    private void anaEkranaYonlendir() {
        startActivity(new Intent(this, AnaEkran.class));
        finish();
    }

    private boolean agBaglantisiVarMi() {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            NetworkInfo aktifAg = connectivityManager.getActiveNetworkInfo();
            return aktifAg != null && aktifAg.isConnected();
        }
        return false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        googleGirisIstemcisi = null;
    }
}