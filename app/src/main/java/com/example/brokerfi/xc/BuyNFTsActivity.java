package com.example.brokerfi.xc;

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
import java.util.Locale;

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

        //购买按钮
        btn_buy_nfts.setOnClickListener(view -> {
            NFT selected = adapter.getSelectedItem();
            int position = adapter.getSelectedPosition();

            if (adapter.getSelectedItem() == null) {
                Toast.makeText(this, "🛒 请先选择要购买的NFT", Toast.LENGTH_SHORT).show();
                return;
            }

            // 执行购买逻辑
            // 创建弹窗
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

                // 设置提示信息
                tv_title.setText(R.string.Buy);
                // 修改标签文本
                tvPriceLabel.setText("Total (Gas fee included)");

                // 显示单价并禁用
                et_price.setText(String.format(Locale.US, "%s BKC", selected.getPrice().add(new BigInteger("20")).toString()));
                et_price.setEnabled(false);
                et_price.setTextColor(ContextCompat.getColor(this, R.color.grey_price_disable));
                et_price.setBackgroundResource(R.drawable.custom_edittext_border_disabled); // 使用自定义背景

                // 设置输入监听，根据份数自动生成total price
                et_shares.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void afterTextChanged(Editable shares) {
                        String input = shares.toString().trim();

                        // 空输入处理
                        if (input.isEmpty()) {
                            et_price.setText("————————");
                            btnConfirm.setEnabled(false);
                            return;
                        }

                        try {
                            BigInteger s = new BigInteger(input);

                            // 判断shares输入格式
                            if (s.compareTo(BigInteger.ONE) < 0) {
                                et_shares.setError("最小为1份");
                                et_price.setText("————————");
                                btnConfirm.setEnabled(false);
                                Toast.makeText(BuyNFTsActivity.this, "份数不能小于1", Toast.LENGTH_SHORT).show();
                                return;
                            }

                            if (s.compareTo(selected.getShares()) > 0) {
                                et_shares.setError("超过可购买份数");
                                et_price.setText("————————");
                                btnConfirm.setEnabled(false);
                                Toast.makeText(BuyNFTsActivity.this, "超过可购买份数", Toast.LENGTH_SHORT).show();
                                return;
                            }
                            BigInteger total = selected.getPrice().multiply(s)
                                    .add(new BigInteger("20"));
                            String displayText = String.format(Locale.US, "%s BKC", total.toString());
                            et_price.setText(displayText);
                            btnConfirm.setEnabled(true);

                        } catch (NumberFormatException e) {
                            et_price.setText("————————");
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
                        Toast.makeText(this, "请输入份数", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    try {
                        BigInteger shareValue = new BigInteger(s);
                        BigInteger totalPrice = selected.getPrice().multiply(shareValue)
                                .add(new BigInteger("20"));
                        String data = ABIUtils.encodeBuy(selected.getListingId(),shareValue);
                        BuyNFT(data,totalPrice);

                        Toast.makeText(this, "购买成功！总价：" + totalPrice + " BKC", Toast.LENGTH_SHORT).show();
                        adapter.clearSelection();
                        dialog.dismiss();

                    } catch (NumberFormatException e) {
                        Toast.makeText(this, "交易失败！", Toast.LENGTH_SHORT).show();
                        adapter.clearSelection();
                        dialog.dismiss();
                        return;
                    }
                });

                btnCancel.setOnClickListener(v -> {
                    Toast.makeText(this, "已取消上架", Toast.LENGTH_SHORT).show();
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
                runOnUiThread(() -> Toast.makeText(BuyNFTsActivity.this, "网络请求失败:服务器响应数据为空 " , Toast.LENGTH_LONG).show());
                return;
            }
            try {
                if (result.trim().startsWith("{")) {
                    JSONObject response = new JSONObject(result);
                    if (response.has("error")) {
                        Toast.makeText(BuyNFTsActivity.this,
                                "call失败: " + response.getString("error"),
                                Toast.LENGTH_LONG).show();
                    } else {
                        String hexData = response.getString("result");
                        ABIUtils.ListedNFTsResult nfts = ABIUtils.decodeGetListedNFTs(hexData);

                        List<NFT> nftList = new ArrayList<>();
                        for (int i = 0; i < nfts.nftIds.length; i++) {
                            NFT nft = new NFT(
                                    nfts.nftIds[i],
                                    nfts.addressList[i],
                                    nfts.images[i], // 直接存储Base64图片数据
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
                                Toast.makeText(BuyNFTsActivity.this, "暂无NFT数据", Toast.LENGTH_LONG).show();
                            }
                            NFTData.clear();
                            NFTData.addAll(nftList);
                            adapter.notifyDataSetChanged();
                        });
                    }
                } else {
                    // 处理非JSON响应（如404）
                    runOnUiThread(() ->
                            Toast.makeText(BuyNFTsActivity.this,
                                    "服务器错误: " + result,
                                    Toast.LENGTH_LONG).show()
                    );
                }
            } catch (JSONException e) {
                e.printStackTrace();
                Log.e("NFT_FETCH", "响应格式错误", e);
                runOnUiThread(() -> Toast.makeText(BuyNFTsActivity.this, "数据解析失败: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }

        } catch (Exception e) {
            runOnUiThread(() -> Toast.makeText(BuyNFTsActivity.this, "请求构造失败: " + e.getMessage(), Toast.LENGTH_LONG).show());
        }
    }

    private void BuyNFT(String data, BigInteger value) {
        String unit="1000000000000000000";
        BigInteger unitbigint = new BigInteger(unit);
        value = value.multiply(unitbigint);
        try {
            String result = MyUtil.sendethtx(data, StorageUtil.getCurrentPrivatekey(this),value.toString(16));
            if(result == null){
                Toast.makeText(BuyNFTsActivity.this,"Transaction Failed",Toast.LENGTH_LONG).show();
                return;
            }
            try {
                // 先检查是否是有效的JSON
                if (result.trim().startsWith("{")) {
                    JSONObject response = new JSONObject(result);
                    if (response.has("error")) {
                        Toast.makeText(BuyNFTsActivity.this,
                                "购买失败: " + response.getString("error"),
                                Toast.LENGTH_LONG).show();
                        //刷新市场NFT信息
                        fetchListedNFTs();
                    } else {
                        String txHash = response.getString("result");
                        checkTransactionStatus(txHash);
                    }
                } else {
                    // 处理非JSON响应（如404）
                    runOnUiThread(() ->
                            Toast.makeText(BuyNFTsActivity.this,
                                    "服务器错误: " + result,
                                    Toast.LENGTH_LONG).show()
                    );
                }
            } catch (JSONException e) {
                e.printStackTrace();
                // 处理无效JSON
                runOnUiThread(() ->
                        Toast.makeText(BuyNFTsActivity.this,
                                "响应格式错误",
                                Toast.LENGTH_LONG).show()
                );
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "购买请求失败", Toast.LENGTH_SHORT).show();
        }
    }

    private void checkTransactionStatus(String txHash) {
        try {
            if(txHash.startsWith("0x")){
                txHash = txHash.substring(2);
            }
            String result = MyUtil.getTransactionReceipt(txHash, StorageUtil.getCurrentPrivatekey(this));
            if(result == null){
                Toast.makeText(BuyNFTsActivity.this, "checkTransactionStatus失败，服务器响应数据为空", Toast.LENGTH_SHORT).show();
                return;
            }
            try {
                JSONObject response = new JSONObject(result);
                JSONObject receipt = response.optJSONObject("result");

                if (receipt != null) {
                    String status = receipt.optString("status", "0x0");
                    if ("0x1".equals(status)) {
                        runOnUiThread(() -> {
                            Toast.makeText(BuyNFTsActivity.this, "✅ 购买成功", Toast.LENGTH_SHORT).show();
                            //刷新市场NFT信息
                            fetchListedNFTs();
                        });
                    } else {
                        runOnUiThread(() -> {
                            Toast.makeText(BuyNFTsActivity.this, "购买失败！", Toast.LENGTH_SHORT).show();
                            //刷新市场NFT信息
                            fetchListedNFTs();
                        });
                    }
                } else {
                    // 交易尚未上链，继续轮询
                    String finalTxHash = txHash;
                    new Handler().postDelayed(() -> checkTransactionStatus(finalTxHash), 2000);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "查询请求失败", Toast.LENGTH_SHORT).show();
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
