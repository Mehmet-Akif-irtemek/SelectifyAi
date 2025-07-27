package com.app.selectifyai;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.NumberPicker;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.lang.reflect.Field;

public class YasFragment extends Fragment {
    private NumberPicker sayiSecici;
    private boolean secildiMi = false; // kullanıcı dokundu mu?

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_yas, container, false);

        sayiSecici = v.findViewById(R.id.sayiSecici);
        sayiSecici.setMinValue(6);
        sayiSecici.setMaxValue(100);
        sayiSecici.setWrapSelectorWheel(false);

        // Yazı rengini beyaz yap
        for (int i = 0; i < sayiSecici.getChildCount(); i++) {
            View child = sayiSecici.getChildAt(i);
            if (child instanceof EditText) {
                try {
                    ((EditText) child).setTextColor(Color.WHITE);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        // Ayırıcı çizgileri beyaz yap
        try {
            @SuppressLint("SoonBlockedPrivateApi") Field dividerField = NumberPicker.class.getDeclaredField("mSelectionDivider");
            dividerField.setAccessible(true);
            dividerField.set(sayiSecici, new ColorDrawable(Color.WHITE));
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Değer seçildiğinde butonu aktif et
        sayiSecici.setOnValueChangedListener((picker, oldVal, newVal) -> {
            secildiMi = true;
            if (getActivity() instanceof OnboardingActivity) {
                ((OnboardingActivity) getActivity()).ileriButonAktifMi(true);
            }
        });

        // İlk başta buton pasif
        if (getActivity() instanceof OnboardingActivity) {
            ((OnboardingActivity) getActivity()).ileriButonAktifMi(false);
        }

        return v;
    }

    @Override
    public void onPause() {
        super.onPause();
        if (secildiMi) {
            int yas = sayiSecici.getValue();
            FirebaseFirestore.getInstance()
                    .collection("kullanicilar")
                    .document(FirebaseAuth.getInstance().getUid())
                    .update("yas", yas);
        }
    }
}