package com.app.selectifyai;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import com.google.android.gms.location.LocationRequest;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.airbnb.lottie.LottieAnimationView;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.app.selectifyai.AnaAktivite;
import com.bumptech.glide.Glide;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.json.JSONArray;
import org.json.JSONObject;

public class AnaEkran extends AnaAktivite {

    private View dimView; // BU SATIRI EKLEYİN
    private boolean hakYok = false;
    private static final String TAG = "AnaEkran";
    private static final int GUNLUK_AI_ONERI_HAKKI = 50;
    private LinearLayout reklamPaneli;
    private LinearLayout panelDavetPremium;
    private Button btnPremium;
    private TextView tvHakSayisi;

    // UI Elemanları
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private Toolbar toolbar;
    private RecyclerView recyclerViewPoi;
    private FloatingActionButton fabAiChat;
    private Button btnYemek, btnCalisma, btnAlisveris;
    private Button btnDavetEt;
    private LottieAnimationView lottieLoading;
    private TextView textView17;
    private TextView textView19;

    // Diğer Değişkenler
    private List<PoiModel> poiList;
    private PoiAdapter poiAdapter;
    private FirebaseFirestore db;
    private FirebaseUser firebaseUser;
    private FusedLocationProviderClient fusedLocationClient;
    private Location kullaniciKonumu = null;
    private String mevcutIlce = null;
    private double guncelSicaklik = 20;
    private String guncelHavaDurumu = "";
    private static final int KONUM_IZNI_KODU = 100;
    private ZarAiManager zarAiManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Oto login kontrolü: FirebaseAuth ile
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(
                this
            );
            if (account != null && account.getIdToken() != null) {
                AuthCredential credential = GoogleAuthProvider.getCredential(
                    account.getIdToken(),
                    null
                );
                FirebaseAuth.getInstance()
                    .signInWithCredential(credential)
                    .addOnCompleteListener(this, task -> {
                        if (task.isSuccessful()) {
                            recreate(); // Otomatik login başarılı, AnaEkran'ı yeniden başlat
                        } else {
                            Intent intent = new Intent(this, ilkekran.class);
                            intent.addFlags(
                                Intent.FLAG_ACTIVITY_CLEAR_TOP |
                                Intent.FLAG_ACTIVITY_NEW_TASK
                            );
                            startActivity(intent);
                            finish();
                        }
                    });
                return;
            } else {
                Intent intent = new Intent(this, ilkekran.class);
                intent.addFlags(
                    Intent.FLAG_ACTIVITY_CLEAR_TOP |
                    Intent.FLAG_ACTIVITY_NEW_TASK
                );
                startActivity(intent);
                finish();
                return;
            }
        }

        setContentView(R.layout.anaekran);
        hideSystemUI();

        FirebaseApp.initializeApp(this);

        SharedPreferences prefs = getSharedPreferences(
            "login_prefs",
            MODE_PRIVATE
        );
        boolean isLoggedIn = prefs.getBoolean("is_logged_in", false);

        if (!isLoggedIn) {
            Intent intent = new Intent(this, ilkekran.class);
            intent.addFlags(
                Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK
            );
            startActivity(intent);
            finish();
            return;
        }
        db = FirebaseFirestore.getInstance();
        firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(
            this
        );
        zarAiManager = new ZarAiManager(this, false);

        initializeUIComponents();
        setupListeners();
        modBildirimAlarmiKur();
        modEmojiSecimiGoster();
        kullaniciBilgileriniGuncelle();

        // Davet ve Premium Paneli
        panelDavetPremium = findViewById(R.id.panelDavetPremium);
        btnDavetEt = findViewById(R.id.btnDavetEt);
        btnPremium = findViewById(R.id.btnPremium);

        btnDavetEt.setOnClickListener(v -> {
            String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
            
            // Play Store'da olmadığı için alternatif yöntemler
            
            // 1. APK indirme linki (GitHub Releases, Firebase App Distribution, vs.)
            String apkDownloadLink = "https://github.com/yourusername/SelectifyAI/releases/latest/download/app-release.apk";
            
            // 2. Deep link (uygulama yüklüyse çalışır)
            String deepLink = "https://yourapp.com/invite?ref=" + uid;
            
            // 3. App scheme link 
            String appSchemeLink = "selectifyai://invite?ref=" + uid;
            
            // 4. Web sitesi linki (uygulamayı tanıtan sayfa)
            String websiteLink = "https://yourwebsite.com/download?ref=" + uid;
            
            String referralCode = uid.substring(0, 8).toUpperCase();
            String shareText = getString(R.string.share_invite_text, referralCode);            
            Intent sendIntent = new Intent();
            sendIntent.setAction(Intent.ACTION_SEND);
            sendIntent.putExtra(Intent.EXTRA_TEXT, shareText);
            sendIntent.setType("text/plain");
            startActivity(Intent.createChooser(sendIntent, getString(R.string.share_selectify)));
            
            // Kullanıcıya bilgi ver
            Toast.makeText(this, getString(R.string.invite_shared_message, uid.substring(0, 8).toUpperCase()), Toast.LENGTH_LONG).show();
        });
        // Başlangıçta paneli gizle
        panelDavetPremium.setVisibility(View.GONE);

        // Davet ile gelen link kontrolü (ilk açılışta)
        checkInviteLink();

        if (
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                new String[] { Manifest.permission.ACCESS_FINE_LOCATION },
                KONUM_IZNI_KODU
            );
        } else {
            konumuAl();
        }

        if (firebaseUser != null) {
            hakGoster();
        }
    }

    private void initializeUIComponents() {
        // Ana UI
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.navigation_view);
        fabAiChat = findViewById(R.id.floatingActionButton);
        dimView = findViewById(R.id.dimView); // BU SATIRI EKLEYİN

        // Kategori Butonları
        btnYemek = findViewById(R.id.button4);
        btnCalisma = findViewById(R.id.button10);
        btnAlisveris = findViewById(R.id.button11);

        // Hak Sistemi UI
        reklamPaneli = findViewById(R.id.reklamPaneli);

        btnPremium = findViewById(R.id.btnPremium);
        tvHakSayisi = findViewById(R.id.tvHakSayisi);

        // RecyclerView
        recyclerViewPoi = findViewById(R.id.recyclerViewPoi);
        lottieLoading = findViewById(R.id.lottieLoading);
        poiList = new ArrayList<>();
        poiAdapter = new PoiAdapter(poiList);
        recyclerViewPoi.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewPoi.setAdapter(poiAdapter);
        recyclerViewPoi.setVisibility(View.GONE);
        lottieLoading.setVisibility(View.GONE);
        textView17 = findViewById(R.id.textView17);
        textView19 = findViewById(R.id.textView19);
    }

    private void setupListeners() {
        // Kategori Butonları
        btnYemek.setOnClickListener(v -> {
            kategoriSec("yemek");
        });
        btnCalisma.setOnClickListener(v -> {
            kategoriSec("calisma");
        });
        btnAlisveris.setOnClickListener(v -> {
            kategoriSec("alisveris");
        });

        // FAB ve Diğer Butonlar
        fabAiChat.setOnClickListener(v ->
            startActivity(new Intent(this, AiChatActivity.class))
        );
        findViewById(R.id.button3).setOnClickListener(v ->
            startActivity(new Intent(AnaEkran.this, ZarAiActivity.class))
        );
        findViewById(R.id.button2).setOnClickListener(v ->
            startActivity(new Intent(AnaEkran.this, ZarActivity.class))
        );

        // Reklam Paneli

        btnPremium.setOnClickListener(v -> {
            // Premium yerine referral kod dialog aç
            showReferralCodeDialog();
        });

        // Navigation Drawer
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
            this,
            drawerLayout,
            toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        );
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();
        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_logout) {
                FirebaseAuth.getInstance().signOut();
                startActivity(new Intent(this, KayitOl.class)); // Giris ekranina yonlendir
                finish();
            } else if (id == R.id.nav_profile) {
                startActivity(new Intent(this, Profil.class));
            } else if (id == R.id.zarat) {
                startActivity(new Intent(this, ZarActivity.class));
            } else if (id == R.id.ayarlar) {
                startActivity(new Intent(this, Ayarlar.class));
            }
            drawerLayout.closeDrawers();
            return true;
        });

        // Bottom Navigation
        BottomNavigationView bottomNavigationView = findViewById(
            R.id.bottom_navigation
        );
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_tasks) {
                startActivity(new Intent(this, TodoActivity.class));
            } else if (id == R.id.nav_settings) {
                startActivity(new Intent(this, Profil.class));
            }
            return true;
        });

        // Mod Emoji seçimi
        LinearLayout modEmojiLayout = findViewById(R.id.modEmojiLayout);
        for (int i = 0; i < modEmojiLayout.getChildCount(); i++) {
            View emojiView = modEmojiLayout.getChildAt(i);
            emojiView.setOnClickListener(v -> {
                String emojiText = ((TextView) v).getText().toString();
                String uid = Objects.requireNonNull(firebaseUser).getUid();
                String tarih = new SimpleDateFormat(
                    "yyyy-MM-dd",
                    Locale.getDefault()
                ).format(new Date());
                Map<String, Object> modData = new HashMap<>();
                modData.put("emoji", emojiText);
                modData.put("timestamp", FieldValue.serverTimestamp());
                db
                    .collection("modlar")
                    .document(uid)
                    .collection("gunluk")
                    .document(tarih)
                    .set(modData)
                    .addOnSuccessListener(unused -> {
                        Toast.makeText(
                            this,
                            getString(R.string.mod_kaydedildi, emojiText),
                            Toast.LENGTH_SHORT
                        ).show();
                        SharedPreferences prefs = getSharedPreferences(
                            "mod_pref",
                            MODE_PRIVATE
                        );
                        prefs
                            .edit()
                            .putString(
                                "mod_alindi_tarihi",
                                new SimpleDateFormat(
                                    "yyyyMMdd",
                                    Locale.getDefault()
                                ).format(new Date())
                            )
                            .apply();
                        modEmojiLayout.setVisibility(View.GONE);
                    })
                    .addOnFailureListener(e ->
                        Toast.makeText(
                            this,
                            getString(R.string.kayit_basarisiz),
                            Toast.LENGTH_SHORT
                        ).show()
                    );
                if (textView19 != null) textView19.setVisibility(View.GONE);
            });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (firebaseUser != null) {
            kullaniciBilgileriniGuncelle();
            hakGoster();
        }
        // Start.io ödüllü reklamını onResume'da yüklemeyi dene
    }
    
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent); // Yeni intent'i kaydet
        
        // Yeni davet linki gelmiş olabilir
        if (firebaseUser != null) {
            checkInviteLink();
        }
    }

    private void hakArttir(int miktar) {
        if (miktar <= 0) {
            Toast.makeText(
                this,
                getString(R.string.invalid_rights_amount),
                Toast.LENGTH_SHORT
            ).show();
            return;
        }

        String uid = Objects.requireNonNull(firebaseUser).getUid();
        String tarih = new SimpleDateFormat(
            "yyyyMMdd",
            Locale.getDefault()
        ).format(new Date());
        DocumentReference hakRef = db
            .collection("kullanicilar")
            .document(uid)
            .collection("groq_limit")
            .document(tarih);

        hakRef
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                // Mevcut kullanım sayısını al, belge yoksa 0 olarak kabul et
                long mevcutKullanim = documentSnapshot.exists()
                    ? (documentSnapshot.getLong("count") != null
                            ? documentSnapshot.getLong("count")
                            : 0)
                    : 0;

                // Yeni kullanım sayısını hesapla (negatif olmamasını sağla)
                long yeniKullanim = Math.max(0, mevcutKullanim - miktar);

                // Firestore'a kaydet
                hakRef
                    .set(
                        Collections.singletonMap("count", yeniKullanim),
                        SetOptions.merge()
                    )
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(
                            this,
                            getString(R.string.rights_earned_message, miktar),
                            Toast.LENGTH_SHORT
                        ).show();
                        hakGoster();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(
                            this,
                            getString(R.string.error_adding_rights, e.getMessage()),
                            Toast.LENGTH_SHORT
                        ).show();
                    });
            })
            .addOnFailureListener(e -> {
                Toast.makeText(
                    this,
                    getString(R.string.error_getting_rights_info, e.getMessage()),
                    Toast.LENGTH_SHORT
                ).show();
            });
    }

    @SuppressLint("StringFormatInvalid")
    private void hakGoster() {
        if (firebaseUser == null) return;

        String uid = firebaseUser.getUid();
        String tarih = new SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(new Date());
        DocumentReference hakRef = db
            .collection("kullanicilar")
            .document(uid)
            .collection("groq_limit")
            .document(tarih);
            
        hakRef.get().addOnSuccessListener(doc -> {
            if (isFinishing() || (android.os.Build.VERSION.SDK_INT >= 17 && isDestroyed())) return;
            
            runOnUiThread(() -> {
                if (tvHakSayisi == null) return;
                
                try {
                    // Maksimum günlük hakkı al (varsayılan 50, davet ile artabilir)
                    Long maxDaily = doc.getLong("gunluk_max_hak");
                    long gunlukMax = maxDaily != null ? maxDaily : GUNLUK_AI_ONERI_HAKKI;
                    
                    // Mevcut kullanım sayısını al
                    long kullanilan = doc.exists() && doc.contains("count") ? doc.getLong("count") : 0;
                    
                    // Kalan hakkı hesapla
                    long kalan = Math.max(0, gunlukMax - kullanilan);
                    
                    // Hak sayısını göster
                    tvHakSayisi.setText(getString(R.string.remaining_rights, (int)kalan, (int)gunlukMax));
                    hakYok = kalan <= 0;
                    
                    Log.d(TAG, "Hak durumu: " + kalan + "/" + gunlukMax + " (Kullanılan: " + kullanilan + ")");
                    
                } catch (Exception e) {
                    // Format hatası durumunda alternatif metin göster
                    long kullanilan = doc.exists() && doc.contains("count") ? doc.getLong("count") : 0;
                    long kalan = Math.max(0, GUNLUK_AI_ONERI_HAKKI - kullanilan);
                    tvHakSayisi.setText(getString(R.string.remaining_rights, (int)kalan, GUNLUK_AI_ONERI_HAKKI));
                    Log.e(TAG, "Hak gösterme hatası: " + e.getMessage());
                }
            });
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Hak bilgisi alınamadı: " + e.getMessage());
        });
    }

    private void kategoriSec(String kategori) {
        // Önce UI bileşenlerini güncelle
        recyclerViewPoi.setVisibility(View.GONE);
        lottieLoading.setVisibility(View.VISIBLE);

        // --- GÜNLÜK LİMİT KONTROLÜ BURADA YAPILACAK ---
        String uid = Objects.requireNonNull(firebaseUser).getUid();
        String tarih = new java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.getDefault()).format(new java.util.Date());
        com.google.firebase.firestore.DocumentReference limitRef = db.collection("kullanicilar").document(uid)
                .collection("groq_limit").document(tarih);

        limitRef.get().addOnSuccessListener(doc -> {
            // Maksimum günlük hakkı al (davet ile artabilir)
            Long maxDaily = doc.getLong("gunluk_max_hak");
            long gunlukMax = maxDaily != null ? maxDaily : GUNLUK_AI_ONERI_HAKKI;
            
            long kullanilan = doc.exists() && doc.contains("count") ? doc.getLong("count") : 0;
            
            if (kullanilan >= gunlukMax) {
                // Günlük limit dolu
                panelDavetPremium.setVisibility(View.VISIBLE);
                lottieLoading.setVisibility(View.GONE);
                return;
            }

            // --- MEVCUT KATEGORİ İŞLEMİ BURAYA TAŞINDI ---
            if (panelDavetPremium == null || dimView == null) return;
            panelDavetPremium.setVisibility(View.GONE);
            dimView.setVisibility(View.GONE);

            if (kullaniciKonumu != null) {
                poiKategoriyeGoreListele(
                        kullaniciKonumu.getLatitude(),
                        kullaniciKonumu.getLongitude(),
                        kategori,
                        3000,
                        false
                );
                // Hak azalt
                limitRef.set(Collections.singletonMap("count", kullanilan + 1), SetOptions.merge())
                        .addOnSuccessListener(aVoid -> hakGoster());
            } else {
                konumuAl();
                Toast.makeText(
                        this,
                        getString(R.string.konum_aliniyor),
                        Toast.LENGTH_SHORT
                ).show();
                lottieLoading.setVisibility(View.GONE);
            }
        });
    }

    private void poiKategoriyeGoreListele(
        double lat,
        double lon,
        String kategori,
        int mesafeMetre,
        boolean kapaliAlan
    ) {
        String keyword = kategori.equals("yemek")
            ? getString(R.string.restaurant_keyword)
            : kategori.equals("calisma")
                ? getString(R.string.library_keyword)
                : kategori.equals("alisveris")
                    ? getString(R.string.shopping_keyword)
                    : getString(R.string.place_keyword);

        String uid = Objects.requireNonNull(firebaseUser).getUid();
        db
            .collection("kullanicilar")
            .document(uid)
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    String yas = String.valueOf(documentSnapshot.get("yas"));
                    String sorumluluk = documentSnapshot.getString(
                        "sorumluluk"
                    );
                    String mod = getSharedPreferences(
                        "mod_pref",
                        MODE_PRIVATE
                    ).getString("mod_son", "neutral");

                    googlePlacesPOIAl(
                        this,
                        lat,
                        lon,
                        keyword,
                        kullaniciKonumu,
                        mod,
                        yas,
                        sorumluluk,
                        guncelHavaDurumu,
                        mevcutIlce,
                        ""
                    );
                }
            })
            .addOnFailureListener(e ->
                Toast.makeText(
                    this,
                    getString(R.string.user_info_fetch_failed),
                    Toast.LENGTH_SHORT
                ).show()
            );
    }

    public void googlePlacesPOIAl(
            Context context,
            double lat,
            double lon,
            String keyword,
            Location kullaniciKonumu,
            String mod,
            String yas,
            String sorumluluk,
            String hava,
            String il,
            String ilce
    ) {
        String uid = Objects.requireNonNull(firebaseUser).getUid();
        zarAiManager.checkAILimit((canUse, remaining) -> {
            if (
                    isFinishing() ||
                    (android.os.Build.VERSION.SDK_INT >= 17 && isDestroyed())
            ) return;
            runOnUiThread(() -> {
                if (!canUse) {
                    Toast.makeText(
                            context,
                            getString(R.string.gunluk_groq_limit_doldu),
                            Toast.LENGTH_LONG
                    ).show();
                    hakGoster();
                    return;
                }
                String tarih = new SimpleDateFormat(
                        "yyyyMMdd",
                        Locale.getDefault()
                ).format(new Date());
                DocumentReference limitRef = db
                        .collection("kullanicilar")
                        .document(uid)
                        .collection("groq_limit")
                        .document(tarih);

                String firebaseKey = KeyDecryptor.getFirebaseKey();
                String url =
                        "https://maps.googleapis.com/maps/api/place/nearbysearch/json?" +
                                "location=" +
                                lat +
                                "," +
                                lon +
                                "&radius=3000" +
                                "&keyword=" +
                                Uri.encode(keyword) +
                                "&key=" +
                                firebaseKey;

                RequestQueue queue = Volley.newRequestQueue(context);
                JsonObjectRequest request = new JsonObjectRequest(
                        Request.Method.GET,
                        url,
                        null,
                        response -> {
                            try {
                                JSONArray results = response.getJSONArray(
                                        "results"
                                );
                                List<PoiModel> list = new ArrayList<>();
                                for (int i = 0; i < results.length(); i++) {
                                    JSONObject obj = results.getJSONObject(i);
                                    String ad = obj.optString("name", "Bilinmiyor");
                                    JSONObject location = obj
                                            .getJSONObject("geometry")
                                            .getJSONObject("location");
                                    double latPoi = location.getDouble("lat");
                                    double lonPoi = location.getDouble("lng");
                                    Location poiKonum = new Location("");
                                    poiKonum.setLatitude(latPoi);
                                    poiKonum.setLongitude(lonPoi);
                                    double mesafe = kullaniciKonumu != null
                                            ? kullaniciKonumu.distanceTo(poiKonum)
                                            : -1;
                                    list.add(
                                            new PoiModel(
                                                    ad,
                                                    "bilinmiyor",
                                                    mesafe,
                                                    latPoi,
                                                    lonPoi
                                            )
                                    );
                                }
                                List<PoiModel> gonderilecek = list.size() > 20
                                        ? list.subList(0, 20)
                                        : list;
                                Gemini.getLogicalPOIs(
                                        context,
                                        gonderilecek,
                                        mod,
                                        yas,
                                        sorumluluk,
                                        hava,
                                        il,
                                        ilce,
                                        new Gemini.GroqCallback() {
                                            @Override
                                            public void onResponse(
                                                    List<PoiModel> finalList
                                            ) {
                                                poiList.clear();
                                                poiList.addAll(finalList);
                                                poiAdapter.notifyDataSetChanged();
                                                lottieLoading.setVisibility(View.GONE);
                                                recyclerViewPoi.setVisibility(
                                                        View.VISIBLE
                                                );
                                                // Günlük limit azaltımı artık kategoriSec'te yapılıyor, burada tekrar yapılmamalı.
                                                // limitRef.get().addOnSuccessListener(doc -> {
                                                //     long kullanilan = doc.exists()
                                                //         ? doc.getLong("count")
                                                //         : 0;
                                                //     limitRef
                                                //         .set(
                                                //             Collections.singletonMap(
                                                //                 "count",
                                                //                 kullanilan + 1
                                                //             ),
                                                //             SetOptions.merge()
                                                //         )
                                                //         .addOnSuccessListener(
                                                //             aVoid -> hakGoster()
                                                //         );
                                                // });
                                                hakGoster();
                                            }

                                            @Override
                                            public void onFailure(String error) {
                                                lottieLoading.setVisibility(View.GONE);
                                                recyclerViewPoi.setVisibility(
                                                        View.VISIBLE
                                                );
                                                Toast.makeText(
                                                        context,
                                                        getString(
                                                                R.string.groq_hatasi,
                                                                error
                                                        ),
                                                        Toast.LENGTH_SHORT
                                                ).show();
                                            }
                                        }
                                );
                            } catch (Exception e) {
                                lottieLoading.setVisibility(View.GONE);
                                recyclerViewPoi.setVisibility(View.VISIBLE);
                                Toast.makeText(
                                        context,
                                        getString(R.string.veri_ayristairilamadi),
                                        Toast.LENGTH_SHORT
                                ).show();
                            }
                        },
                        error -> {
                            lottieLoading.setVisibility(View.GONE);
                            recyclerViewPoi.setVisibility(View.VISIBLE);
                            Toast.makeText(
                                    context,
                                    getString(
                                            R.string.google_places_hatasi_toast,
                                            error.toString()
                                    ),
                                    Toast.LENGTH_SHORT
                            ).show();
                        }
                );
                queue.add(request);
            });
        });
    }

    private void konumuAl() {
        if (
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                        ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
        ) {
            return;
        }

        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(5000);
        locationRequest.setFastestInterval(2000);

        fusedLocationClient.requestLocationUpdates(locationRequest, new com.google.android.gms.location.LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);
                fusedLocationClient.removeLocationUpdates(this); // Konum alındıktan sonra durdur

                if (locationResult == null || locationResult.getLastLocation() == null) {
                    Log.w("Konum", "Canlı konum alınamadı");
                    Toast.makeText(AnaEkran.this, getString(R.string.location_not_available), Toast.LENGTH_SHORT).show();
                    return;
                }

                Location location = locationResult.getLastLocation();
                kullaniciKonumu = location;
                Log.d("Konum", "Lat: " + location.getLatitude() + ", Lon: " + location.getLongitude());
                updateLocationUI(location);
            }
        }, Looper.getMainLooper());
    }

    private void updateLocationUI(Location location) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> adresler = geocoder.getFromLocation(
                location.getLatitude(),
                location.getLongitude(),
                1
            );
            if (adresler != null && !adresler.isEmpty()) {
                Address adres = adresler.get(0);
                mevcutIlce = adres.getSubAdminArea();
                if (mevcutIlce == null) mevcutIlce = "N/A";
                ((TextView) findViewById(R.id.textView16)).setText(
                    getString(
                        R.string.konum_ornek,
                        adres.getAdminArea(),
                        mevcutIlce
                    )
                );
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        havaDurumunuGetir(location.getLatitude(), location.getLongitude());
    }

    private void havaDurumunuGetir(double lat, double lon) {
        String apiKey = KeyDecryptor.getWeatherKey();
        String dilKodu = Locale.getDefault().getLanguage();
        String url =
            "https://api.openweathermap.org/data/2.5/weather?lat=" +
            lat +
            "&lon=" +
            lon +
            "&appid=" +
            apiKey +
            "&units=metric&lang=" +
            dilKodu;
        RequestQueue queue = Volley.newRequestQueue(this);
        JsonObjectRequest request = new JsonObjectRequest(
            Request.Method.GET,
            url,
            null,
            response -> {
                try {
                    String sehir = response.getString("name");
                    JSONObject main = response.getJSONObject("main");
                    guncelSicaklik = main.getDouble("temp");
                    JSONObject weather = response
                        .getJSONArray("weather")
                        .getJSONObject(0);
                    guncelHavaDurumu = weather
                        .getString("description")
                        .toLowerCase(Locale.ROOT);
                    ((TextView) findViewById(R.id.textViewHava)).setText(
                        getString(
                            R.string.hava_durumu_ornek,
                            sehir,
                            guncelHavaDurumu,
                            guncelSicaklik
                        )
                    );
                    String icon = weather.getString("icon");
                    int resId = getResources().getIdentifier(
                        "icon_" + icon,
                        "drawable",
                        getPackageName()
                    );
                    ((ImageView) findViewById(
                            R.id.imageViewHava
                        )).setImageResource(
                        resId != 0 ? resId : R.drawable.icon_03d
                    );
                } catch (Exception e) {
                    e.printStackTrace();
                }
            },
            error ->
                Toast.makeText(
                    this,
                    getString(R.string.hava_durumu_alinamadi),
                    Toast.LENGTH_SHORT
                ).show()
        );
        queue.add(request);
    }

    private void kullaniciBilgileriniGuncelle() {
        if (firebaseUser == null) return;
        String uid = firebaseUser.getUid();
        View headerView = navigationView.getHeaderView(0);
        TextView textViewUsername = headerView.findViewById(
            R.id.textViewUsername
        );
        ImageView imageViewProfile = headerView.findViewById(
            R.id.imageViewProfile
        );
        TextView t = findViewById(R.id.textView15);

        db
            .collection("kullanicilar")
            .document(uid)
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    String kullaniciAdi = documentSnapshot.getString(
                        "kullaniciAdi"
                    );
                    String profilFotoUrl = documentSnapshot.getString(
                        "profilFotoUrl"
                    );
                    textViewUsername.setText(kullaniciAdi);
                    t.setText(
                        getString(R.string.hosgeldin_kullanici, kullaniciAdi)
                    );
                    if (profilFotoUrl != null && !profilFotoUrl.isEmpty()) {
                        Glide.with(this)
                            .load(profilFotoUrl)
                            .placeholder(R.drawable.outline_account_circle_24)
                            .error(R.drawable.ic_launcher_background)
                            .circleCrop()
                            .into(imageViewProfile);
                    }
                }
            })
            .addOnFailureListener(e ->
                Toast.makeText(
                    this,
                    getString(R.string.kullanici_verileri_alinamadi),
                    Toast.LENGTH_SHORT
                ).show()
            );
    }

    private void modEmojiSecimiGoster() {
        SharedPreferences prefs = getSharedPreferences(
            "mod_pref",
            MODE_PRIVATE
        );
        String bugun = new SimpleDateFormat(
            "yyyyMMdd",
            Locale.getDefault()
        ).format(new Date());
        String kayitliTarih = prefs.getString("mod_alindi_tarihi", "");
        LinearLayout modEmojiLayout = findViewById(R.id.modEmojiLayout);

        if (!bugun.equals(kayitliTarih)) {
            modEmojiLayout.setVisibility(View.VISIBLE);
            View.OnClickListener modClickListener = v -> {
                String emoji = ((TextView) v).getText().toString();
                String uid = Objects.requireNonNull(firebaseUser).getUid();
                String tarih = new SimpleDateFormat(
                    "yyyy-MM-dd",
                    Locale.getDefault()
                ).format(new Date());
                Map<String, Object> modData = new HashMap<>();
                modData.put("emoji", emoji);
                modData.put("timestamp", FieldValue.serverTimestamp());
                db
                    .collection("modlar")
                    .document(uid)
                    .collection("gunluk")
                    .document(tarih)
                    .set(modData)
                    .addOnSuccessListener(unused -> {
                        Toast.makeText(
                            this,
                            getString(R.string.mod_kaydedildi, emoji),
                            Toast.LENGTH_SHORT
                        ).show();
                        prefs
                            .edit()
                            .putString("mod_alindi_tarihi", bugun)
                            .apply();
                        modEmojiLayout.setVisibility(View.GONE);
                        if (textView19 != null) textView19.setVisibility(
                            View.GONE
                        ); // <-- BURAYA EKLE
                    })
                    .addOnFailureListener(e ->
                        Toast.makeText(
                            this,
                            getString(R.string.kayit_basarisiz),
                            Toast.LENGTH_SHORT
                        ).show()
                    );
            };
            findViewById(R.id.emoji_happy).setOnClickListener(modClickListener);
            findViewById(R.id.emoji_sad).setOnClickListener(modClickListener);
            findViewById(R.id.emoji_angry).setOnClickListener(modClickListener);
            findViewById(R.id.emoji_calm).setOnClickListener(modClickListener);
        } else {
            modEmojiLayout.setVisibility(View.GONE);
            findViewById(R.id.textView19).setVisibility(View.GONE);
        }
    }

    private void modBildirimAlarmiKur() {
        Intent intent = new Intent(this, ModNotificationReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
            this,
            101,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        );
        AlarmManager alarmManager = (AlarmManager) getSystemService(
            ALARM_SERVICE
        );
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 12); // Bildirim saatini 12:00 olarak ayarlayalım
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        if (Calendar.getInstance().after(cal)) {
            cal.add(Calendar.DATE, 1);
        }
        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            cal.getTimeInMillis(),
            AlarmManager.INTERVAL_DAY,
            pendingIntent
        );
    }

    private void hideSystemUI() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            getWindow().setDecorFitsSystemWindows(false);
            WindowInsetsController insetsController =
                getWindow().getInsetsController();
            if (insetsController != null) {
                insetsController.hide(
                    WindowInsets.Type.statusBars() |
                    WindowInsets.Type.navigationBars()
                );
                insetsController.setSystemBarsBehavior(
                    WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                );
            }
        } else {
            getWindow()
                .getDecorView()
                .setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY |
                    View.SYSTEM_UI_FLAG_FULLSCREEN |
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                );
        }
    }

    @Override
    public void onRequestPermissionsResult(
        int requestCode,
        @NonNull String[] permissions,
        @NonNull int[] grantResults
    ) {
        super.onRequestPermissionsResult(
            requestCode,
            permissions,
            grantResults
        );
        if (
            requestCode == KONUM_IZNI_KODU &&
            grantResults.length > 0 &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            konumuAl();
        } else {
            Toast.makeText(
                this,
                getString(R.string.konum_izni_gerekli),
                Toast.LENGTH_SHORT
            ).show();
        }
    }
    
    /**
     * ZarAiActivity mantığı ile maksimum günlük hakkı artırır
     * Bu sayede davet sistemi doğru çalışır
     */
    private void artirMaxGunlukHak(String uid, int miktar, String mesaj) {
        String tarih = new SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(new Date());
        DocumentReference hakRef = db
            .collection("kullanicilar")
            .document(uid)
            .collection("groq_limit")
            .document(tarih);
            
        hakRef.get().addOnSuccessListener(doc -> {
            // Mevcut günlük maksimum hakkı al (varsayılan 50)
            Long maxDaily = doc.getLong("gunluk_max_hak");
            long gunlukMax = maxDaily != null ? maxDaily : GUNLUK_AI_ONERI_HAKKI;
            
            // Mevcut kullanım sayısını al
            long mevcutKullanim = doc.exists() && doc.contains("count") ? doc.getLong("count") : 0;
            
            // Yeni maksimum hakkı belirle
            long yeniMaxHak = gunlukMax + miktar;
            
            // Kalan hakkı hesapla (max - kullanım)
            long kalanHak = Math.max(0, yeniMaxHak - mevcutKullanim);
            
            // Firestore'a kaydet
            Map<String, Object> data = new HashMap<>();
            data.put("gunluk_max_hak", yeniMaxHak);
            data.put("count", mevcutKullanim); // Mevcut kullanımı koru
            
            hakRef.set(data, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, mesaj, Toast.LENGTH_LONG).show();
                    hakGoster(); // Hak sayısını güncelle
                    Log.d(TAG, "Hak artırıldı: " + uid + " -> +" + miktar + " (Yeni max: " + yeniMaxHak + ")");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Hak artırma hatası: " + e.getMessage());
                    Toast.makeText(this, getString(R.string.bonus_added_error), Toast.LENGTH_SHORT).show();
                });
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Hak bilgisi alma hatası: " + e.getMessage());
        });
    }
    
    /**
     * Davet linklerini kontrol eder (Deep link, referrer, vs.)
     */
    private void checkInviteLink() {
        String inviterUid = null;
        
        // 1. Deep link kontrolü (URL'den ref parametresi)
        Intent intent = getIntent();
        Uri data = intent.getData();
        if (data != null && data.getQueryParameter("ref") != null) {
            inviterUid = data.getQueryParameter("ref");
            Log.d(TAG, "Deep link referrer bulundu: " + inviterUid);
        }
        
        // 2. Play Store referrer kontrolü (Google Play Install Referrer API)
        if (inviterUid == null) {
            // Google Play referrer için ek kod eklenebilir
            // Şimdilik basit string kontrolü yapalım
            String referrer = intent.getStringExtra("referrer");
            if (referrer != null && !referrer.isEmpty()) {
                inviterUid = referrer;
                Log.d(TAG, "Play Store referrer bulundu: " + inviterUid);
            }
        }
        
        // 3. Saved State kontrolü (önceki session'dan kalmış olabilir)
        if (inviterUid == null) {
            SharedPreferences prefs = getSharedPreferences("invite_prefs", MODE_PRIVATE);
            String savedReferrer = prefs.getString("pending_referrer", null);
            if (savedReferrer != null && !savedReferrer.isEmpty()) {
                inviterUid = savedReferrer;
                // Kullanıldıktan sonra sil
                prefs.edit().remove("pending_referrer").apply();
                Log.d(TAG, "Saved referrer bulundu: " + inviterUid);
            }
        }
        
        // Davet işlemini gerçekleştir
        if (inviterUid != null && firebaseUser != null) {
            processInvite(inviterUid);
        }
    }
    
    /**
     * Davet işlemini gerçekleştirir
     */
    private void processInvite(String inviterUid) {
        String currentUid = Objects.requireNonNull(firebaseUser).getUid();
        
        // Kendi kendini davet etmesini engelle
        if (inviterUid.equals(currentUid)) {
            Toast.makeText(this, getString(R.string.cannot_use_own_invite), Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Daha önce bu davet kullanılmış mı kontrol et
        SharedPreferences prefs = getSharedPreferences("invite_prefs", MODE_PRIVATE);
        boolean alreadyUsed = prefs.getBoolean("invite_used_" + inviterUid, false);
        
        if (alreadyUsed) {
            Log.d(TAG, "Bu davet daha önce kullanılmış: " + inviterUid);
            return;
        }
        
        // Davet işlemini kaydet
        prefs.edit().putBoolean("invite_used_" + inviterUid, true).apply();
        
        // Davet eden kişiye maksimum günlük hakkı artır (ZarAi mantığı)
        artirMaxGunlukHak(inviterUid, 20, getString(R.string.invite_bonus_inviter));
        
        // Davet edilen kişiye de hak ekle
        artirMaxGunlukHak(currentUid, 10, getString(R.string.invite_bonus_invited));
        
        // Firestore'a da kaydet
        FirebaseFirestore.getInstance()
            .collection("kullanicilar")
            .document(inviterUid)
            .update("invitedSomeone", true)
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "Davet işlemi başarılı: " + inviterUid + " -> " + currentUid);
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Davet kaydetme hatası: " + e.getMessage());
            });
    }
    
    /**
     * Referral kod girme dialog'unu gösterir - Uygulama stil çizgisi
     */
    private void showReferralCodeDialog() {
        // Ana container - Uygulama teması
        LinearLayout mainContainer = new LinearLayout(this);
        mainContainer.setOrientation(LinearLayout.VERTICAL);
        mainContainer.setPadding(40, 30, 40, 30);
        mainContainer.setBackgroundColor(getResources().getColor(R.color.x)); // Beyaz arkaplan
        
        // Başlık - ZarAi font ve koyu mavi renk teması
        TextView titleText = new TextView(this);
        titleText.setText(getString(R.string.referral_dialog_title));
        titleText.setTextSize(22);
        titleText.setTextColor(getResources().getColor(R.color.white)); // Koyu mavi metin
        titleText.setTypeface(ResourcesCompat.getFont(this, R.font.bf10));
        titleText.setPadding(0, 0, 0, 15);
        titleText.setGravity(android.view.Gravity.CENTER);
        
        // Açıklama metni - ZarAi font ve koyu mavi teması
        TextView descText = new TextView(this);
        descText.setText(getString(R.string.referral_dialog_message));
        descText.setTextSize(14);
        descText.setTypeface(ResourcesCompat.getFont(this, R.font.bf3));
        descText.setTextColor(getResources().getColor(R.color.white)); // Koyu mavi metin
        descText.setPadding(0, 0, 0, 25);
        descText.setGravity(android.view.Gravity.CENTER);
        descText.setLineSpacing(4f, 1.0f);
        
        // Mevcut kod kartı - Uygulama buton stili
        String currentUid = Objects.requireNonNull(firebaseUser).getUid();
        String myCode = currentUid.substring(0, 8).toUpperCase();
        
        LinearLayout codeCard = new LinearLayout(this);
        codeCard.setOrientation(LinearLayout.VERTICAL);
        codeCard.setBackgroundResource(R.drawable.button1); // Uygulama buton background
        codeCard.getBackground().setTint(getResources().getColor(R.color.butonmor));
        codeCard.setPadding(30, 25, 30, 25);
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 
            ViewGroup.LayoutParams.WRAP_CONTENT
        );
        cardParams.setMargins(0, 0, 0, 25);
        codeCard.setLayoutParams(cardParams);
        
        TextView myCodeText = new TextView(this);
        myCodeText.setText(getString(R.string.my_referral_code, myCode));
        myCodeText.setTextSize(16);
        myCodeText.setTextColor(getResources().getColor(R.color.white));
        myCodeText.setGravity(android.view.Gravity.CENTER);
        myCodeText.setTypeface(ResourcesCompat.getFont(this, R.font.bf10));
        
        // Kopyala butonu - ZarAi buton stili
        Button copyButton = new Button(this);
        copyButton.setText(getString(R.string.btn_copy_code));
        copyButton.setTextSize(12);
        copyButton.setBackgroundResource(R.drawable.button1);
        copyButton.getBackground().setTint(getResources().getColor(R.color.x)); // ZarAi mavi rengi
        copyButton.setTextColor(getResources().getColor(R.color.white));
        copyButton.setTypeface(ResourcesCompat.getFont(this, R.font.bf11));
        copyButton.setPadding(25, 12, 25, 12);
        copyButton.setAllCaps(false);
        LinearLayout.LayoutParams copyParams = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, 
            ViewGroup.LayoutParams.WRAP_CONTENT
        );
        copyParams.gravity = android.view.Gravity.CENTER;
        copyParams.setMargins(0, 15, 0, 0);
        copyButton.setLayoutParams(copyParams);
        
        copyButton.setOnClickListener(v -> {
            android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            android.content.ClipData clip = android.content.ClipData.newPlainText("Referral Code", myCode);
            clipboard.setPrimaryClip(clip);
            Toast.makeText(this, getString(R.string.code_copied, myCode), Toast.LENGTH_SHORT).show();
        });
        
        codeCard.addView(myCodeText);
        codeCard.addView(copyButton);
        
        // Ayırıcı görünüm
        View spacer = new View(this);
        LinearLayout.LayoutParams spacerParams = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 20
        );
        spacer.setLayoutParams(spacerParams);
        
        // EditText - ZarAi beyaz input stili 
        final android.widget.EditText input = new android.widget.EditText(this);
        input.setHint(getString(R.string.referral_code_hint));
        input.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);
        input.setTextSize(16);
        input.setPadding(25, 20, 25, 20);
        input.setBackgroundResource(R.drawable.button1); // ZarAi stili
        input.getBackground().setTint(getResources().getColor(R.color.x)); // ZarAi mavi arkaplan
        input.setTextColor(getResources().getColor(R.color.white)); // Beyaz metin
        input.setHintTextColor(getResources().getColor(R.color.white)); // Beyaz hint
        input.setTypeface(ResourcesCompat.getFont(this, R.font.bf5));
        LinearLayout.LayoutParams inputParams = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 
            ViewGroup.LayoutParams.WRAP_CONTENT
        );
        inputParams.setMargins(0, 0, 0, 25);
        input.setLayoutParams(inputParams);
        
        // Layout'a ekle
        mainContainer.addView(titleText);
        mainContainer.addView(descText);
        mainContainer.addView(codeCard);
        mainContainer.addView(spacer);
        mainContainer.addView(input);
        
        // Buton container
        LinearLayout buttonContainer = new LinearLayout(this);
        buttonContainer.setOrientation(LinearLayout.HORIZONTAL);
        buttonContainer.setGravity(android.view.Gravity.CENTER);
        buttonContainer.setPadding(0, 15, 0, 0);
        
        // İptal butonu - ZarAi buton stili
        Button cancelButton = new Button(this);
        cancelButton.setText(getString(R.string.btn_cancel));
        cancelButton.setBackgroundResource(R.drawable.button1);
        cancelButton.getBackground().setTint(android.graphics.Color.parseColor("#4068A0")); // ZarAi mavi tonu
        cancelButton.setTextColor(getResources().getColor(R.color.white));
        cancelButton.setTypeface(ResourcesCompat.getFont(this, R.font.bf11));
        cancelButton.setAllCaps(false);
        LinearLayout.LayoutParams cancelParams = new LinearLayout.LayoutParams(
            0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f
        );
        cancelParams.setMargins(0, 0, 10, 0);
        cancelButton.setLayoutParams(cancelParams);
        
        // Kullan butonu - ZarAi ana renk (#4068A0)
        Button useButton = new Button(this);
        useButton.setText(getString(R.string.btn_use_code));
        useButton.setBackgroundResource(R.drawable.button1);
        useButton.getBackground().setTint(android.graphics.Color.parseColor("#4068A0")); // ZarAi mavi tonu
        useButton.setTextColor(getResources().getColor(R.color.white));
        useButton.setTypeface(ResourcesCompat.getFont(this, R.font.bf11));
        useButton.setAllCaps(false);
        LinearLayout.LayoutParams useParams = new LinearLayout.LayoutParams(
            0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f
        );
        useParams.setMargins(10, 0, 0, 0);
        useButton.setLayoutParams(useParams);
        
        buttonContainer.addView(cancelButton);
        buttonContainer.addView(useButton);
        mainContainer.addView(buttonContainer);
        
        // Dialog oluştur - Minimal çerçeve
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setView(mainContainer);
        builder.setCancelable(true);
        
        android.app.AlertDialog dialog = builder.create();
        
        // Buton tıklama olayları
        useButton.setOnClickListener(v -> {
            String referralCode = input.getText().toString().trim().toUpperCase();
            if (!referralCode.isEmpty()) {
                dialog.dismiss();
                processReferralCode(referralCode);
            } else {
                input.setError(getString(R.string.enter_valid_code));
                input.requestFocus();
            }
        });
        
        cancelButton.setOnClickListener(v -> dialog.dismiss());
        
        // Dialog'u göster
        dialog.show();
        
        // Dialog penceresini optimize et - Yuvarlak köşeler
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(R.drawable.button1);
            dialog.getWindow().getAttributes().windowAnimations = android.R.style.Animation_Dialog;
            dialog.getWindow().setLayout(
                (int) (getResources().getDisplayMetrics().widthPixels * 0.9),
                ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }
    }
    
    /**
     * Referral kodunu işler
     */
    private void processReferralCode(String referralCode) {
        // Kod formatını kontrol et
        if (referralCode.length() < 6) {
            Toast.makeText(this, getString(R.string.code_too_short), Toast.LENGTH_SHORT).show();
            return;
        }
        
        String currentUid = Objects.requireNonNull(firebaseUser).getUid();
        String myCode = currentUid.substring(0, 8).toUpperCase();
        
        // Kendi kodunu kullanmasını engelle
        if (referralCode.equals(myCode)) {
            Toast.makeText(this, getString(R.string.cannot_use_own_code), Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Daha önce bu kod kullanılmış mı?
        SharedPreferences prefs = getSharedPreferences("invite_prefs", MODE_PRIVATE);
        boolean alreadyUsed = prefs.getBoolean("referral_used_" + referralCode, false);
        
        if (alreadyUsed) {
            Toast.makeText(this, getString(R.string.code_already_used), Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Kodu kullanıldı olarak işaretle
        prefs.edit().putBoolean("referral_used_" + referralCode, true).apply();
        
        // Firestore'da bu koda sahip kullanıcıyı bul
        findUserByReferralCode(referralCode);
    }
    
    /**
     * Referral koduna göre kullanıcı bulur
     */
    private void findUserByReferralCode(String referralCode) {
        // Referral kodu UID'nin ilk 8 karakteri olduğu için
        // Tüm kullanıcıları tarayıp eşleşen UID bulmamız gerekiyor
        
        db.collection("kullanicilar")
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                String foundUid = null;
                
                for (com.google.firebase.firestore.DocumentSnapshot doc : queryDocumentSnapshots) {
                    String uid = doc.getId();
                    String userCode = uid.substring(0, Math.min(8, uid.length())).toUpperCase();
                    
                    if (userCode.equals(referralCode)) {
                        foundUid = uid;
                        break;
                    }
                }
                
                if (foundUid != null) {
                    // Referral kodunu kullan
                    processInvite(foundUid);
                } else {
                    Toast.makeText(this, getString(R.string.invalid_referral_code), Toast.LENGTH_SHORT).show();
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Referral kod arama hatası: " + e.getMessage());
                Toast.makeText(this, getString(R.string.code_check_error), Toast.LENGTH_SHORT).show();
            });
    }
}
