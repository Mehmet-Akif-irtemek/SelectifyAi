package com.app.selectifyai;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class PremiumPagerAdapter extends FragmentStateAdapter {

    public PremiumPagerAdapter(@NonNull FragmentActivity fa) {
        super(fa);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0: return new FreeFragment();
            case 1: return new PremiumFragment();
            case 2: return new ProFragment();
            default: return new FreeFragment();
        }
    }

    @Override
    public int getItemCount() {
        return 3;
    }
}