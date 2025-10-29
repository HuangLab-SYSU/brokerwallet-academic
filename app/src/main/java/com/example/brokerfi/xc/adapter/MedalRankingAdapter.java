package com.example.brokerfi.xc.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.brokerfi.R;
import com.example.brokerfi.xc.MedalRankingActivity.MedalRankingItem;
import java.util.List;

public class MedalRankingAdapter extends RecyclerView.Adapter<MedalRankingAdapter.ViewHolder> {
    
    private List<MedalRankingItem> rankingList;
    
    public MedalRankingAdapter(List<MedalRankingItem> rankingList) {
        this.rankingList = rankingList;
    }
    
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_medal_ranking, parent, false);
        return new ViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        MedalRankingItem item = rankingList.get(position);
        
        holder.rankText.setText(String.valueOf(item.getRank()));
        holder.displayNameText.setText(item.getDisplayName() != null && !item.getDisplayName().isEmpty() 
            ? item.getDisplayName() : "Anonymous");
        holder.addressText.setText(item.getFormattedAddress());
        holder.goldMedalText.setText(String.valueOf(item.getGoldMedals()));
        holder.silverMedalText.setText(String.valueOf(item.getSilverMedals()));
        holder.bronzeMedalText.setText(String.valueOf(item.getBronzeMedals()));
//        holder.totalMedalText.setText("Total: " + item.getTotalMedalScore());
        
        // Handle representative work display
        if (item.isShowRepresentativeWork() && 
            item.getRepresentativeWork() != null && 
            !item.getRepresentativeWork().trim().isEmpty()) {
            holder.representativeWorkText.setText("Contribution: " + item.getRepresentativeWork());
            holder.representativeWorkText.setVisibility(View.VISIBLE);
        } else {
            holder.representativeWorkText.setVisibility(View.GONE);
        }
    }
    
    @Override
    public int getItemCount() {
        return rankingList != null ? rankingList.size() : 0;
    }
    
    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView rankText;
        TextView displayNameText;
        TextView addressText;
        TextView goldMedalText;
        TextView silverMedalText;
        TextView bronzeMedalText;
//        TextView totalMedalText;
        TextView representativeWorkText;
        
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            rankText = itemView.findViewById(R.id.rankText);
            displayNameText = itemView.findViewById(R.id.displayNameText);
            addressText = itemView.findViewById(R.id.addressText);
            goldMedalText = itemView.findViewById(R.id.goldMedalText);
            silverMedalText = itemView.findViewById(R.id.silverMedalText);
            bronzeMedalText = itemView.findViewById(R.id.bronzeMedalText);
//            totalMedalText = itemView.findViewById(R.id.totalMedalText);
            representativeWorkText = itemView.findViewById(R.id.representativeWorkText);
        }
    }
}