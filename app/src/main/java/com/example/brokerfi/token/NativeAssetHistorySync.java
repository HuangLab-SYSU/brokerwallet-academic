package com.example.brokerfi.token;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import com.example.brokerfi.brokerfi.model.TransactionResponse;
import com.example.brokerfi.core.config.ChainConfig;
import com.google.gson.Gson;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import okhttp3.*;

/** Bounded read-only adapter over the existing wallet native transaction API. */
final class NativeAssetHistorySync {
    private static final java.util.concurrent.ExecutorService WORK=Executors.newSingleThreadExecutor();
    private static final OkHttpClient HTTP=new OkHttpClient.Builder().callTimeout(20,TimeUnit.SECONDS).retryOnConnectionFailure(false).build();
    static void sync(Context context,String wallet,Runnable done) {
        Context app=context.getApplicationContext();
        WORK.execute(() -> {
            try (Response response=HTTP.newCall(new Request.Builder().url(ChainConfig.getGetTx2AccountUrl(wallet)).build()).execute()) {
                if (response.isSuccessful() && response.body()!=null) {
                    TransactionResponse body=new Gson().fromJson(response.body().string(),TransactionResponse.class);
                    if (body!=null) for(TokenTxRecord r:NativeAssetHistory.convert(body.getData(),wallet)) TokenTxHistoryStore.addIfAbsent(app,wallet,r);
                }
            } catch (Exception ignored) { } finally { if(done!=null)new Handler(Looper.getMainLooper()).post(done); }
        });
    }
}
