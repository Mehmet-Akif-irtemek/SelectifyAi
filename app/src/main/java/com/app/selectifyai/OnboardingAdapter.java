package com.app.selectifyai;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import java.util.List;

public class OnboardingAdapter extends FragmentStateAdapter {
    private final List<Fragment> fragmentler;

    public OnboardingAdapter(@NonNull FragmentActivity fa, List<Fragment> fragmentler) {
        super(fa);
        this.fragmentler = fragmentler;
    }

    @NonNull
    @Override
    public Fragment createFragment(int pozisyon) {
        return fragmentler.get(pozisyon);
    }

    @Override
    public int getItemCount() {
        return fragmentler.size();
    }
}