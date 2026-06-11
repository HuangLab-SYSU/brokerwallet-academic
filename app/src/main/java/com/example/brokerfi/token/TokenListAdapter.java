package com.example.brokerfi.token;

import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.example.brokerfi.R;

import java.util.ArrayList;
import java.util.List;

public class TokenListAdapter extends RecyclerView.Adapter<TokenListAdapter.Holder> {

    public interface Listener {
        void onEnabledChanged(TokenItem item, boolean enabled);
    }

    private final List<TokenItem> items = new ArrayList<>();
    @Nullable
    private Listener listener;

    public void setListener(@Nullable Listener listener) {
        this.listener = listener;
    }

    public void submitList(List<TokenItem> data) {
        items.clear();
        if (data != null) {
            items.addAll(data);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_token, parent, false);
        return new Holder(view, this);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    void dispatchEnabledChange(TokenItem item, boolean enabled) {
        if (listener != null) {
            listener.onEnabledChanged(item, enabled);
        }
    }

    static final class Holder extends RecyclerView.ViewHolder {

        private final TokenListAdapter adapter;
        private final TextView icon;
        private final TextView name;
        private final TextView symbol;
        private final FrameLayout switchWrap;
        private final FrameLayout switchTrack;
        private final View switchThumb;
        private final int thumbInsetPx;
        @Nullable
        private TokenItem boundItem;

        Holder(@NonNull View itemView, TokenListAdapter adapter) {
            super(itemView);
            this.adapter = adapter;
            icon = itemView.findViewById(R.id.token_icon);
            name = itemView.findViewById(R.id.token_name);
            symbol = itemView.findViewById(R.id.token_symbol);
            switchWrap = itemView.findViewById(R.id.token_switch_wrap);
            switchTrack = itemView.findViewById(R.id.token_switch_track);
            switchThumb = itemView.findViewById(R.id.token_switch_thumb);
            thumbInsetPx = Math.round(3f * itemView.getResources().getDisplayMetrics().density);

            switchWrap.setOnTouchListener((v, event) -> {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    v.getParent().requestDisallowInterceptTouchEvent(true);
                }
                return false;
            });

            switchWrap.setOnClickListener(v -> onSwitchAreaClick());
        }

        void bind(TokenItem item) {
            boundItem = item;
            name.setText(item.getName());
            symbol.setText(item.displaySubtitle());

            String letter = "?";
            if (!TextUtils.isEmpty(item.getSymbol())) {
                letter = item.getSymbol().substring(0, 1).toUpperCase();
            }
            icon.setText(letter);

            boolean builtIn = item.isBuiltIn();
            applyToggleVisual(builtIn || item.isEnabled());
            boolean interactive = !builtIn;
            switchWrap.setEnabled(interactive);
            switchWrap.setClickable(interactive);
            switchWrap.setFocusable(interactive);
            switchWrap.setAlpha(1f);
        }

        private void onSwitchAreaClick() {
            TokenItem item = boundItem;
            if (item == null || item.isBuiltIn()) {
                return;
            }
            boolean next = !item.isEnabled();
            item.setEnabled(next);
            applyToggleVisual(next);
            adapter.dispatchEnabledChange(item, next);
        }

        private void applyToggleVisual(boolean checked) {
            switchTrack.setBackgroundResource(
                    checked ? R.drawable.token_toggle_track_on : R.drawable.token_toggle_track_off);
            FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) switchThumb.getLayoutParams();
            if (checked) {
                lp.gravity = Gravity.CENTER_VERTICAL | Gravity.END;
                lp.setMarginStart(0);
                lp.setMarginEnd(thumbInsetPx);
            } else {
                lp.gravity = Gravity.CENTER_VERTICAL | Gravity.START;
                lp.setMarginStart(thumbInsetPx);
                lp.setMarginEnd(0);
            }
            switchThumb.setLayoutParams(lp);
        }
    }
}


