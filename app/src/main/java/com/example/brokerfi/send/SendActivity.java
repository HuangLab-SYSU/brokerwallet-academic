package com.example.brokerfi.xc;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.brokerfi.R;
import com.example.brokerfi.main.menu.NavigationHelper;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.example.brokerfi.core.security.SecurityUtil;
import com.example.brokerfi.core.storage.StorageUtil;
import com.example.brokerfi.core.util.MyUtil;
import com.example.brokerfi.main.MainActivity;


public class SendActivity extends AppCompatActivity {

    private ImageView menu;
    private ImageView notificationBtn;
    private RelativeLayout action_bar;
    private EditText edt_sendfrom;
    private EditText edt_sendto;
    private EditText edt_amount;
    private SeekBar seekBarFee;
    private EditText edtFeeValue;
    private NavigationHelper navigationHelper;
    private Button button;

    // Gas price presets: Slow(20), Medium(50), Fast(100) in Gwei
    private static final int[] FEE_PRESETS = {20, 50, 100};
    private String currentFee = "50"; // default to Medium


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send);

        intView();
        intEvent();
        findViewById(R.id.dashedBorderView).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setClass(SendActivity.this, MainActivity.class);
                startActivity(intent);
            }
        });

    }


    private void intView() {
        menu = findViewById(R.id.menu);
        notificationBtn = findViewById(R.id.notificationBtn);
        action_bar = findViewById(R.id.action_bar);
        edt_sendfrom = findViewById(R.id.edt_sendfrom);



        String account = StorageUtil.getPrivateKey(this);
        String acc = StorageUtil.getCurrentAccount(this);
        int i;
        if (acc == null){
            i=0;
        }else {
            i = Integer.parseInt(acc);
        }
        if (account != null) {
            String[] split = account.split(";");
            String privatekey = split[i];
            String fromaddr = SecurityUtil.GetAddress(privatekey);
            edt_sendfrom.setText(fromaddr);
        }



        edt_sendfrom.setEnabled(false);
        edt_sendto = findViewById(R.id.edt_sendto);

        edt_amount=findViewById(R.id.edt_amount);
        edtFeeValue = findViewById(R.id.edt_fee_value);
        seekBarFee = findViewById(R.id.seekbar_fee);

        // Setup gas price slider
        setupFeeSlider();

        button=findViewById(R.id.btn_send);
    }

    private boolean updatingFee = false; // prevent infinite EditText <-> SeekBar loop

    private void setupFeeSlider() {
        edtFeeValue.setText(currentFee);

        // SeekBar -> EditText
        seekBarFee.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (updatingFee) return;
                if (fromUser) {
                    currentFee = String.valueOf(progress);
                    updatingFee = true;
                    edtFeeValue.setText(currentFee);
                    updatingFee = false;
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // EditText -> SeekBar
        edtFeeValue.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(android.text.Editable s) {
                if (updatingFee) return;
                String val = s.toString().trim();
                if (val.isEmpty()) return;
                try {
                    double d = Double.parseDouble(val);
                    currentFee = val;
                    int progress = (int) Math.round(d);
                    if (progress < 0) progress = 0;
                    if (progress > seekBarFee.getMax()) progress = seekBarFee.getMax();
                    updatingFee = true;
                    seekBarFee.setProgress(progress);
                    updatingFee = false;
                } catch (NumberFormatException ignored) {}
            }
        });

        // Preset label clicks snap slider + edittext
        findViewById(R.id.label_slow).setOnClickListener(v -> snapToPreset(0));
        findViewById(R.id.label_medium).setOnClickListener(v -> snapToPreset(1));
        findViewById(R.id.label_fast).setOnClickListener(v -> snapToPreset(2));
    }

    private void snapToPreset(int index) {
        int value = FEE_PRESETS[index];
        currentFee = String.valueOf(value);
        updatingFee = true;
        seekBarFee.setProgress(value);
        edtFeeValue.setText(currentFee);
        updatingFee = false;
    }

    private void intEvent(){
        navigationHelper = new NavigationHelper(menu, action_bar,this,notificationBtn);

        String scannedData = getIntent().getStringExtra("scannedData");
        if(scannedData != null){
            edt_sendto.setText(scannedData);
        }
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // SendInfo
                String fromAddress = edt_sendfrom.getText().toString();
                String toAddress = edt_sendto.getText().toString();
                String amount = edt_amount.getText().toString();
                String fee = currentFee;
                
                // IsInput？
                if (toAddress.isEmpty() || amount.isEmpty() || fee.isEmpty()) {
                    Toast.makeText(SendActivity.this, R.string.send_toast_please_fill_in_all_fields, Toast.LENGTH_SHORT).show();
                    return;
                }
                
                // Toast Confirm
                showConfirmDialog(fromAddress, toAddress, amount, fee);
            }
        });

    }

    private volatile boolean tx = false;
    private AlertDialog confirmDialog;
    // Dialog view references kept as fields so the post-send UI can be updated in place.
    private Button btnConfirm;
    private Button btnCancel;
    private LinearLayout hashRow;
    private TextView tvHash;
    private Button btnCopyHash;

    private void showConfirmDialog(String fromAddress, String toAddress, String amount, String fee) {
        //SUM
        double amountValue = Double.parseDouble(amount);
        double feeValue = Double.parseDouble(fee);
        double totalValue = amountValue + feeValue;
        String totalAmount = String.format("%.6f", totalValue);

        // Creat Dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        //Dialog Layout

        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_confirm_transaction, null);

        TextView tvFrom = dialogView.findViewById(R.id.tv_from);
        TextView tvTo = dialogView.findViewById(R.id.tv_to);
        TextView tvAmount = dialogView.findViewById(R.id.tv_amount);
        TextView tvFee = dialogView.findViewById(R.id.tv_fee);
        TextView tvTotal = dialogView.findViewById(R.id.tv_total);
        
        tvFrom.setText(tvFrom.getContext().getString(R.string.dialog_confirm_transaction_dialog_from) + " " + fromAddress);
        tvTo.setText(tvTo.getContext().getString(R.string.dialog_confirm_transaction_dialog_to) + " " + toAddress);
        tvAmount.setText(tvAmount.getContext().getString(R.string.dialog_confirm_transaction_amount) + " " + amount + " " + tvAmount.getContext().getString(R.string.after_broker_bkc));
        tvFee.setText(tvFee.getContext().getString(R.string.dialog_confirm_transaction_gas_fee) + " " + fee + " " + tvFee.getContext().getString(R.string.after_broker_bkc));
        tvTotal.setText(tvTotal.getContext().getString(R.string.dialog_confirm_transaction_dialog_total) + " " + totalAmount + " " + tvTotal.getContext().getString(R.string.after_broker_bkc));
        

        tvFrom.setText("From: " + fromAddress);
        tvTo.setText("To: " + toAddress);
        tvAmount.setText("Amount: " + amount + " BKC");
        tvFee.setText("Gas Fee: " + fee + " BKC");
        tvTotal.setText("Total: " + totalAmount + " BKC");

        // Hash row references (hidden until the tx is broadcast)
        hashRow = dialogView.findViewById(R.id.hash_row);
        tvHash = dialogView.findViewById(R.id.tv_hash);
        btnCopyHash = dialogView.findViewById(R.id.btn_copy_hash);
        hashRow.setVisibility(View.GONE);

        // Copy the displayed hash to the system clipboard.
        btnCopyHash.setOnClickListener(v -> {
            String label = "Hash: ";
            String content = tvHash.getText().toString();
            String hash = content.startsWith(label) ? content.substring(label.length()) : content;
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            if (clipboard != null && !hash.isEmpty()) {
                clipboard.setPrimaryClip(ClipData.newPlainText("Transaction Hash", hash));
                Toast.makeText(SendActivity.this, "Hash copied", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setView(dialogView);

        btnCancel = dialogView.findViewById(R.id.btn_cancel);
        btnConfirm = dialogView.findViewById(R.id.btn_confirm);

        btnCancel.setOnClickListener(v -> {
            confirmDialog.dismiss();
        });

        // Keep the dialog open after confirm so the tx hash can be shown in place.
        btnConfirm.setOnClickListener(v -> {
            btnConfirm.setText("Sending...");
            btnConfirm.setEnabled(false);
            btnCancel.setEnabled(false);
            sendtx2network();
        });

        confirmDialog = builder.create();
        confirmDialog.getWindow().setBackgroundDrawableResource(R.color.black);
        confirmDialog.show();
    }

    private void sendtx2network(){
        if(tx){
            Toast.makeText(SendActivity.this,R.string.send_toast_do_not_resubmit,Toast.LENGTH_LONG).show();
            return;
        }
        tx = true;
        String sendTo = edt_sendto.getText().toString();

        // Verify the format of the destination address
        if (!SecurityUtil.isAddressFormatValid(sendTo)) {
            Toast.makeText(SendActivity.this,R.string.send_toast_invalid_address_format,Toast.LENGTH_LONG).show();
            tx = false;
            return;
        }

        // Remove 0x or 0X prefix before sending
        String formattedSendTo = SecurityUtil.removeAddressPrefix(sendTo);

        String amount = edt_amount.getText().toString();
        String fee = currentFee;

        String account = StorageUtil.getPrivateKey(this);
        String acc = StorageUtil.getCurrentAccount(this);
        int i;
        if (acc == null){
            i=0;
        }else {
            i = Integer.parseInt(acc);
        }
        if (account != null) {
            String[] split = account.split(";");
            String privatekey = split[i];
            new Thread(()->{
                runOnUiThread(()->{
                    Toast.makeText(SendActivity.this,R.string.send_toast_submit_success,Toast.LENGTH_LONG).show();
                });
                try {
                    String s = MyUtil.SendTX(privatekey,formattedSendTo,amount,fee);
                    if(s!=null && s.startsWith("success:")){
                        // Extract the tx hash returned by eth_sendRawTransaction.
                        final String hash = s.substring("success:".length());
                        runOnUiThread(()->{
                            tvHash.setText("Hash: " + hash);
                            hashRow.setVisibility(View.VISIBLE);
                            btnConfirm.setText("Done");
                            btnConfirm.setEnabled(true);
                            // Re-purpose the confirm button to close the dialog.
                            btnConfirm.setOnClickListener(v -> confirmDialog.dismiss());
                            Toast.makeText(SendActivity.this,"Send successfully",Toast.LENGTH_LONG).show();
                        });

                    }else{
                        runOnUiThread(()->{
                            Toast.makeText(SendActivity.this,"Send failed："+ s,Toast.LENGTH_LONG).show();
                            if (confirmDialog != null && confirmDialog.isShowing()) {
                                confirmDialog.dismiss();
                            }
                        });

                    }
                }finally {
                    tx=false;
                }
            }).start();

        }

    }
    
    @Override
    public void onBackPressed() {
      
        if (confirmDialog != null && confirmDialog.isShowing()) {
            confirmDialog.dismiss();
        } 
        
        else if (navigationHelper != null && navigationHelper.isPopupVisible()) {
            navigationHelper.hidePopup();
        } 
        
        else {
            super.onBackPressed();
            overridePendingTransition(R.anim.slide_in_from_left, R.anim.slide_out_to_right);
        }
    }
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        IntentResult intentResult = IntentIntegrator.parseActivityResult(
                requestCode,resultCode,data
        );
        if (intentResult.getContents() != null){
            String scannedData = intentResult.getContents();
            Intent intent = new Intent(this,SendActivity.class);
            intent.putExtra("scannedData",scannedData);
            startActivity(intent);

        }
    }

}
