package com.example.brokerfi.token;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import com.example.brokerfi.R;
import com.example.brokerfi.xc.ChainAddressUtil;

/** Address-entry helper for paste, 0x normalization, validation, and caret reset. */
public final class TokenAddressInputHelper {

    public interface OnAddressChanged {
        void onAddressChanged();
    }

    private TokenAddressInputHelper() {
    }

    public static void bindPasteButton(
            ImageButton pasteButton, EditText field, Context context, OnAddressChanged onChanged) {
        if (pasteButton == null || field == null) {
            return;
        }
        pasteButton.setOnClickListener(v -> pasteFromClipboard(context, field, onChanged));
    }

    public static void bindAddressField(EditText field, OnAddressChanged onChanged) {
        if (field == null) {
            return;
        }
        configureAddressField(field);
        TextWatcher watcher = new TextWatcher() {
            private boolean internalChange;
            private int lastBulkInsertStart = -1;
            private int lastBulkInsertCount;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (!internalChange && count > 1) {
                    lastBulkInsertStart = start;
                    lastBulkInsertCount = count;
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (internalChange) {
                    return;
                }
                if (lastBulkInsertStart >= 0) {
                    int start = lastBulkInsertStart;
                    int end = start + lastBulkInsertCount;
                    lastBulkInsertStart = -1;
                    lastBulkInsertCount = 0;
                    if (end <= s.length()) {
                        String inserted = s.subSequence(start, end).toString();
                        applyNormalizedPaste(field, inserted, onChanged, v -> internalChange = v);
                        return;
                    }
                }
                notifyChanged(onChanged);
            }
        };
        field.addTextChangedListener(watcher);
        field.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                normalizeFieldInPlace(field, onChanged);
            }
        });
    }

    public static boolean pasteFromClipboard(
            Context context, EditText field, OnAddressChanged onChanged) {
        if (context == null || field == null) {
            return false;
        }
        ClipboardManager clipboard =
                (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard == null || !clipboard.hasPrimaryClip()) {
            Toast.makeText(context, R.string.token_clipboard_empty, Toast.LENGTH_SHORT).show();
            return false;
        }
        ClipData clip = clipboard.getPrimaryClip();
        if (clip == null || clip.getItemCount() == 0 || clip.getItemAt(0).getText() == null) {
            Toast.makeText(context, R.string.token_clipboard_empty, Toast.LENGTH_SHORT).show();
            return false;
        }
        String text = clip.getItemAt(0).getText().toString();
        String normalized = normalizePastedText(text);
        if (TextUtils.isEmpty(normalized)) {
            Toast.makeText(context, R.string.token_scan_invalid_address, Toast.LENGTH_SHORT).show();
            return false;
        }
        applyAddressToField(field, normalized);
        Toast.makeText(context, R.string.token_pasted, Toast.LENGTH_SHORT).show();
        notifyChanged(onChanged);
        return true;
    }

    public static void applyAddressToField(EditText field, String displayAddress) {
        if (field == null || TextUtils.isEmpty(displayAddress)) {
            return;
        }
        field.setText(displayAddress);
        showAddressStart(field);
    }

    public static String normalizePastedText(String raw) {
        if (TextUtils.isEmpty(raw)) {
            return null;
        }
        String trimmed = raw.trim();
        String parsed = ChainAddressUtil.parseAddressFromQr(trimmed);
        if (!TextUtils.isEmpty(parsed)) {
            return ChainAddressUtil.displayAddress(parsed);
        }
        String compact = trimmed.replaceAll("\\s+", "");
        parsed = ChainAddressUtil.parseAddressFromQr(compact);
        if (!TextUtils.isEmpty(parsed)) {
            return ChainAddressUtil.displayAddress(parsed);
        }
        String display = ChainAddressUtil.displayAddress(compact);
        return ChainAddressUtil.isValidAddress(display) ? display : null;
    }

    private static void configureAddressField(EditText field) {
        field.setSingleLine(true);
        field.setHorizontallyScrolling(true);
        field.setScrollBarStyle(EditText.SCROLLBARS_INSIDE_INSET);
    }

    private static void applyNormalizedPaste(
            EditText field,
            String inserted,
            OnAddressChanged onChanged,
            InternalFlag flag) {
        String normalized = normalizePastedText(inserted);
        if (!TextUtils.isEmpty(normalized)) {
            flag.set(true);
            applyAddressToField(field, normalized);
            flag.set(false);
            notifyChanged(onChanged);
            return;
        }
        String whole = field.getText() != null ? field.getText().toString() : "";
        normalized = normalizePastedText(whole);
        if (!TextUtils.isEmpty(normalized)) {
            flag.set(true);
            applyAddressToField(field, normalized);
            flag.set(false);
            notifyChanged(onChanged);
        }
    }

    private static void normalizeFieldInPlace(EditText field, OnAddressChanged onChanged) {
        String raw = field.getText() != null ? field.getText().toString().trim() : "";
        if (TextUtils.isEmpty(raw)) {
            return;
        }
        String normalized = normalizePastedText(raw);
        if (TextUtils.isEmpty(normalized)) {
            String display = ChainAddressUtil.displayAddress(raw.replaceAll("\\s+", ""));
            if (ChainAddressUtil.isValidAddress(display)) {
                normalized = display;
            }
        }
        if (!TextUtils.isEmpty(normalized) && !TextUtils.equals(raw, normalized)) {
            applyAddressToField(field, normalized);
            notifyChanged(onChanged);
        }
    }

    private static void showAddressStart(EditText field) {
        field.post(() -> {
            field.setSelection(0);
            field.scrollTo(0, 0);
            field.requestFocus();
        });
    }

    private static void notifyChanged(OnAddressChanged onChanged) {
        if (onChanged != null) {
            onChanged.onAddressChanged();
        }
    }

    private interface InternalFlag {
        void set(boolean value);
    }
}
