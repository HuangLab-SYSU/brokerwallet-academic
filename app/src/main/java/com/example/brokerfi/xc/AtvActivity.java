package com.example.brokerfi.xc;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;

import com.example.brokerfi.R;
import com.example.brokerfi.xc.menu.NavigationHelper;
import com.example.brokerfi.xc.model.Transaction;
import com.example.brokerfi.xc.model.TransactionResponse;
import com.example.brokerfi.xc.SecurityUtil;
import com.google.gson.Gson;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AtvActivity extends AppCompatActivity {
    private ImageView menu;
    private ImageView notificationBtn;
    private RelativeLayout action_bar;
    private NavigationHelper navigationHelper;

    //交易组件
    private RecyclerView transactionsRecyclerView;
    private TransactionAdapter transactionAdapter;
    private List<Transaction> transactionList = new ArrayList<>();
    private ProgressBar progressBar;
    private ExecutorService executorService;
    private static final String CACHE_FILE_NAME = "transactions_cache.txt";
    
    private static final String API_URL = "http://dash.broker-chain.com/gettx2?acc=";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_atv);
        intView();
        intEvent();

        initRecyclerView();

        executorService = Executors.newSingleThreadExecutor();

        loadTransactions();
    }
    
    private void initRecyclerView() {
        transactionsRecyclerView = findViewById(R.id.transactions_recycler_view);
        progressBar = findViewById(R.id.progress_bar);
        
        //layoutmanager
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        transactionsRecyclerView.setLayoutManager(layoutManager);
        
        // get currentaccount
        String currentAddress = getCurrentAccountAddress();
        
        // creat adapter
        transactionAdapter = new TransactionAdapter(transactionList, currentAddress);
        transactionsRecyclerView.setAdapter(transactionAdapter);
    }
    
    private void loadTransactions() {
        showProgress();
        Log.d("AtvActivity", "开始加载交易数据...");
        
        
        //testAddressValidation();
        
        executorService.execute(() -> {
            try {
                String currentAddress = getCurrentAccountAddress();
                
                // API URL
                String fullUrl = API_URL + currentAddress;
                Log.d("AtvActivity", "请求URL: " + fullUrl);
                
                URL url = new URL(fullUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(15000);
                connection.setReadTimeout(15000);
                connection.setRequestProperty("Accept", "application/json");
                
                int responseCode = connection.getResponseCode();
                Log.d("AtvActivity", "响应码: " + responseCode);
                
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    InputStream inputStream = connection.getInputStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                    StringBuilder response = new StringBuilder();
                    String line;
                    
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    
                    String responseString = response.toString();
                    Log.d("AtvActivity", "API响应: " + responseString);
                    
                    reader.close();
                    inputStream.close();
                    
                    // 解析
                    Gson gson = new Gson();
                    TransactionResponse transactionResponse = gson.fromJson(responseString, TransactionResponse.class);
                    
                    if (transactionResponse != null && transactionResponse.getData() != null) {
                        List<Transaction> newTransactions = transactionResponse.getData();
                        
                        // 过滤
                        List<Transaction> filteredTransactions = filterTransactions(newTransactions);
                        
                        Log.d("AtvActivity", "获取到交易数量: " + newTransactions.size() + "，过滤后数量: " + filteredTransactions.size());
                        
                        // 缓存
                        cacheTransactions(filteredTransactions);
                        
                        // 在UI线程更新数据
                        runOnUiThread(() -> {
                            transactionList.clear();
                            transactionList.addAll(filteredTransactions);
                            Log.d("AtvActivity", "更新适配器数据，数量: " + transactionList.size());
                            transactionAdapter.updateTransactions(transactionList);
                            hideProgress();
                        });
                    } else {
                        Log.d("AtvActivity", "没有获取到交易数据");
                        runOnUiThread(() -> {
                            Toast.makeText(AtvActivity.this, "没有找到交易记录", Toast.LENGTH_SHORT).show();
                            hideProgress();
                        });
                    }
                } else {
                    Log.e("AtvActivity", "服务器响应错误: " + responseCode);

                    loadCachedTransactions();
                }
                
                connection.disconnect();
            } catch (Exception e) {
                Log.e("AtvActivity", "加载交易数据异常", e);

                loadCachedTransactions();
            }
        });
    }
    
    //过滤包含B2E的交易和非标准地址格式的交易
    private List<Transaction> filterTransactions(List<Transaction> transactions) {
        List<Transaction> filtered = new ArrayList<>();
        if (transactions != null) {
            for (Transaction transaction : transactions) {
                boolean containsB2E = false;
                boolean hasInvalidAddress = false;
                
                // 检查from或to字段是否包含B2E
                if ((transaction.getFrom() != null && transaction.getFrom().contains("B2E")) ||
                    (transaction.getTo() != null && transaction.getTo().contains("B2E"))) {
                    containsB2E = true;
                }
                
                // 检查地址格式
                if ((transaction.getFrom() != null && !isValidAddress(transaction.getFrom())) ||
                    (transaction.getTo() != null && !isValidAddress(transaction.getTo()))) {
                    hasInvalidAddress = true;
                }
                

                if (!containsB2E && !hasInvalidAddress) {
                    filtered.add(transaction);
                } else {
                    Log.d("AtvActivity", "过滤掉交易：from=" + transaction.getFrom() + ", to=" + transaction.getTo() + 
                          ", 原因：" + (containsB2E ? "包含B2E" : "") + (containsB2E && hasInvalidAddress ? "和" : "") + 
                          (hasInvalidAddress ? "地址格式不标准" : ""));
                }
            }
        }
        return filtered;
    }
    
    //Store to local
    private void cacheTransactions(List<Transaction> transactions) {
        try {
            File file = new File(getCacheDir(), CACHE_FILE_NAME);
            FileOutputStream fos = new FileOutputStream(file);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(new ArrayList<>(transactions));
            oos.close();
            fos.close();
            Log.d("AtvActivity", "交易数据已缓存到本地");
        } catch (Exception e) {
            Log.e("AtvActivity", "缓存交易数据失败", e);
        }
    }
    
    //load from local
    private void loadCachedTransactions() {
        try {
            File file = new File(getCacheDir(), CACHE_FILE_NAME);
            if (file.exists()) {
                FileInputStream fis = new FileInputStream(file);
                ObjectInputStream ois = new ObjectInputStream(fis);
                List<Transaction> cachedTransactions = (List<Transaction>) ois.readObject();
                ois.close();
                fis.close();
                
                Log.d("AtvActivity", "从缓存加载交易数据，数量: " + cachedTransactions.size());
                runOnUiThread(() -> {
                    transactionList.clear();
                    transactionList.addAll(cachedTransactions);
                    transactionAdapter.updateTransactions(transactionList);
                    Toast.makeText(AtvActivity.this, "无法获取最新交易数据，显示缓存数据", Toast.LENGTH_SHORT).show();
                    hideProgress();
                });
            } else {
                Log.d("AtvActivity", "没有找到缓存文件");
                runOnUiThread(() -> {
                    Toast.makeText(AtvActivity.this, "无法获取交易数据且无缓存", Toast.LENGTH_SHORT).show();
                    hideProgress();
                });
            }
        } catch (Exception e) {
            Log.e("AtvActivity", "加载缓存数据失败", e);
            runOnUiThread(() -> {
                Toast.makeText(AtvActivity.this, "加载缓存数据失败", Toast.LENGTH_SHORT).show();
                hideProgress();
            });
        }
    }
    
    private void showProgress() {
        runOnUiThread(() -> {
            progressBar.setVisibility(View.VISIBLE);
        });
    }
    
    private void hideProgress() {
        runOnUiThread(() -> {
            progressBar.setVisibility(View.GONE);
        });
    }
    
    //Get addr
    private String getCurrentAccountAddress() {
        String privateKey = StorageUtil.getCurrentPrivatekey(this);
        if (privateKey != null) {
            return SecurityUtil.GetAddress(privateKey);
        }
        return "";
    }
    
    private void intView() {
        menu = findViewById(R.id.menu);
        notificationBtn = findViewById(R.id.notificationBtn);
        action_bar = findViewById(R.id.action_bar);
        
        
        findViewById(R.id.dashedBorderView).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setClass(AtvActivity.this, MainActivity.class);
                startActivity(intent);
            }
        });
    }

    private void intEvent(){
        navigationHelper = new NavigationHelper(menu, action_bar,this,notificationBtn);
    }
    
    @Override
    public void onBackPressed() {
        super.onBackPressed();//退回上一级页面
        overridePendingTransition(R.anim.slide_in_from_left, R.anim.slide_out_to_right);
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        IntentResult intentResult = IntentIntegrator.parseActivityResult(
                requestCode, resultCode, data
        );
        if (intentResult.getContents() != null){
            String scannedData = intentResult.getContents();
            Intent intent = new Intent(this, SendActivity.class);
            intent.putExtra("scannedData", scannedData);
            startActivity(intent);
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
    

    private boolean isValidAddress(String address) {
        if (address == null || address.isEmpty()) {
            return false;
        }
        // 40个字符、十六进制
        return address.length() == 40 && address.matches("^[0-9a-fA-F]+$");
    }

}
