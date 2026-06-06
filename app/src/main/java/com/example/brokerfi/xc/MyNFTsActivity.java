package com.example.brokerfi.xc;


import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.brokerfi.R;
import com.example.brokerfi.xc.adapter.NFTAdapter;
import com.example.brokerfi.xc.menu.NavigationHelper;
import com.example.brokerfi.xc.model.NFT;
import com.example.brokerfi.xc.net.ABIUtils;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

public class MyNFTsActivity extends AppCompatActivity{

    private ImageView menu;
    private ImageView notificationBtn;
    private RelativeLayout action_bar;
    private NavigationHelper navigationHelper;
    private RecyclerView recyclerView;
    private NFTAdapter adapter;
    private ImageView btn_list;
    private ImageView btn_unlist;
    List<NFT> NFTData = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_nfts);

        intView();
        intEvent();
        fetchMyNFTs();
        findViewById(R.id.dashedBorderView).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setClass(MyNFTsActivity.this, MainActivity.class);
                startActivity(intent);
            }
        });
    }

    private void intView() {
        menu = findViewById(R.id.menu);
        notificationBtn = findViewById(R.id.notificationBtn);
        action_bar = findViewById(R.id.action_bar);
        btn_list = findViewById(R.id.btn_list);
        btn_unlist = findViewById(R.id.btn_unlist);
        recyclerView = findViewById(R.id.rv_nft_list);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new NFTAdapter(NFTData,false);
        recyclerView.setAdapter(adapter);
    }
    
    private void intEvent(){
        navigationHelper = new NavigationHelper(menu, action_bar,this,notificationBtn);

        btn_list.setOnClickListener(view -> {
            NFT selected = adapter.getSelectedItem();
            int position = adapter.getSelectedPosition();

            if (selected == null) {
                Toast.makeText(this, R.string.my_nfts_toast_select_nft_to_list_first, Toast.LENGTH_SHORT).show();
                return;
            }

            if (selected.isListed()) {
                Toast.makeText(this, R.string.my_nfts_toast_nft_already_listed, Toast.LENGTH_SHORT).show();
                adapter.clearSelection();
                return;
            }

            AlertDialog dialog = new AlertDialog.Builder(this)
                    .setView(R.layout.dialog_confirm)
                    .create();

            dialog.setOnShowListener(dialogInterface -> {

                TextView tv_title = dialog.findViewById(R.id.tv_title);
                EditText et_shares = dialog.findViewById(R.id.et_shares);
                EditText et_price = dialog.findViewById(R.id.et_price);
                Button btnConfirm = dialog.findViewById(R.id.btn_confirm);
                Button btnCancel = dialog.findViewById(R.id.btn_cancel);

                tv_title.setText(R.string.my_nfts_list);

                btnConfirm.setOnClickListener(v -> {
                    String shares = et_shares.getText().toString().trim();
                    String price = et_price.getText().toString().trim();

                    if (shares.isEmpty()) {
                        et_shares.setError(getString(R.string.mint_error_required_field));
                        et_shares.requestFocus();
                        Toast.makeText(MyNFTsActivity.this, R.string.my_nfts_toast_enter_nft_shares_to_list, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (price.isEmpty()) {
                        et_price.setError(getString(R.string.mint_error_required_field));
                        et_price.requestFocus();
                        Toast.makeText(MyNFTsActivity.this, R.string.my_nfts_toast_enter_price_per_share, Toast.LENGTH_SHORT).show();
                        return;
                    }

                    BigInteger shareValue = new BigInteger(shares);
                    BigInteger priceValue = new BigInteger(price);
                    try {
                        if (shareValue.compareTo(BigInteger.ONE) < 0) {
                            et_shares.setError(getString(R.string.buy_nfts_error_1));
                            Toast.makeText(MyNFTsActivity.this, R.string.buy_nfts_toast_1, Toast.LENGTH_SHORT).show();
                            return;
                        }

                        if (shareValue.compareTo(selected.getShares()) > 0) {
                            et_shares.setError(getString(R.string.my_nfts_toast_exceeds_owned_nft_shares));
                            Toast.makeText(MyNFTsActivity.this, R.string.my_nfts_toast_exceeds_owned_nft_shares, Toast.LENGTH_SHORT).show();
                            return;
                        }

                        if (priceValue.compareTo(BigInteger.ZERO) <= 0) {
                            et_price.setError(getString(R.string.my_nfts_error_minimum_0_01));
                            Toast.makeText(MyNFTsActivity.this, R.string.my_nfts_toast_price_gt_zero, Toast.LENGTH_SHORT).show();
                            return;
                        }
                    } catch (NumberFormatException e) {
                        et_shares.setError(getString(R.string.my_nfts_error_format_example_10));
                        et_price.setError(getString(R.string.my_nfts_error_format_example_0_05));
                        Toast.makeText(MyNFTsActivity.this, R.string.my_nfts_toast_enter_valid_number, Toast.LENGTH_SHORT).show();
                        return;
                    }



                    String data = ABIUtils.encodeList(selected.getId(),shareValue,priceValue);
                    listNFT(data);
                    adapter.clearSelection();
                    dialog.dismiss();
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


        btn_unlist.setOnClickListener(view -> {
            NFT selected = adapter.getSelectedItem();
            int position = adapter.getSelectedPosition();

            if (selected == null) {
                Toast.makeText(this, R.string.my_nfts_toast_select_unlist_nft, Toast.LENGTH_SHORT).show();
                return;
            }

            if (!selected.isListed()) {
                Toast.makeText(this, R.string.my_nfts_toast_nft_not_listed, Toast.LENGTH_SHORT).show();
                adapter.clearSelection();
                return;
            }

            AlertDialog dialog = new AlertDialog.Builder(this)
                    .setView(R.layout.dialog_confirm_unlist)
                    .create();

            dialog.setOnShowListener(dialogInterface -> {

                Button btnConfirm = dialog.findViewById(R.id.btn_confirm);
                Button btnCancel = dialog.findViewById(R.id.btn_cancel);

                btnConfirm.setOnClickListener(v -> {

                    String data = ABIUtils.encodeUnlist(selected.getListingId());
                    unlistNFT(data);
                    Toast.makeText(this, R.string.my_nfts_toast_unlist_successful, Toast.LENGTH_SHORT).show();
                    adapter.clearSelection();
                    dialog.dismiss();
                });

                btnCancel.setOnClickListener(v -> {
                    Toast.makeText(this, R.string.my_nfts_toast_unlist_cancelled, Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                });
            });

            dialog.show();
            dialog.getWindow().setDimAmount(0.5f);
        });

    }

    private void fetchMyNFTs() {

        try {
            String result = MyUtil.sendethcall(ABIUtils.encodeGetMyNFTs(), StorageUtil.getCurrentPrivatekey(this));
            if(result == null){
                runOnUiThread(() ->
                        Toast.makeText(MyNFTsActivity.this,
                                R.string.my_nfts_toast_fetch_empty_response,
                                Toast.LENGTH_LONG).show()
                );
                return;
            }
            try {
                if (result.trim().startsWith("{")) {
                    JSONObject response = new JSONObject(result);
                    if (response.has("error")) {
                        Toast.makeText(MyNFTsActivity.this,
                                MyNFTsActivity.this.getString(R.string.buy_nfts_toast_call) + " " + response.getString("error"),
                                Toast.LENGTH_LONG).show();
                    } else {
                        String hexData = response.getString("result");
                        ABIUtils.MyNFTsResult nfts = ABIUtils.decodeGetMyNFTs(hexData);

                        List<NFT> nftList = new ArrayList<>();
                        for (int i = 0; i < nfts.nftIds.length; i++) {
                            NFT nft = new NFT(
                                    nfts.nftIds[i],
                                    SecurityUtil.GetAddress(StorageUtil.getCurrentPrivatekey(this)),
                                    nfts.images[i],
                                    nfts.names[i],
                                    nfts.sharesList[i],
                                    nfts.pricesList[i],
                                    nfts.listingStatus[i],
                                    nfts.listingIds[i]
                            );
                            nftList.add(nft);
                        }

                        runOnUiThread(() -> {
                            if (nftList.isEmpty()) {
                                Toast.makeText(MyNFTsActivity.this, R.string.buy_nfts_toast_no_nft_data, Toast.LENGTH_LONG).show();
                            }
                            NFTData.clear();
                            NFTData.addAll(nftList);
                            adapter.notifyDataSetChanged();
                        });
                    }
                } else {
                    runOnUiThread(() ->
                            Toast.makeText(MyNFTsActivity.this,
                                    MyNFTsActivity.this.getString(R.string.buy_nfts_toast_7) + " " + result,
                                    Toast.LENGTH_LONG).show()
                    );
                }
            }catch (JSONException e){
                e.printStackTrace();
                Log.e("NFT_FETCH", "响应格式错误", e);
                runOnUiThread(() -> Toast.makeText(MyNFTsActivity.this, MyNFTsActivity.this.getString(R.string.buy_nfts_toast_8) + " " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        } catch (Exception e) {
            runOnUiThread(() -> Toast.makeText(MyNFTsActivity.this, MyNFTsActivity.this.getString(R.string.buy_nfts_toast_9) + " " + e.getMessage(), Toast.LENGTH_LONG).show());
        }
    }

    private void listNFT(String data) {
        try {
            String result = MyUtil.sendethtx(data, StorageUtil.getCurrentPrivatekey(this));
            if(result == null){
                runOnUiThread(() -> Toast.makeText(MyNFTsActivity.this, R.string.my_nfts_toast_list_empty_response, Toast.LENGTH_LONG).show());
                return;
            }
            if (result.trim().startsWith("{")) {
                JSONObject response = new JSONObject(result);
                if (response.has("error")) {
                    Toast.makeText(MyNFTsActivity.this,
                            MyNFTsActivity.this.getString(R.string.my_nfts_toast_list_failed_prefix) + " " + response.getString("error"),
                            Toast.LENGTH_LONG).show();
                    // 刷新NFT数据
                    fetchMyNFTs();
                } else {
                    String txHash = response.getString("result");
                    checkTransactionStatus(txHash);
                }
            } else {
                // 处理非JSON响应（如404）
                runOnUiThread(() -> Toast.makeText(MyNFTsActivity.this,
                        MyNFTsActivity.this.getString(R.string.buy_nfts_toast_7) + " " + result,
                        Toast.LENGTH_LONG).show());
            }
            return;

        } catch (Exception e) {
            runOnUiThread(() -> Toast.makeText(MyNFTsActivity.this, R.string.my_nfts_toast_list_build_error, Toast.LENGTH_LONG).show());
        }
    }
    private void checkTransactionStatus(String txHash) {
        if(txHash.startsWith("0x")){
            txHash = txHash.substring(2);
        }
        try {


            String result = MyUtil.getTransactionReceipt(txHash, StorageUtil.getCurrentPrivatekey(this));
            if(result == null){
                runOnUiThread(() ->
                        Toast.makeText(this, R.string.buy_nfts_toast_4, Toast.LENGTH_SHORT).show()
                );
                return;
            }
            JSONObject response = new JSONObject(result);
            JSONObject receipt = response.optJSONObject("result");

            if (receipt != null) {
                String status = receipt.optString("status", "0x0");
                if ("0x1".equals(status)) {
                    runOnUiThread(() -> {
                        Toast.makeText(MyNFTsActivity.this, R.string.my_nfts_toast_list_successful, Toast.LENGTH_SHORT).show();
                        fetchMyNFTs();
                    });
                } else {
                    runOnUiThread(() ->  {
                        Toast.makeText(MyNFTsActivity.this, R.string.my_nfts_toast_list_failed, Toast.LENGTH_SHORT).show();
                        // 刷新NFT数据
                        fetchMyNFTs();
                    });
                }
            } else {
                // 交易尚未上链，继续轮询
                String finalTxHash = txHash;
                new Handler().postDelayed(() -> checkTransactionStatus(finalTxHash), 2000);
            }

        } catch (JSONException e) {
            e.printStackTrace();
            Toast.makeText(this, R.string.buy_nfts_toast_15, Toast.LENGTH_SHORT).show();
        }
    }


    // 下架NFT的后端接口调用
    private void unlistNFT(String data) {
        try {
            String result = MyUtil.sendethtx(data, StorageUtil.getCurrentPrivatekey(this));
            if(result == null){
                runOnUiThread(() -> Toast.makeText(MyNFTsActivity.this, MyNFTsActivity.this.getString(R.string.my_nfts_toast_unlist_empty_response) + " ", Toast.LENGTH_LONG).show());
                return;
            }
            try {
                if (result.trim().startsWith("{")) {
                    JSONObject response = new JSONObject(result);
                    if (response.has("error")) {
                        Toast.makeText(MyNFTsActivity.this,
                                MyNFTsActivity.this.getString(R.string.my_nfts_toast_unlist_failed_prefix) + " " + response.getString("error"),
                                Toast.LENGTH_LONG).show();
                        fetchMyNFTs();
                    } else {
                        String txHash = response.getString("result");
                        checkUnlistTransactionStatus(txHash);
                    }
                } else {
                    runOnUiThread(() -> Toast.makeText(MyNFTsActivity.this,
                            MyNFTsActivity.this.getString(R.string.buy_nfts_toast_7) + " " + result,
                            Toast.LENGTH_LONG).show());
                }
            } catch (JSONException e) {
                runOnUiThread(() -> Toast.makeText(MyNFTsActivity.this, R.string.my_nfts_toast_unlist_parse_error, Toast.LENGTH_LONG).show());
            }

        } catch (Exception e) {
            runOnUiThread(() -> Toast.makeText(MyNFTsActivity.this, R.string.my_nfts_toast_unlist_build_error, Toast.LENGTH_LONG).show());
        }
    }

    private void checkUnlistTransactionStatus(String txHash) {

        if(txHash.startsWith("0x")){
            txHash = txHash.substring(2);
        }
        try {
            String result = MyUtil.getTransactionReceipt(txHash, StorageUtil.getCurrentPrivatekey(this));
            if(result == null){
                runOnUiThread(() ->
                        Toast.makeText(this, R.string.buy_nfts_toast_4, Toast.LENGTH_SHORT).show()
                );
                return;
            }
            JSONObject response = new JSONObject(result);
            JSONObject receipt = response.optJSONObject("result");

            if (receipt != null) {
                String status = receipt.optString("status", "0x0");
                if ("0x1".equals(status)) {
                    runOnUiThread(() -> {
                        Toast.makeText(MyNFTsActivity.this, R.string.my_nfts_toast_unlist_successful, Toast.LENGTH_SHORT).show();
                        fetchMyNFTs();
                    });
                } else {
                    runOnUiThread(() ->  {
                        Toast.makeText(MyNFTsActivity.this, R.string.my_nfts_toast_unlist_failed, Toast.LENGTH_SHORT).show();
                        fetchMyNFTs();
                    });
                }
            } else {
                String finalTxHash = txHash;
                new Handler().postDelayed(() -> checkUnlistTransactionStatus(finalTxHash), 2000);
            }

        } catch (JSONException e) {
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
