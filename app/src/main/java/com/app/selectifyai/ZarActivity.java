package com.app.selectifyai;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.SuppressLint;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.widget.*;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;

import com.airbnb.lottie.LottieAnimationView;

import java.util.ArrayList;
import java.util.List;

public class ZarActivity extends AnaAktivite {
    private LottieAnimationView zarAnim;
    private View dimView;
    private LinearLayout sonucKart;
    private TextView sonucMetni;
    private Button btnTamam, zarAtButton, secenekEkleBtn;
    private RadioGroup modeRadioGroup;
    private LinearLayout secenekPanel, evetHayirPanel, secenekContainer;
    private EditText soruEdit;
    private ImageView sonucZarImage;

    private final List<EditText> secenekEditTextList = new ArrayList<>();
    private static final int MAX_SECENEK = 6;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_zar);
        hideSystemUI();

        zarAnim = findViewById(R.id.zarAnim);
        dimView = findViewById(R.id.dimView);
        sonucKart = findViewById(R.id.sonucKart);
        sonucMetni = findViewById(R.id.sonucMetni);
        btnTamam = findViewById(R.id.btnTamam);
        zarAtButton = findViewById(R.id.zarAtButton);
        modeRadioGroup = findViewById(R.id.modeRadioGroup);
        secenekPanel = findViewById(R.id.secenekPanel);
        evetHayirPanel = findViewById(R.id.evetHayirPanel);
        secenekContainer = findViewById(R.id.secenekPanel);
        soruEdit = findViewById(R.id.soruEdit);
        sonucZarImage = findViewById(R.id.sonucZarImage);

        zarAnim = findViewById(R.id.zarAnim);

// 1. JSON içeriğini yüklemiş olmalı (zaten XML’de tanımladık)
        zarAnim.setProgress(1f); // 1.0 = son kare
        // İlk 2 seçenek
        ekleYeniSecenek(getString(R.string.ilk_secenek));
        ekleYeniSecenek(getString(R.string.ikinci_secenek));

        modeRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.secenekMode) {
                secenekPanel.setVisibility(View.VISIBLE);
                evetHayirPanel.setVisibility(View.GONE);
            } else {
                secenekPanel.setVisibility(View.GONE);
                evetHayirPanel.setVisibility(View.VISIBLE);
            }
        });
        modeRadioGroup.check(R.id.secenekMode);

        // + Butonu
        secenekEkleBtn = new Button(this);
        secenekEkleBtn.setText(getString(R.string.secenek_ekle));
        secenekEkleBtn.setBackgroundResource(R.drawable.button1);
        secenekEkleBtn.setBackgroundTintList(getResources().getColorStateList(R.color.xx));
        secenekEkleBtn.setTextColor(getResources().getColor(R.color.white));
        secenekEkleBtn.setPadding(24, 12, 24, 12);
        secenekEkleBtn.setTypeface(ResourcesCompat.getFont(this, R.font.bf1));
        secenekEkleBtn.setAllCaps(false);
        secenekEkleBtn.setOnClickListener(v -> {
            if (secenekEditTextList.size() < MAX_SECENEK) {
                ekleYeniSecenek((secenekEditTextList.size() + 1) + ". " + getString(R.string.secenek));
            } else {
                Toast.makeText(this, getString(R.string.max_6_secenek), Toast.LENGTH_SHORT).show();
            }
        });
        secenekContainer.addView(secenekEkleBtn);

        zarAtButton.setOnClickListener(v -> {
            if (modeRadioGroup.getCheckedRadioButtonId() == R.id.secenekMode) {
                List<String> secenekler = new ArrayList<>();
                for (EditText et : secenekEditTextList) {
                    String sec = et.getText().toString().trim();
                    if (!sec.isEmpty()) secenekler.add(sec);
                }

                if (secenekler.size() < 2) {
                    Toast.makeText(this, getString(R.string.iki_secenek_gir), Toast.LENGTH_SHORT).show();
                    return;
                }

                playZarAnim(() -> {
                    int zar = (int)(Math.random() * secenekler.size()) + 1;
                    String sonuc = secenekler.get(zar - 1);
                    gosterSonuc(sonuc, zar);
                });

            } else {
                String soru = soruEdit.getText().toString().trim();
                if (soru.isEmpty()) {
                    Toast.makeText(this, getString(R.string.soru_gir), Toast.LENGTH_SHORT).show();
                    return;
                }
                playZarAnim(() -> {
                    int zar = (int)(Math.random() * 6) + 1;
                    String cevap = (zar % 2 == 0) ? getString(R.string.hayir) : getString(R.string.evet);
                    gosterSonuc(cevap, zar);
                });
            }
        });

        btnTamam.setOnClickListener(v -> {
            dimView.setVisibility(View.GONE);
            sonucKart.setVisibility(View.GONE);
        });
    }

    private void hideSystemUI() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            getWindow().setDecorFitsSystemWindows(false);
            WindowInsetsController insetsController = getWindow().getInsetsController();
            if (insetsController != null) {
                insetsController.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
                insetsController.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        } else {
            View decorView = getWindow().getDecorView();
            decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY |
                    View.SYSTEM_UI_FLAG_FULLSCREEN |
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
    }

    private void ekleYeniSecenek(String hint) {
        EditText editText = new EditText(this);
        int heightInDp = 50;
        int heightInPx = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, heightInDp, getResources().getDisplayMetrics());

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, heightInPx);
        editText.setLayoutParams(params);
        editText.setHint(hint);
        editText.setTextColor(getResources().getColor(R.color.white));
        editText.setHintTextColor(getResources().getColor(R.color.white));
        editText.setBackgroundResource(R.drawable.button1);
        editText.setBackgroundTintList(getResources().getColorStateList(R.color.x));
        editText.setPadding(24, 12, 24, 12);
        editText.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        editText.setFontFeatureSettings("ppsregular");

        secenekEditTextList.add(editText);
        secenekContainer.addView(editText, secenekContainer.getChildCount() - 1);

        Space space = new Space(this);
        LinearLayout.LayoutParams spaceParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 16);
        space.setLayoutParams(spaceParams);
        secenekContainer.addView(space, secenekContainer.getChildCount() - 1);
    }

    private void playZarAnim(Runnable sonucCallback) {
        dimView.setVisibility(View.GONE);
        sonucKart.setVisibility(View.GONE);
        zarAnim.setProgress(0f);
        zarAnim.playAnimation();
        zarAnim.addAnimatorListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                dimView.setVisibility(View.VISIBLE);
                sonucKart.setVisibility(View.VISIBLE);
                sonucCallback.run();
                zarAnim.removeAllAnimatorListeners();
            }
        });
    }

    private void gosterSonuc(String text, int zar) {
        sonucMetni.setText(text);
        String zarResimAdi = "dice_" + zar;
        int resId = getResources().getIdentifier(zarResimAdi, "drawable", getPackageName());
        if (resId != 0) {
            sonucZarImage.setImageResource(resId);
            sonucZarImage.setAlpha(0.25f);
            sonucZarImage.setVisibility(View.VISIBLE);
        } else {
            sonucZarImage.setVisibility(View.GONE);
        }
    }
}