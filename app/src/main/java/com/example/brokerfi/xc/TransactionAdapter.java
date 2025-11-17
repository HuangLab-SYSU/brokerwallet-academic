package com.example.brokerfi.xc;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.brokerfi.R;
import com.example.brokerfi.xc.model.Transaction;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class TransactionAdapter extends RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder> {

    private List<Transaction> transactions;
    private String currentAddress; // 当前用户的地址，用于判断交易方向，通过 from to 匹配当前地址来判断

    public TransactionAdapter(List<Transaction> transactions, String currentAddress) {
        this.transactions = transactions;
        this.currentAddress = currentAddress.toLowerCase();
        Log.d("TransactionAdapter", "初始化适配器，当前地址: " + currentAddress);
    }

    @NonNull
    @Override
    public TransactionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Log.d("TransactionAdapter", "创建ViewHolder");
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.transaction_item, parent, false);
        return new TransactionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TransactionViewHolder holder, int position) {
        Log.d("TransactionAdapter", "绑定数据到ViewHolder，位置: " + position);
        Transaction transaction = transactions.get(position);
        Log.d("TransactionAdapter", "交易信息 - 从: " + transaction.getFrom() + ", 到: " + transaction.getTo() + ", 金额: " + transaction.getValue());

        // 判断交易类型（发送或接收）
        String type = "UNKNOWN";
        String amount = transaction.getValue();

        // 将wei转换为BKC（1 BKC = 1e18 wei）
        try {
            double valueInBKC = Double.parseDouble(amount) / 1e18;
            amount = String.format("%.6f", valueInBKC);
        } catch (NumberFormatException e) {
            amount = "0.0";
        }

        if (transaction.getFrom() != null && transaction.getFrom().toLowerCase().equals(currentAddress)) {
            type = "SEND";
            holder.transactionAmount.setText("- " + amount + " BKC");
            holder.transactionAmount.setTextColor(android.graphics.Color.parseColor("#FF6B6B"));
        } else if (transaction.getTo() != null && transaction.getTo().toLowerCase().equals(currentAddress)) {
            type = "RECEIVE";
            holder.transactionAmount.setText("+ " + amount + " BKC");
            holder.transactionAmount.setTextColor(android.graphics.Color.WHITE);
        } else if (transaction.getFrom() != null && transaction.getFrom().equals("Faucet")) {
            type = "FAUCET";
            holder.transactionAmount.setText("+ " + amount + " BKC");
            holder.transactionAmount.setTextColor(android.graphics.Color.WHITE);
        }

        holder.transactionType.setText(type);

        // 格式化时间，dashboard返回的时间格式非标准时间格式
        String formattedTime = formatTimestamp(transaction.getTimestamp());
        holder.transactionTime.setText(formattedTime);

        // 设置其他信息
        holder.transactionFrom.setText(transaction.getFrom() != null ? transaction.getFrom() : "-");
        holder.transactionTo.setText(transaction.getTo() != null ? transaction.getTo() : "-");
        holder.transactionId.setText(transaction.getId());

        // 格式化Gasfee
        String fee = transaction.getFee();
        if (fee != null && !fee.isEmpty()) {
            try {
                double feeInBKC = Double.parseDouble(fee) / 1e18;
                holder.transactionFee.setText(String.format("%.6f", feeInBKC) + " BKC");
            } catch (NumberFormatException e) {
                holder.transactionFee.setText("0.0 BKC");
            }
        } else {
            holder.transactionFee.setText("0.0 BKC");
        }
    }

    @Override
    public int getItemCount() {
        int count = transactions != null ? transactions.size() : 0;
        Log.d("TransactionAdapter", "获取项目数量: " + count);
        return count;
    }

    // 更新数据集
    public void updateTransactions(List<Transaction> newTransactions) {
        Log.d("TransactionAdapter", "更新交易数据，新交易数量: " + (newTransactions != null ? newTransactions.size() : 0));
        this.transactions = newTransactions;
        notifyDataSetChanged();
    }

    static class TransactionViewHolder extends RecyclerView.ViewHolder {
        TextView transactionType;
        TextView transactionAmount;
        TextView transactionStatus;
        TextView transactionFrom;
        TextView transactionTo;
        TextView transactionTime;
        TextView transactionFee;
        TextView transactionId;

        public TransactionViewHolder(@NonNull View itemView) {
            super(itemView);
            transactionType = itemView.findViewById(R.id.transaction_type);
            transactionAmount = itemView.findViewById(R.id.transaction_amount);
            transactionStatus = itemView.findViewById(R.id.transaction_status);
            transactionFrom = itemView.findViewById(R.id.transaction_from);
            transactionTo = itemView.findViewById(R.id.transaction_to);
            transactionTime = itemView.findViewById(R.id.transaction_time);
            transactionFee = itemView.findViewById(R.id.transaction_fee);
            transactionId = itemView.findViewById(R.id.transaction_id);
        }
    }

    // 格式化时间戳
    private String formatTimestamp(String timestamp) {
        if (timestamp == null || timestamp.isEmpty()) {
            return "-";
        }

        try {
            // 解析ISO 8601格式
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
            Date date = inputFormat.parse(timestamp);

            // 输出格式化的时间
            SimpleDateFormat outputFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
            return outputFormat.format(date);
        } catch (ParseException e) {
            try {
                // 其他格式
                SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
                Date date = inputFormat.parse(timestamp);
                SimpleDateFormat outputFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
                return outputFormat.format(date);
            } catch (ParseException ex) {
                return timestamp; // 解析不了则返回初始时间戳
            }
        }
    }
}