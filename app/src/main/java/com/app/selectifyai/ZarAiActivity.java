// ZarAiActivity.java - Düzeltilmiş Reklam Kodu

package com.app.selectifyai;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import com.airbnb.lottie.LottieAnimationView;
// (AdMob importları kaldırıldı)
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ZarAiActivity extends AnaAktivite {

    // Yeni Davet Hakkı ve Max Günlük Hak alanı
    private static final int EXTRA_DAVET_HAKKI = 10;
    private static final String FIELD_MAX_DAILY = "gunluk_max_hak";

    private TextView aiYorumu;
    private static final String TAG = "ZarAiActivity";
    private static final int MAX_SECENEK = 6;
    private static final int KONUM_IZIN_KODU = 1001;
    private static final int GUNLUK_HAK = 10;

    // Test reklam ID'si - Gerçek yayında değiştirin!
    private static final String AD_UNIT_ID =
        "ca-app-pub-3940256099942544/5224354917"; // TEST ID
    // Gerçek ID: "ca-app-pub-4497036434463360/9909505336";

    // UI
    private LottieAnimationView zarAnim;
    private View dimView;
    private LinearLayout sonucKart;
    private TextView sonucMetni, tvHakSayisi;
    private Button btnTamam, secenekEkleBtn, zarAtButton, btnPaylas;
    private ImageView sonucZarImage;
    private LottieAnimationView konfetiAnim;
    private TextView emojiView;
    private TextView cevapText;
    private TextView gerekceText;
    private EditText inputSoru;
    private RadioGroup modeRadioGroup;
    private LinearLayout secenekPanel, evetHayirPanel;
    private final List<EditText> secenekEditTextList = new ArrayList<>();
    private LinearLayout reklamPaneli;
    private Button btnReklamIzle;
    private Button btnPremium;

    // (AdMob alanları kaldırıldı)

    // Firebase
    private final FirebaseFirestore firestore = FirebaseFirestore.getInstance();
    private final FirebaseAuth auth = FirebaseAuth.getInstance();

    private static final String ZAR_HAKKI_COLLECTION = "zar_hakki";
    private static final String FIELD_REMAINING = "kalan";

    private AIResponseHelper aiHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.zarai);
        hideSystemUI();
        aiYorumu = findViewById(R.id.aiYorumu);
        aiHelper = new AIResponseHelper(this);
        zarAnim = findViewById(R.id.zarAnim);

        // 1. JSON içeriğini yüklemiş olmalı (zaten XML’de tanımladık)
        zarAnim.setProgress(1f); // 1.0 = son kare
        // UI bağlantılarını yap
        initializeViews();

        // Gerekçe Göster butonu ve gerekçeText bağlantısı
        Button btnGerekceGoster = findViewById(R.id.btnGerekceGoster);
        gerekceText = findViewById(R.id.gerekceText);
        if (btnGerekceGoster != null && gerekceText != null) {
            btnGerekceGoster.setOnClickListener(v -> {
                if (gerekceText.getVisibility() == View.GONE) {
                    gerekceText.setVisibility(View.VISIBLE);

                } else {
                    gerekceText.setVisibility(View.GONE);

                }
            });
        }

        setupModeSelection();
        setupButtons();

        // İlk seçenekleri ekle
        addNewOption(getString(R.string.birinci_secenek));
        addNewOption(getString(R.string.ikinci_secenek));

        hakGoster();
    }


    private void initializeViews() {
        zarAnim = findViewById(R.id.zarAnim);
        dimView = findViewById(R.id.dimView);
        sonucKart = findViewById(R.id.sonucKart);
        sonucMetni = findViewById(R.id.sonucMetni);
        btnTamam = findViewById(R.id.btnTamam);
        zarAtButton = findViewById(R.id.zarAtButton);
        sonucZarImage = findViewById(R.id.sonucZarImage);
        inputSoru = findViewById(R.id.inputSoru);
        modeRadioGroup = findViewById(R.id.modeRadioGroup);
        secenekPanel = findViewById(R.id.secenekPanel);
        btnPaylas = findViewById(R.id.btnPaylas);
        Button btnLinklePaylas = findViewById(R.id.btnLinklePaylas);
        evetHayirPanel = findViewById(R.id.evetHayirPanel);
        tvHakSayisi = findViewById(R.id.tvHakSayisi);
        konfetiAnim = findViewById(R.id.konfetiAnim);
        emojiView = findViewById(R.id.emojiView);
        cevapText = findViewById(R.id.cevapText);
        gerekceText = findViewById(R.id.gerekceText);

        if (gerekceText != null) {
            gerekceText.setVisibility(View.GONE);
        }

        // Yeni öğeler
        reklamPaneli = findViewById(R.id.reklamPaneli);
        reklamPaneli.setVisibility(View.GONE);
        btnReklamIzle = findViewById(R.id.btnReklamIzle);
        btnPremium = findViewById(R.id.btnPremium);

        // Gerekçe Göster butonu
        Button btnGerekceGoster = findViewById(R.id.btnGerekceGoster);
        if (btnGerekceGoster != null && gerekceText != null) {
            btnGerekceGoster.setOnClickListener(v -> {
                if (gerekceText.getVisibility() == View.GONE) {
                    gerekceText.setVisibility(View.VISIBLE);

                } else {
                    gerekceText.setVisibility(View.GONE);

                }
            });
        }
    }

    private void setupModeSelection() {
        modeRadioGroup.check(R.id.secenekMode);
        secenekPanel.setVisibility(View.VISIBLE);
        evetHayirPanel.setVisibility(View.GONE);

        modeRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.secenekMode) {
                secenekPanel.setVisibility(View.VISIBLE);
                evetHayirPanel.setVisibility(View.GONE);
            } else {
                secenekPanel.setVisibility(View.GONE);
                evetHayirPanel.setVisibility(View.VISIBLE);
            }
        });
    }

    private void setupButtons() {
        // Seçenek ekle butonu
        secenekEkleBtn = new Button(this);
        secenekEkleBtn.setText(getString(R.string.secenek_ekle));
        secenekEkleBtn.setBackgroundResource(R.drawable.button1);
        secenekEkleBtn.setBackgroundTintList(
                getResources().getColorStateList(R.color.xx)
        );
        secenekEkleBtn.setTextColor(getResources().getColor(R.color.white));
        secenekEkleBtn.setTypeface(ResourcesCompat.getFont(this, R.font.bf1));
        secenekEkleBtn.setAllCaps(false);
        secenekEkleBtn.setPadding(24, 12, 24, 12);
        secenekEkleBtn.setOnClickListener(v -> {
            if (secenekEditTextList.size() < MAX_SECENEK) {
                addNewOption(
                        String.format(
                                getString(R.string.secenek_n_hint),
                                secenekEditTextList.size() + 1
                        )
                );
            } else {
                Toast.makeText(
                        this,
                        getString(R.string.en_fazla_6_secenek),
                        Toast.LENGTH_SHORT
                ).show();
            }
        });
        secenekPanel.addView(secenekEkleBtn);

        // Zar atma
        zarAtButton.setOnClickListener(v -> hakKontrolEtVeZarAt());

        // Tamam butonu
        btnTamam.setOnClickListener(v -> {
            dimView.setVisibility(View.GONE);
            sonucKart.setVisibility(View.GONE);
            if (konfetiAnim != null) {
                konfetiAnim.setVisibility(View.GONE);
                konfetiAnim.cancelAnimation();
            }
        });

        btnPaylas.setOnClickListener(v -> {
            StringBuilder shareContent = new StringBuilder();
            String soru = inputSoru.getText().toString().trim();
            if (!soru.isEmpty()) {
                shareContent.append("Soru: ").append(soru).append("\n");
            }
            String cevap = cevapText != null ? cevapText.getText().toString() : "";
            if (!cevap.isEmpty()) {
                shareContent.append("Cevap: ").append(cevap).append("\n");
            }
            String gerekce = gerekceText != null ? gerekceText.getText().toString() : "";
            if (!gerekce.isEmpty()) {
                shareContent.append("Gerekçe: ").append(gerekce).append("\n");
            }

            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_TEXT, shareContent.toString());
            startActivity(Intent.createChooser(shareIntent, "Paylaş"));
        });

        // Link ile Paylaş butonu
        Button btnLinklePaylas = findViewById(R.id.btnLinklePaylas);
        if (btnLinklePaylas != null) {
            btnLinklePaylas.setOnClickListener(v -> {
                StringBuilder sb = new StringBuilder();
                if (!inputSoru.getText().toString().isEmpty()) {
                    sb.append("Soru: ").append(inputSoru.getText().toString()).append("\n");
                }
                if (!cevapText.getText().toString().isEmpty()) {
                    sb.append("Cevap: ").append(cevapText.getText().toString()).append("\n");
                }
                if (!gerekceText.getText().toString().isEmpty()) {
                    sb.append("Gerekçe: ").append(gerekceText.getText().toString()).append("\n");
                }

                String shareText = sb.toString();
                String link = "https://selectifyai.com/share?data=" +
                    android.util.Base64.encodeToString(shareText.getBytes(), android.util.Base64.URL_SAFE | android.util.Base64.NO_WRAP);

                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("text/plain");
                shareIntent.putExtra(Intent.EXTRA_TEXT, "Kararım:\n" + link);

                Intent chooser = Intent.createChooser(shareIntent, "Arkadaşını siteye davet et");
                startActivityForResult(chooser, 333); // Paylaşım kodu
            });
        }

        // Reklam izleme (AdMob kodu kaldırıldı)

        // Premium’a geç

        // Arkadaşını Davet Et: Günlük hak artırımı
    }

    private void hakKontrolEtVeZarAt() {
        String uid = auth.getUid();
        if (uid == null) {
            Toast.makeText(
                this,
                "Giriş yapmalısınız",
                Toast.LENGTH_SHORT
            ).show();
            return;
        }
        String bugun = new java.text.SimpleDateFormat(
            "yyyy-MM-dd",
            java.util.Locale.getDefault()
        ).format(new java.util.Date());
        com.google.firebase.firestore.DocumentReference hakRef = firestore
            .collection("kullanicilar")
            .document(uid)
            .collection(ZAR_HAKKI_COLLECTION)
            .document(bugun);

        hakRef
            .get()
            .addOnSuccessListener(doc -> {
                Long maxDaily = doc.getLong(FIELD_MAX_DAILY);
                long gunlukMax = maxDaily != null ? maxDaily : GUNLUK_HAK;
                Long kalanHak = doc.exists() && doc.contains(FIELD_REMAINING)
                    ? doc.getLong(FIELD_REMAINING)
                    : gunlukMax;
                if (kalanHak > 0) {
                    hakRef.set(
                        java.util.Collections.singletonMap(
                            FIELD_REMAINING,
                            kalanHak - 1
                        ),
                        com.google.firebase.firestore.SetOptions.merge()
                    );
                    tvHakSayisi.setText("Kalan hak: " + (kalanHak - 1));
                    zarAt();
                } else {
                    reklamPaneli.setVisibility(View.VISIBLE);
                }
            });
    }

    private void hakEkle(int miktar) {
        String uid = auth.getUid();
        if (uid == null) return;
        String bugun = new java.text.SimpleDateFormat(
            "yyyy-MM-dd",
            java.util.Locale.getDefault()
        ).format(new java.util.Date());
        com.google.firebase.firestore.DocumentReference hakRef = firestore
            .collection("kullanicilar")
            .document(uid)
            .collection(ZAR_HAKKI_COLLECTION)
            .document(bugun);

        hakRef
            .get()
            .addOnSuccessListener(doc -> {
                Long maxDaily = doc.getLong(FIELD_MAX_DAILY);
                long gunlukMax = maxDaily != null ? maxDaily : GUNLUK_HAK;
                Long kalanHak = doc.exists() && doc.contains(FIELD_REMAINING)
                    ? doc.getLong(FIELD_REMAINING)
                    : 0L;
                hakRef.set(
                    java.util.Collections.singletonMap(
                        FIELD_REMAINING,
                        kalanHak + miktar
                    ),
                    com.google.firebase.firestore.SetOptions.merge()
                );
                tvHakSayisi.setText("Kalan hak: " + (kalanHak + miktar));
                Toast.makeText(
                    this,
                    miktar + " ek hak kazandınız!",
                    Toast.LENGTH_SHORT
                ).show();
            });
    }

    private void hakGoster() {
        String uid = auth.getUid();
        if (uid == null) {
            tvHakSayisi.setText("Giriş yapın");
            return;
        }
        String bugun = new java.text.SimpleDateFormat(
            "yyyy-MM-dd",
            java.util.Locale.getDefault()
        ).format(new java.util.Date());
        com.google.firebase.firestore.DocumentReference hakRef = firestore
            .collection("kullanicilar")
            .document(uid)
            .collection(ZAR_HAKKI_COLLECTION)
            .document(bugun);

        hakRef
            .get()
            .addOnSuccessListener(doc -> {
                Long maxDaily = doc.getLong(FIELD_MAX_DAILY);
                long gunlukMax = maxDaily != null ? maxDaily : GUNLUK_HAK;
                Long kalanHak = doc.exists() && doc.contains(FIELD_REMAINING)
                    ? doc.getLong(FIELD_REMAINING)
                    : gunlukMax;
                tvHakSayisi.setText("Kalan hak: " + kalanHak);
            });
    }


    // Zar atma işlemi (değişiklik yok)
    private void zarAt() {
        if (modeRadioGroup.getCheckedRadioButtonId() == R.id.secenekMode) {
            handleChoiceMode();
        } else {
            handleYesNoMode();
        }
    }

    private void handleChoiceMode() {
        List<String> choices = getChoicesFromUI();
        if (choices.size() < 2) {
            Toast.makeText(
                this,
                getString(R.string.en_az_iki_secenek),
                Toast.LENGTH_SHORT
            ).show();
            return;
        }

        if (checkLocationPermission()) {
            playDiceAnimation(() -> {
                // AI'dan seçim iste
                aiHelper.getChoiceResponse(
                    choices,
                    "",
                    new AIResponseHelper.AICallback() {
                        @Override
                        public void onResponse(String response) {
                            String cevap = extractAnswer(response);
                            String gerekce = extractJustification(response);

                            aiHelper.getEmojiResponse(cevap, new AIResponseHelper.AICallback() {
                                @Override
                                public void onResponse(String emoji) {
                                    String emojiToShow = extractEmojiFromAIResponse(emoji);
                                    runOnUiThread(() -> {
                                        emojiView.setVisibility(View.VISIBLE);
                                        emojiView.setText(emojiToShow);
                                        cevapText.setText(cevap);
                                        gerekceText.setText(gerekce);
                                        gerekceText.setVisibility(View.GONE);

                                        showResultWithDice(
                                            cevap,
                                            (int) (Math.random() * 6) + 1,
                                            emojiToShow
                                        );
                                    });
                                }

                                @Override
                                public void onError(String error) {
                                    runOnUiThread(() -> {
                                        cevapText.setText(cevap);
                                        gerekceText.setText(gerekce);
                                        showResultWithDice(cevap, (int)(Math.random() * 6) + 1, "🎲");
                                    });
                                }
                            });
                        }

                        @Override
                        public void onError(String error) {
                            runOnUiThread(() ->
                                showResultWithDice(
                                    getString(R.string.ai_cevap_alinamadi),
                                    (int) (Math.random() * 6) + 1,
                                    "🎲"
                                )
                            );
                        }
                    }
                );
            });
        }
    }

    private void handleYesNoMode() {
        String question = inputSoru.getText().toString().trim();
        if (question.isEmpty()) {
            Toast.makeText(
                this,
                getString(R.string.lutfen_soru_gir),
                Toast.LENGTH_SHORT
            ).show();
            return;
        }

        if (checkLocationPermission()) {
            playDiceAnimation(() -> {
                // AI'dan evet/hayır iste
                aiHelper.getYesNoResponse(
                    question,
                    "Kullanıcının aşağıdaki sorusuna sadece 'Evet' ya da 'Hayır' cevabı ver.\nSoru:",
                    new AIResponseHelper.AICallback() {
                        @Override
                        public void onResponse(String response) {
                            String cevap = extractAnswer(response);
                            String gerekce = extractJustification(response);

                            aiHelper.getEmojiResponse(cevap, new AIResponseHelper.AICallback() {
                                @Override
                                public void onResponse(String emoji) {
                                    String emojiToShow = extractEmojiFromAIResponse(emoji);
                                    runOnUiThread(() -> {
                                        cevapText.setText(cevap);
                                        gerekceText.setText(gerekce);
                                        gerekceText.setVisibility(View.GONE);
                                        emojiView.setText(emojiToShow);
                                        emojiView.setVisibility(View.VISIBLE);
                                        showResultWithDice(cevap, (int)(Math.random() * 6) + 1, emojiToShow);
                                    });
                                }

                                @Override
                                public void onError(String error) {
                                    runOnUiThread(() -> {
                                        cevapText.setText(cevap);
                                        gerekceText.setText(gerekce);
                                        gerekceText.setVisibility(View.GONE);
                                        emojiView.setText("🎲");
                                        emojiView.setVisibility(View.VISIBLE);
                                        showResultWithDice(cevap, (int)(Math.random() * 6) + 1, "🎲");
                                    });
                                }
                            });
                        }

                        @Override
                        public void onError(String error) {
                            runOnUiThread(() ->
                                showResultWithDice(
                                    getString(R.string.ai_cevap_alinamadi),
                                    (int) (Math.random() * 6) + 1,
                                    "🎲"
                                )
                            );
                        }
                    }
                );
            });
        }
    }

    private boolean checkLocationPermission() {
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
                KONUM_IZIN_KODU
            );
            return false;
        }
        return true;
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
        if (requestCode == KONUM_IZIN_KODU) {
            if (
                grantResults.length > 0 &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED
            ) {
                Log.d(TAG, getString(R.string.konum_izni_verildi_log));
                zarAt(); // İzin verilince zar atmayı tekrar dene
            } else {
                Log.w(TAG, getString(R.string.konum_izni_reddedildi_log));
                Toast.makeText(
                    this,
                    getString(R.string.konum_izni_gerekli_toast),
                    Toast.LENGTH_SHORT
                ).show();
            }
        }
    }

    private List<String> getChoicesFromUI() {
        List<String> choices = new ArrayList<>();
        for (EditText editText : secenekEditTextList) {
            String choice = editText.getText().toString().trim();
            if (!choice.isEmpty()) {
                choices.add(choice);
            }
        }
        return choices;
    }

    private void playDiceAnimation(Runnable callback) {
        dimView.setVisibility(View.GONE);
        sonucKart.setVisibility(View.GONE);
        zarAnim.setProgress(0f);
        zarAnim.playAnimation();
        zarAnim.addAnimatorListener(
            new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    zarAnim.removeAllAnimatorListeners();
                    dimView.setVisibility(View.VISIBLE);
                    sonucKart.setVisibility(View.VISIBLE);
                    callback.run();
                }
            }
        );
    }

    private void showResult(String result) {
        runOnUiThread(() -> {
            sonucMetni.setText(result);
            sonucZarImage.setVisibility(View.GONE);
        });
    }

    private void showResultWithDice(
        String result,
        int diceNumber,
        String emoji
    ) {
        runOnUiThread(() -> {
            int color = getResources().getColor(R.color.white);

            sonucMetni.setText(result);
            sonucMetni.setTextColor(color);
            emojiView.setVisibility(View.VISIBLE);
            emojiView.setText(emoji);

            // aiYorumu'nun görünürlüğünü GONE yap (gerekçeyi başta gösterme)
            if (aiYorumu != null) {
                aiYorumu.setVisibility(View.GONE);
            }

            if (konfetiAnim != null) {
                konfetiAnim.setVisibility(View.GONE);
                konfetiAnim.cancelAnimation();
            }

            sonucZarImage.setVisibility(View.VISIBLE);

            String diceImageName = "dice_" + diceNumber;
            int resId = getResources().getIdentifier(
                diceImageName,
                "drawable",
                getPackageName()
            );
            if (resId != 0) {
                sonucZarImage.setImageResource(resId);
                sonucZarImage.setAlpha(0.25f);
            } else {
                sonucZarImage.setImageResource(R.drawable.dice_1);
                sonucZarImage.setAlpha(0.25f);
            }

            // Sonuç kartı animasyonu
            sonucKart.setScaleX(0.8f);
            sonucKart.setScaleY(0.8f);
            sonucKart.setAlpha(0f);
            sonucKart.setVisibility(View.VISIBLE);
            dimView.setVisibility(View.VISIBLE);
            sonucKart
                .animate()
                .scaleX(1f)
                .scaleY(1f)
                .alpha(1f)
                .setDuration(400)
                .setInterpolator(
                    new android.view.animation.OvershootInterpolator()
                )
                .start();
        });
    }

    private void addNewOption(String hint) {
        EditText editText = new EditText(this);
        int heightInDp = 50;
        int heightInPx = (int) TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            heightInDp,
            getResources().getDisplayMetrics()
        );
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            heightInPx
        );
        editText.setLayoutParams(params);
        editText.setHint(hint);
        editText.setTypeface(ResourcesCompat.getFont(this, R.font.bf1));

        editText.setTextColor(getResources().getColor(R.color.white));
        editText.setHintTextColor(getResources().getColor(R.color.white));
        editText.setBackgroundResource(R.drawable.button1);
        editText.setBackgroundTintList(
            getResources().getColorStateList(R.color.x)
        );
        editText.setPadding(24, 12, 24, 12);
        editText.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        secenekEditTextList.add(editText);
        secenekPanel.addView(editText, secenekPanel.getChildCount() - 1);

        Space space = new Space(this);
        int spaceHeight = (int) TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            8,
            getResources().getDisplayMetrics()
        );
        LinearLayout.LayoutParams spaceParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            spaceHeight
        );
        space.setLayoutParams(spaceParams);
        secenekPanel.addView(space, secenekPanel.getChildCount() - 1);
    }

    private void hideSystemUI() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            getWindow().setDecorFitsSystemWindows(false);
            final WindowInsetsController insetsController =
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
            View decorView = getWindow().getDecorView();
            decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY |
                View.SYSTEM_UI_FLAG_FULLSCREEN |
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            );
            getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
            );
        }
    }

    // AI cevabından "Answer: ..." satırını sadece satır başında olacak şekilde ayıklar, yoksa cevabın tamamını döner
    private String extractAnswer(String aiResponse) {
        if (aiResponse == null) return "";
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
            "(?m)^Answer:\\s*(.*)$",
            java.util.regex.Pattern.MULTILINE
        );
        java.util.regex.Matcher matcher = pattern.matcher(aiResponse);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return aiResponse.trim();
    }

    // AI cevabından "Justification", "Gerekçe", "Açıklama", "Sebep" ve benzeri başlıklarla başlayan gerekçeyi ultra kapsamlı şekilde ayıklar
    private String extractJustification(String aiResponse) {
        if (aiResponse == null) return "";
        Pattern pattern = Pattern.compile(
            "(?i)(?:\\b|\\s)(Justification|Justf?|Reason(?:ing|ıng|ıon|ıngs)?|Explanation|Exp|Açıklama|Açiklama|Sebep|Sebebi|Neden|Nedeni|Rationale|Motive|Motivasyon|Basis|Gerekçe|GEREKÇE|Gerekçesi|Gerekçelendirme|Gerekçelendirmesi|Neden[\\w]*|Açıklam[\\w]*|Açiklam[\\w]*)[\\s\\-:=]+([\\s\\S]+)$",
            Pattern.CASE_INSENSITIVE
        );
        Matcher matcher = pattern.matcher(aiResponse);
        if (matcher.find()) {
            return matcher.group(2).trim();
        }
        return "";
    }

    // AI cevabından "Emoji: ..." satırını sadece satır başında olacak şekilde ayıklar, yoksa ilk emoji veya 🎲 döner
    private String extractEmojiFromAIResponse(String aiResponse) {
        if (aiResponse == null) return "🎲";
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
            "(?m)^Emoji:\\s*([\\p{So}\\p{Cn}\\p{Emoji_Presentation}])",
            java.util.regex.Pattern.MULTILINE
        );
        java.util.regex.Matcher matcher = pattern.matcher(aiResponse);
        if (matcher.find()) {
            return matcher.group(1);
        }
        // Eğer regex ile bulamazsa, ilk emoji karakterini ara
        return extractFirstEmoji(aiResponse);
    }

    // AI cevabından ilk emoji karakterini ayıklar, yoksa fallback olarak 🎲 döner
    private String extractFirstEmoji(String text) {
        if (text == null) return "🎲";
        java.util.regex.Pattern emojiPattern = java.util.regex.Pattern.compile(
            "[\\p{So}\\p{Cn}]"
        );
        java.util.regex.Matcher matcher = emojiPattern.matcher(text);
        if (matcher.find()) {
            return matcher.group();
        }
        return "🎲";
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // (AdMob alanı kaldırıldığı için rewardedAd = null; satırı silindi)
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 333) {
            hakEkle(10); // Kullanıcı paylaşım ekranını açtıysa ek hak ver
        }
    }
}
