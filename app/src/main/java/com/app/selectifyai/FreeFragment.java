package com.app.selectifyai;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class FreeFragment extends Fragment {

    private Context localizedContext;

    @Override
    public void onAttach(@NonNull Context context) {
        localizedContext = DilHelper.wrap(context);
        super.onAttach(localizedContext);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        LayoutInflater localInflater = inflater.cloneInContext(localizedContext);
        return localInflater.inflate(R.layout.fragment_free, container, false);
    }
}