package com.example.brokerfi.nft;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.brokerfi.R;
import com.example.brokerfi.nft.adapter.NFTAdapter;
import com.example.brokerfi.main.menu.NavigationHelper;
import com.example.brokerfi.nft.model.NFT;
import com.example.brokerfi.core.network.ABIUtils;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import com.example.brokerfi.core.storage.StorageUtil;
import com.example.brokerfi.core.util.MyUtil;
import com.example.brokerfi.main.MainActivity;
import com.example.brokerfi.send.SendActivity;


public class BuyNFTsActivity extends AppCompatActivity{

    private ImageView menu;
    private ImageView notificationBtn;
    private RelativeLayout action_bar;
    private NavigationHelper navigationHelper;
    private RecyclerView recyclerView;
    private NFTAdapter adapter;
    private ImageView btn_buy_nfts;

    List<NFT> NFTData = new ArrayList<>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_buy_nfts);

        intView();
        intEvent();

        fetchListedNFTs();
        findViewById(R.id.dashedBorderView).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setClass(BuyNFTsActivity.this, MainActivity.class);
                startActivity(intent);
            }
        });
    }
    private void intView() {
        menu = findViewById(R.id.menu);
        notificationBtn = findViewById(R.id.notificationBtn);
        action_bar = findViewById(R.id.action_bar);
        btn_buy_nfts = findViewById(R.id.btn_buy_nfts);
        recyclerView = findViewById(R.id.rv_nft_list);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new NFTAdapter(NFTData,true);
        recyclerView.setAdapter(adapter);
    }
    private void intEvent(){
        navigationHelper = new NavigationHelper(menu, action_bar,this,notificationBtn);

        // buy button
        btn_buy_nfts.setOnClickListener(view -> {
            NFT selected = adapter.getSelectedItem();
            int position = adapter.getSelectedPosition();

            if (adapter.getSelectedItem() == null) {
                Toast.makeText(this, R.string.buy_nfts_toast_select_nft_to_buy_first, Toast.LENGTH_SHORT).show();
                return;
            }

            // Execute purchase logic
            // Create pop-up window
            AlertDialog dialog = new AlertDialog.Builder(this)
                    .setView(R.layout.dialog_confirm)
                    .create();

            dialog.setOnShowListener(dialogInterface -> {
                TextView tv_title = dialog.findViewById(R.id.tv_title);
                TextView tvPriceLabel = dialog.findViewById((R.id.tv_price_label));
                EditText et_shares = dialog.findViewById(R.id.et_shares);
                EditText et_price = dialog.findViewById(R.id.et_price);
                Button btnConfirm = dialog.findViewById(R.id.btn_confirm);
                Button btnCancel = dialog.findViewById(R.id.btn_cancel);

                // Set reminder information
                tv_title.setText(R.string.Buy);
                // Modify label text
                tvPriceLabel.setText(R.string.buy_nfts_total_gas_fee_included);

                // Show unit price and disable
                et_price.setText(getString(R.string.buy_nfts_price_bkc, selected.getPrice().add(new BigInteger("20")).toString()));
                et_price.setEnabled(false);
                et_price.setTextColor(ContextCompat.getColor(this, R.color.grey_price_disable));
                et_price.setBackgroundResource(R.drawable.custom_edittext_border_disabled); // Use a custom background

                // Set up input monitoring and automatically generate total price based on the number of copies.
                et_shares.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void afterTextChanged(Editable shares) {
                        String input = shares.toString().trim();

                        // Empty input handling
                        if (input.isEmpty()) {
                            et_price.setText(R.string.buy_nfts_price_placeholder);
                            btnConfirm.setEnabled(false);
                            return;
                        }

                        try {
                            BigInteger s = new BigInteger(input);

                            // Determine the shares input format
                            if (s.compareTo(BigInteger.ONE) < 0) {
                                et_shares.setError(getString(R.string.buy_nfts_error_1));
                                et_price.setText(R.string.buy_nfts_price_placeholder);
                                btnConfirm.setEnabled(false);
                                Toast.makeText(BuyNFTsActivity.this, R.string.buy_nfts_toast_1, Toast.LENGTH_SHORT).show();
                                return;
                            }

                            if (s.compareTo(selected.getShares()) > 0) {
                                et_shares.setError(getString(R.string.buy_nfts_toast_exceeds_shares));
                                et_price.setText(R.string.buy_nfts_price_placeholder);
                                btnConfirm.setEnabled(false);
                                Toast.makeText(BuyNFTsActivity.this, R.string.buy_nfts_toast_exceeds_shares, Toast.LENGTH_SHORT).show();
                                return;
                            }
                            BigInteger total = selected.getPrice().multiply(s)
                                    .add(new BigInteger("20"));
                            String displayText = getString(R.string.buy_nfts_price_bkc, total.toString());
                            et_price.setText(displayText);
                            btnConfirm.setEnabled(true);

                        } catch (NumberFormatException e) {
                            et_price.setText(R.string.buy_nfts_price_placeholder);
                            btnConfirm.setEnabled(false);
                        }
                    }

                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {}
                });
                btnConfirm.setOnClickListener(v -> {
                    String s = et_shares.getText().toString();
                    if (s.isEmpty()) {
                        Toast.makeText(this, R.string.buy_nfts_toast_2, Toast.LENGTH_SHORT).show();
                        return;
                    }

                    try {
                        BigInteger shareValue = new BigInteger(s);
                        BigInteger totalPrice = selected.getPrice().multiply(shareValue)
                                .add(new BigInteger("20"));
                        String data = ABIUtils.encodeBuy(selected.getListingId(),shareValue);
                        BuyNFT(data,totalPrice);

                        Toast.makeText(this, getString(R.string.buy_nfts_toast_3) + totalPrice + " " + getString(R.string.after_broker_bkc), Toast.LENGTH_SHORT).show();
                        adapter.clearSelection();
                        dialog.dismiss();

                    } catch (NumberFormatException e) {
                        Toast.makeText(this, R.string.buy_nfts_toast_4, Toast.LENGTH_SHORT).show();
                        adapter.clearSelection();
                        dialog.dismiss();
                        return;
                    }
                });

                btnCancel.setOnClickListener(v -> {
                    Toast.makeText(this, R.string.buy_nfts_toast_5, Toast.LENGTH_SHORT).show();
                    adapter.clearSelection();
                    dialog.dismiss();
                });
            });
            dialog.show();
            dialog.getWindow().setDimAmount(0.5f);
        });
    }

    private void fetchListedNFTs() {
        try {
            String result = MyUtil.sendethcall(ABIUtils.encodeGetListedNFTs(), StorageUtil.getCurrentPrivatekey(this));
            if(result == null) {
                runOnUiThread(() -> Toast.makeText(BuyNFTsActivity.this, BuyNFTsActivity.this.getString(R.string.buy_nfts_toast_6) + " " , Toast.LENGTH_LONG).show());
                return;
            }
            try {
                if (result.trim().startsWith("{")) {
                    JSONObject response = new JSONObject(result);
                    if (response.has("error")) {
                        Toast.makeText(BuyNFTsActivity.this,
                                BuyNFTsActivity.this.getString(R.string.buy_nfts_toast_call) + " " + response.getString("error"),
                                Toast.LENGTH_LONG).show();
                    } else {
                        String hexData = response.getString("result");
                        ABIUtils.ListedNFTsResult nfts = ABIUtils.decodeGetListedNFTs(hexData);

                        List<NFT> nftList = new ArrayList<>();
                        for (int i = 0; i < nfts.nftIds.length; i++) {
                            NFT nft = new NFT(
                                    nfts.nftIds[i],
                                    nfts.addressList[i],
                                    nfts.images[i], // Directly store Base64 image data
                                    nfts.names[i],
                                    nfts.sharesList[i],
                                    nfts.pricesList[i],
                                    true,
                                    nfts.listingIds[i]
                            );
                            nftList.add(nft);
                        }

                        runOnUiThread(() -> {
                            if (nftList.isEmpty()) {
                                Toast.makeText(BuyNFTsActivity.this, R.string.buy_nfts_toast_no_nft_data, Toast.LENGTH_LONG).show();
                            }
                            NFTData.clear();
                            NFTData.addAll(nftList);
                            adapter.notifyDataSetChanged();
                        });
                    }
                } else {
                    // Handle non-JSON responses (such as 404)
                    runOnUiThread(() ->
                            Toast.makeText(BuyNFTsActivity.this,
                                    BuyNFTsActivity.this.getString(R.string.buy_nfts_toast_7) + " " + result,
                                    Toast.LENGTH_LONG).show()
                    );
                }
            } catch (JSONException e) {
                e.printStackTrace();
                Log.e("NFT_FETCH", "响应格式错误", e);
                runOnUiThread(() -> Toast.makeText(BuyNFTsActivity.this, BuyNFTsActivity.this.getString(R.string.buy_nfts_toast_8) + " " + e.getMessage(), Toast.LENGTH_LONG).show());
            }

        } catch (Exception e) {
            runOnUiThread(() -> Toast.makeText(BuyNFTsActivity.this, BuyNFTsActivity.this.getString(R.string.buy_nfts_toast_9) + " " + e.getMessage(), Toast.LENGTH_LONG).show());
        }
    }

    private void BuyNFT(String data, BigInteger value) {
        String unit="1000000000000000000";
        BigInteger unitbigint = new BigInteger(unit);
        value = value.multiply(unitbigint);
        try {
            String result = MyUtil.sendethtx(data, StorageUtil.getCurrentPrivatekey(this),value.toString(16));
            if(result == null){
                Toast.makeText(BuyNFTsActivity.this,R.string.buy_nfts_toast_transaction_failed,Toast.LENGTH_LONG).show();
                return;
            }
            try {
                // First check if it is valid JSON
                if (result.trim().startsWith("{")) {
                    JSONObject response = new JSONObject(result);
                    if (response.has("error")) {
                        Toast.makeText(BuyNFTsActivity.this,
                                BuyNFTsActivity.this.getString(R.string.buy_nfts_toast_10) + " " + response.getString("error"),
                                Toast.LENGTH_LONG).show();
                        // Refresh market NFT information
                        fetchListedNFTs();
                    } else {
                        String txHash = response.getString("result");
                        checkTransactionStatus(txHash);
                    }
                } else {
                    // Handle non-JSON responses (such as 404)
                    runOnUiThread(() ->
                            Toast.makeText(BuyNFTsActivity.this,
                                    BuyNFTsActivity.this.getString(R.string.buy_nfts_toast_7) + " " + result,
                                    Toast.LENGTH_LONG).show()
                    );
                }
            } catch (JSONException e) {
                e.printStackTrace();
                // Handling invalid JSON
                runOnUiThread(() ->
                        Toast.makeText(BuyNFTsActivity.this,
                                R.string.buy_nfts_toast_11,
                                Toast.LENGTH_LONG).show()
                );
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, R.string.buy_nfts_toast_12, Toast.LENGTH_SHORT).show();
        }
    }

    private void checkTransactionStatus(String txHash) {
        try {
            if(txHash.startsWith("0x")){
                txHash = txHash.substring(2);
            }
            String result = MyUtil.getTransactionReceipt(txHash, StorageUtil.getCurrentPrivatekey(this));
            if(result == null){
                Toast.makeText(BuyNFTsActivity.this, R.string.buy_nfts_toast_checktransactionstatus, Toast.LENGTH_SHORT).show();
                return;
            }
            try {
                JSONObject response = new JSONObject(result);
                JSONObject receipt = response.optJSONObject("result");

                if (receipt != null) {
                    String status = receipt.optString("status", "0x0");
                    if ("0x1".equals(status)) {
                        runOnUiThread(() -> {
                            Toast.makeText(BuyNFTsActivity.this, R.string.buy_nfts_toast_13, Toast.LENGTH_SHORT).show();
                            // Refresh market NFT information
                            fetchListedNFTs();
                        });
                    } else {
                        runOnUiThread(() -> {
                            Toast.makeText(BuyNFTsActivity.this, R.string.buy_nfts_toast_14, Toast.LENGTH_SHORT).show();
                            // Refresh market NFT information
                            fetchListedNFTs();
                        });
                    }
                } else {
                    // The transaction has not yet been uploaded to the chain, continue polling.
                    String finalTxHash = txHash;
                    new Handler().postDelayed(() -> checkTransactionStatus(finalTxHash), 2000);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, R.string.buy_nfts_toast_15, Toast.LENGTH_SHORT).show();
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
