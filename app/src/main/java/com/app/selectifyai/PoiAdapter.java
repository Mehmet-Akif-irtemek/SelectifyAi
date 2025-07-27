package com.app.selectifyai;

import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class PoiAdapter extends RecyclerView.Adapter<PoiAdapter.PoiViewHolder> {
    private final List<PoiModel> poiList;

    public PoiAdapter(List<PoiModel> poiList) {
        this.poiList = poiList;
    }

    @NonNull
    @Override
    public PoiViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_poi, parent, false);
        return new PoiViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull PoiViewHolder holder, int position) {
        PoiModel model = poiList.get(position);
        holder.txtAd.setText(model.getAd());

        if (model.getTur().equals("kategori")) {
            holder.txtTur.setText("");
        } else {
            String mesafeStr = model.getMesafe() >= 0 ?
                String.format(holder.itemView.getContext().getString(R.string.mesafe_ek), (int) model.getMesafe()) : "";
            holder.txtTur.setText(String.format(holder.itemView.getContext().getString(R.string.tur_mesafe), model.getTur(), mesafeStr));
        }

        // TIKLANINCA HARİTA AÇ
        holder.itemView.setOnClickListener(v -> {
            if (!model.getTur().equals("kategori")) {
                String uri = "google.navigation:q=" + model.getLat() + "," + model.getLon();
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
                intent.setPackage("com.google.android.apps.maps");
                try {
                    v.getContext().startActivity(intent);
                } catch (Exception e) {
                    Toast.makeText(v.getContext(), R.string.kedi, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return poiList.size();
    }

    static class PoiViewHolder extends RecyclerView.ViewHolder {
        TextView txtAd, txtTur;

        public PoiViewHolder(@NonNull View itemView) {
            super(itemView);
            txtAd = itemView.findViewById(R.id.textPoiAd);
            txtTur = itemView.findViewById(R.id.textPoiTur);
        }
    }
}