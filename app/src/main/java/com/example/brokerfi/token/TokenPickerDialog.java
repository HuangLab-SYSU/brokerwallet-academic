package com.example.brokerfi.token;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.brokerfi.R;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.util.ArrayList;
import java.util.List;

/** Bottom-sheet picker for enabled tokens. */
public final class TokenPickerDialog {

    public interface Listener {
        void onTokenSelected(TokenItem item);
    }

    private TokenPickerDialog() {
    }

    public static void show(
            AppCompatActivity activity,
            List<TokenItem> tokens,
            String selectedContract,
            Listener listener) {
        if (activity == null || tokens == null || tokens.isEmpty()) {
            return;
        }
        BottomSheetDialog dialog = new BottomSheetDialog(activity);
        View content = LayoutInflater.from(activity).inflate(R.layout.dialog_token_picker, null);
        dialog.setContentView(content);

        RecyclerView list = content.findViewById(R.id.token_picker_list);
        list.setLayoutManager(new LinearLayoutManager(activity));
        list.setAdapter(new RecyclerView.Adapter<PickerHolder>() {
            @NonNull
            @Override
            public PickerHolder onCreateViewHolder(@NonNull android.view.ViewGroup parent, int viewType) {
                View row = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_token_picker, parent, false);
                return new PickerHolder(row);
            }

            @Override
            public void onBindViewHolder(@NonNull PickerHolder holder, int position) {
                TokenItem item = tokens.get(position);
                holder.name.setText(item.getName());
                holder.symbol.setText(item.displaySubtitle());
                String letter = TokenItem.iconLetter(item.getSymbol());
                holder.icon.setText(letter);
                boolean selected = selectedContract != null
                        && selectedContract.equals(item.getContractAddress());
                holder.itemView.setActivated(selected);
                holder.check.setVisibility(selected ? View.VISIBLE : View.GONE);
                holder.itemView.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onTokenSelected(item);
                    }
                    dialog.dismiss();
                });
            }

            @Override
            public int getItemCount() {
                return tokens.size();
            }
        });
        dialog.show();
    }

    public interface SwapListener {
        void onOptionSelected(SwapTokenOption option);
    }

    /** Swap picker: only native BKC and the official wrapped BKC token are allowed. */
    public static void showForSwap(
            AppCompatActivity activity,
            List<TokenItem> tokens,
            String officialwrappedBkcContract,
            String selectedKey,
            SwapListener listener) {
        if (activity == null) {
            return;
        }
        List<SwapTokenOption> options = new ArrayList<>();
        options.add(SwapTokenOption.nativeBkc());
        TokenItem official = null;
        if (tokens != null && !TextUtils.isEmpty(officialwrappedBkcContract)) {
            for (TokenItem item : tokens) {
                if (item.matchesOfficialwrappedBkc(officialwrappedBkcContract)) {
                    official = item;
                    break;
                }
            }
        }
        if (official == null && !TextUtils.isEmpty(officialwrappedBkcContract)) {
            official = TokenItem.builtInwrappedBkc(officialwrappedBkcContract);
        }
        if (official != null) {
            options.add(SwapTokenOption.fromToken(official));
        }
        BottomSheetDialog dialog = new BottomSheetDialog(activity);
        View content = LayoutInflater.from(activity).inflate(R.layout.dialog_token_picker, null);
        dialog.setContentView(content);

        RecyclerView list = content.findViewById(R.id.token_picker_list);
        list.setLayoutManager(new LinearLayoutManager(activity));
        list.setAdapter(new RecyclerView.Adapter<PickerHolder>() {
            @NonNull
            @Override
            public PickerHolder onCreateViewHolder(@NonNull android.view.ViewGroup parent, int viewType) {
                View row = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_token_picker, parent, false);
                return new PickerHolder(row);
            }

            @Override
            public void onBindViewHolder(@NonNull PickerHolder holder, int position) {
                SwapTokenOption option = options.get(position);
                holder.name.setText(option.getName());
                holder.symbol.setText(option.getSymbol());
                String letter = TokenItem.iconLetter(option.getSymbol());
                holder.icon.setText(letter);
                boolean selected = selectedKey != null && selectedKey.equals(option.selectionKey());
                holder.itemView.setActivated(selected);
                holder.check.setVisibility(selected ? View.VISIBLE : View.GONE);
                holder.itemView.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onOptionSelected(option);
                    }
                    dialog.dismiss();
                });
            }

            @Override
            public int getItemCount() {
                return options.size();
            }
        });
        dialog.show();
    }

    private static final class PickerHolder extends RecyclerView.ViewHolder {
        final TextView icon;
        final TextView name;
        final TextView symbol;
        final ImageView check;

        PickerHolder(@NonNull View itemView) {
            super(itemView);
            icon = itemView.findViewById(R.id.token_picker_icon);
            name = itemView.findViewById(R.id.token_picker_name);
            symbol = itemView.findViewById(R.id.token_picker_symbol);
            check = itemView.findViewById(R.id.token_picker_check);
        }
    }
}
