package com.example.brokerfi.token;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.example.brokerfi.R;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TokenTxAdapter extends RecyclerView.Adapter<TokenTxAdapter.Holder> {

    public interface OnRecordClickListener {
        void onRecordClick(TokenTxRecord record);
    }

    private static final int COLOR_AMOUNT = Color.parseColor("#000000");

    private final List<TokenTxRecord> records = new ArrayList<>();
    private final SimpleDateFormat timeFormat =
            new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
    @Nullable
    private OnRecordClickListener listener;

    public void setOnRecordClickListener(@Nullable OnRecordClickListener listener) {
        this.listener = listener;
    }

    public void setRecords(List<TokenTxRecord> items) {
        records.clear();
        if (items != null) {
            records.addAll(items);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_token_tx, parent, false);
        return new Holder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        TokenTxRecord record = records.get(position);
        TokenTxDisplayHelper.bindType(
                holder.itemView.getContext(),
                holder.typeText,
                record);
        TokenTxDisplayHelper.bindAmount(
                holder.itemView.getContext(),
                holder.amountText,
                holder.swapAmountRow,
                holder.swapFromText,
                holder.swapToText,
                record,
                false);
        holder.amountText.setTextColor(COLOR_AMOUNT);
        holder.swapFromText.setTextColor(COLOR_AMOUNT);
        holder.swapToText.setTextColor(COLOR_AMOUNT);
        holder.timeText.setText(timeFormat.format(new Date(record.timestampMs)));

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onRecordClick(record);
            }
        });
    }

    @Override
    public int getItemCount() {
        return records.size();
    }

    static final class Holder extends RecyclerView.ViewHolder {
        final TextView typeText;
        final TextView amountText;
        final View swapAmountRow;
        final TextView swapFromText;
        final TextView swapToText;
        final TextView timeText;

        Holder(@NonNull View itemView) {
            super(itemView);
            typeText = itemView.findViewById(R.id.token_tx_type);
            amountText = itemView.findViewById(R.id.token_tx_amount);
            swapAmountRow = itemView.findViewById(R.id.token_tx_swap_amount);
            swapFromText = itemView.findViewById(R.id.token_tx_swap_from);
            swapToText = itemView.findViewById(R.id.token_tx_swap_to);
            timeText = itemView.findViewById(R.id.token_tx_time);
        }
    }
}


