package com.app.selectifyai;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class KonumIzniFragment extends Fragment {
    private static final int KONUM_IZNI_ISTEK = 2001;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_konum_izni, container, false);
        ImageButton btn = v.findViewById(R.id.btnIzinVer);
        btn.setOnClickListener(view -> izinIste());
        return v;
    }

    private void izinIste() {
        if (ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            requestPermissions(
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    KONUM_IZNI_ISTEK
            );
        } else {
            izinSonucunuKaydet(true);
        }
    }

    @Override
    public void onRequestPermissionsResult(int rc, @NonNull String[] per, @NonNull int[] grant) {
        if (rc == KONUM_IZNI_ISTEK) {
            boolean verildi = grant.length > 0 && grant[0] == PackageManager.PERMISSION_GRANTED;
            izinSonucunuKaydet(verildi);
        }
    }

    private void izinSonucunuKaydet(boolean verildi) {
        FirebaseFirestore.getInstance()
                .collection("kullanicilar")
                .document(FirebaseAuth.getInstance().getUid())
                .update("konumIzniVerildi", verildi);
    }
}