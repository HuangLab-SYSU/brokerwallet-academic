package com.example.brokerfi.xc;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.brokerfi.R;
import com.example.brokerfi.xc.menu.NavigationHelper;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

public class SendActivity extends AppCompatActivity {

    private ImageView menu;
    private ImageView notificationBtn;
    private RelativeLayout action_bar;
    private EditText edt_sendfrom;
    private EditText edt_sendto;
    private EditText edt_amount;
    private EditText edt_fee;
    private NavigationHelper navigationHelper;
    private Button button;


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
        edt_fee= findViewById(R.id.edt_amount2);

        button=findViewById(R.id.btn_send);
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
                String fee = edt_fee.getText().toString();
                
                // IsInput？
                if (toAddress.isEmpty() || amount.isEmpty() || fee.isEmpty()) {
                    Toast.makeText(SendActivity.this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                // Toast Confirm
                showConfirmDialog(fromAddress, toAddress, amount, fee);
            }
        });

    }

    private volatile boolean tx = false;
    private AlertDialog confirmDialog;
    
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
        
        tvFrom.setText("From: " + fromAddress);
        tvTo.setText("To: " + toAddress);
        tvAmount.setText("Amount: " + amount + " BKC");
        tvFee.setText("Gas Fee: " + fee + " BKC");
        tvTotal.setText("Total: " + totalAmount + " BKC");
        
        builder.setView(dialogView);
        
        builder.setNegativeButton("Cancel", (dialog, which) -> {
            dialog.dismiss();
        });
        
        builder.setPositiveButton("Confirm", (dialog, which) -> {
            dialog.dismiss();
            sendtx2network();
        });
        
        confirmDialog = builder.create();
        confirmDialog.getWindow().setBackgroundDrawableResource(R.color.black);
        confirmDialog.show();
    }
    
    private void sendtx2network(){
        if(tx){
            Toast.makeText(SendActivity.this,"Do not resubmit the transaction!",Toast.LENGTH_LONG).show();
            return;
        }
        tx = true;
        String sendTo = edt_sendto.getText().toString();
        
        // Verify the format of the destination address
        if (!SecurityUtil.isAddressFormatValid(sendTo)) {
            Toast.makeText(SendActivity.this,"Invalid address format!",Toast.LENGTH_LONG).show();
            tx = false;
            return;
        }
        
        // Remove 0x or 0X prefix before sending 
        String formattedSendTo = SecurityUtil.removeAddressPrefix(sendTo);
        
        String amount = edt_amount.getText().toString();
        String fee = edt_fee.getText().toString();

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
                    Toast.makeText(SendActivity.this,"Submit transaction successfully! Please wait for the result.",Toast.LENGTH_LONG).show();
                });
                try {
                    String s = MyUtil.SendTX(privatekey,formattedSendTo,amount,fee);
                    if(s!=null &&s.contains("success")){
                        runOnUiThread(()->{
                            Toast.makeText(SendActivity.this,"Send successfully",Toast.LENGTH_LONG).show();
                        });

                    }else{
                        runOnUiThread(()->{
                            Toast.makeText(SendActivity.this,"Send failed："+ s,Toast.LENGTH_LONG).show();
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