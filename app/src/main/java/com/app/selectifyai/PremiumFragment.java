package com.app.selectifyai;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class PremiumFragment extends Fragment {

    private Context localizedContext;

    @Override
    public void onAttach(@NonNull Context context) {
        localizedContext = DilHelper.wrap(context);
        super.onAttach(localizedContext);
    }

    @Nullable
    @Override
    public View onCreateView(
        @NonNull LayoutInflater inflater,
        @Nullable ViewGroup container,
        @Nullable Bundle savedInstanceState
    ) {
        LayoutInflater localInflater = inflater.cloneInContext(
            localizedContext
        );
        View view = localInflater.inflate(
            R.layout.fragment_premium,
            container,
            false
        );

        view
            .findViewById(R.id.btnSatinAl)
            .setOnClickListener(v -> {
                String url = "https://random.itch.io/";
                Intent browserIntent = new Intent(
                    Intent.ACTION_VIEW,
                    android.net.Uri.parse(url)
                );
                startActivity(browserIntent);
            });

        return view;
    }
}
