package com.app.selectifyai;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import android.content.Context;

public class SorumlulukFragment extends Fragment {
    private EditText editSorumluluk;
    private boolean girildiMi = false;
    // private Context localizedContext; // KALDIRILDI

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context); // Sadece orijinal context
        // localizedContext = DilHelper.wrap(context); // KALDIRILDI
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_sorumluluk, container, false); // Orijinal inflater
        editSorumluluk = v.findViewById(R.id.editSorumluluk);

        editSorumluluk.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                boolean dolu = !TextUtils.isEmpty(s.toString().trim());
                if (getActivity() instanceof OnboardingActivity) {
                    ((OnboardingActivity) getActivity()).ileriButonAktifMi(dolu);
                }
                girildiMi = dolu;
            }
        });

        return v;
    }

    @Override
    public void onPause() {
        super.onPause();
        if (girildiMi) {
            String sorumluluk = editSorumluluk.getText().toString().trim();
            FirebaseFirestore.getInstance()
                    .collection("kullanicilar")
                    .document(FirebaseAuth.getInstance().getUid())
                    .update("sorumluluk", sorumluluk);
        }
    }
}