package com.app.selectifyai;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class Profil extends AnaAktivite {

    private TextView textViewAdMevcut, textViewSorumlulukMevcut;
    private EditText editTextAd, editTextSorumluluk;
    private Button buttonKaydet, buttonPPYukle;
    private ImageView imageViewPP;

    private final FirebaseAuth auth = FirebaseAuth.getInstance();
    private final FirebaseFirestore firestore = FirebaseFirestore.getInstance();
    private final FirebaseStorage storage = FirebaseStorage.getInstance();

    private Uri ppUri = null;
    private final int PICK_IMAGE_CODE = 2001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profil);

        textViewAdMevcut = findViewById(R.id.textViewAdMevcut);
        textViewSorumlulukMevcut = findViewById(R.id.textViewSorumlulukMevcut);
        editTextAd = findViewById(R.id.editTextAd);
        editTextSorumluluk = findViewById(R.id.editTextSorumluluk);
        buttonKaydet = findViewById(R.id.buttonKaydet);
        imageViewPP = findViewById(R.id.imageViewPP);
        buttonPPYukle = findViewById(R.id.buttonPPYukle);
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setSelectedItemId(R.id.nav_settings); // aktif olan item

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_tasks) {
                startActivity(new Intent(this, TodoActivity.class));
                return true;
            } else if (id == R.id.nav_home) {
                startActivity(new Intent(this, AnaEkran.class));
                return true;
            }
            return true;
        });
        String uid = auth.getUid();
        if (uid != null) {
            firestore.collection("kullanicilar").document(uid).get().addOnSuccessListener(doc -> {
                textViewAdMevcut.setText(getString(R.string.mevcut_ad, doc.getString("kullaniciAdi")));
                textViewSorumlulukMevcut.setText(getString(R.string.mevcut_sorumluluk, doc.getString("sorumluluk")));

                StorageReference ppRef = storage.getReference().child("pp/" + uid + ".jpg");
                ppRef.getDownloadUrl().addOnSuccessListener(uri ->
                        Glide.with(this)
                                .load(uri)
                                .circleCrop()
                                .into(imageViewPP)
                );
            });
        }

        hideSystemUI();

        buttonPPYukle.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            startActivityForResult(intent, PICK_IMAGE_CODE);
        });

        buttonKaydet.setOnClickListener(v -> {
            String yeniAd = editTextAd.getText().toString().trim();
            String yeniSorumluluk = editTextSorumluluk.getText().toString().trim();
            Map<String, Object> guncelle = new HashMap<>();
            if (!yeniAd.isEmpty()) guncelle.put("kullaniciAdi", yeniAd);
            if (!yeniSorumluluk.isEmpty()) guncelle.put("sorumluluk", yeniSorumluluk);

            if (!guncelle.isEmpty()) {
                firestore.collection("kullanicilar").document(uid)
                        .update(guncelle)
                        .addOnSuccessListener(unused -> {
                            Toast.makeText(this, getString(R.string.profil_guncellendi), Toast.LENGTH_SHORT).show();
                            if (guncelle.containsKey("kullaniciAdi"))
                                textViewAdMevcut.setText(getString(R.string.mevcut_ad, yeniAd));
                            if (guncelle.containsKey("sorumluluk"))
                                textViewSorumlulukMevcut.setText(getString(R.string.mevcut_sorumluluk, yeniSorumluluk));
                        });
            }

            if (ppUri != null) {
                profilFotoDegistir(uid);
            }
        });
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

    private void profilFotoDegistir(String uid) {
        Cursor returnCursor = getContentResolver().query(ppUri, null, null, null, null);
        int sizeIndex = returnCursor.getColumnIndex(OpenableColumns.SIZE);
        returnCursor.moveToFirst();
        long sizeInBytes = returnCursor.getLong(sizeIndex);
        returnCursor.close();

        if (sizeInBytes > 2 * 1024 * 1024) {
            Toast.makeText(this, getString(R.string.fotograf_buyuk), Toast.LENGTH_SHORT).show();
            return;
        }

        String bugun = new SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(new Date());
        DocumentReference docRef = firestore.collection("kullanicilar").document(uid);

        docRef.get().addOnSuccessListener(doc -> {
            Map<String, Object> degistirme = (Map<String, Object>) doc.get("profilDegistirme");
            String kayitliTarih = degistirme != null ? (String) degistirme.get("tarih") : "";
            Long sayi = degistirme != null ? (Long) degistirme.get("sayi") : 0;

            if (!bugun.equals(kayitliTarih)) sayi = 0L;
            if (sayi >= 3) {
                Toast.makeText(this, getString(R.string.fotograf_limit), Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), ppUri);
                Bitmap scaled = Bitmap.createScaledBitmap(bitmap, 512, 512, true);

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                scaled.compress(Bitmap.CompressFormat.JPEG, 70, baos);
                byte[] data = baos.toByteArray();

                StorageReference ppRef = storage.getReference().child("pp/" + uid + ".jpg");

                Long finalSayi = sayi;
                ppRef.delete().addOnCompleteListener(task -> {
                    ppRef.putBytes(data).addOnSuccessListener(taskSnapshot -> {
                        ppRef.getDownloadUrl().addOnSuccessListener(uri -> {
                            Map<String, Object> guncelle = new HashMap<>();
                            guncelle.put("profilFotoUrl", uri.toString());

                            Map<String, Object> degisim = new HashMap<>();
                            degisim.put("tarih", bugun);
                            degisim.put("sayi", finalSayi + 1);
                            guncelle.put("profilDegistirme", degisim);

                            firestore.collection("kullanicilar").document(uid)
                                    .update(guncelle)
                                    .addOnSuccessListener(unused ->
                                            Glide.with(this)
                                                    .load(uri)
                                                    .circleCrop()
                                                    .into(imageViewPP)
                                    );
                        });
                    });
                });

            } catch (Exception e) {
                Toast.makeText(this, getString(R.string.fotograf_islenemedi, e.getMessage()), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_CODE && resultCode == RESULT_OK && data != null) {
            ppUri = data.getData();
            Glide.with(this)
                    .load(ppUri)
                    .circleCrop()
                    .into(imageViewPP);
        }
    }
}