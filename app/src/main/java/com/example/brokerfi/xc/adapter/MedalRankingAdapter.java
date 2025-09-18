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
        holder.addressText.setText(item.getAddress());
        holder.goldMedalText.setText("金牌: " + item.getGold());
        holder.silverMedalText.setText("银牌: " + item.getSilver());
        holder.bronzeMedalText.setText("铜牌: " + item.getBronze());
        holder.totalMedalText.setText("总计: " + item.getTotal());
    }
    
    @Override
    public int getItemCount() {
        return rankingList != null ? rankingList.size() : 0;
    }
    
    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView rankText;
        TextView addressText;
        TextView goldMedalText;
        TextView silverMedalText;
        TextView bronzeMedalText;
        TextView totalMedalText;
        
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            rankText = itemView.findViewById(R.id.rankText);
            addressText = itemView.findViewById(R.id.addressText);
            goldMedalText = itemView.findViewById(R.id.goldMedalText);
            silverMedalText = itemView.findViewById(R.id.silverMedalText);
            bronzeMedalText = itemView.findViewById(R.id.bronzeMedalText);
            totalMedalText = itemView.findViewById(R.id.totalMedalText);
        }
    }
}