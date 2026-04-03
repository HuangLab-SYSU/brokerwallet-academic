package com.example.brokerfi.xc.manager;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.tencent.cos.xml.CosXmlService;
import com.tencent.cos.xml.exception.CosXmlClientException;
import com.tencent.cos.xml.exception.CosXmlServiceException;
import com.tencent.cos.xml.listener.CosXmlResultListener;
import com.tencent.cos.xml.model.CosXmlRequest;
import com.tencent.cos.xml.model.CosXmlResult;
import com.tencent.cos.xml.model.object.PutObjectRequest;
import com.tencent.cos.xml.model.object.PutObjectResult;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class CosUploadManager {

    private static final String TAG = "COS_UPLOAD";
    private static final String BUCKET = "wallet-community-1416742399";

    private final Context context;
    private final CosXmlService cosXmlService;

    public CosUploadManager(Context context, CosXmlService cosXmlService) {
        this.context = context.getApplicationContext();
        this.cosXmlService = cosXmlService;
    }

    public void uploadSingle(Uri uri, UploadCallback callback) {
        List<Uri> list = new ArrayList<>();
        list.add(uri);
        uploadMultiple(list, callback);
    }

    public void uploadMultiple(List<Uri> uris, UploadCallback callback) {

        if (uris == null || uris.isEmpty()) {
            callback.onSuccess(new ArrayList<>());
            return;
        }

        List<String> resultUrls = new ArrayList<>();
        final int total = uris.size();
        final int[] successCount = {0};

        for (Uri uri : uris) {
            uploadOne(uri, new InnerCallback() {
                @Override
                public void onSuccess(String url) {
                    resultUrls.add(url);
                    successCount[0]++;

                    if (successCount[0] == total) {
                        callback.onSuccess(resultUrls);
                    }
                }

                @Override
                public void onFail(String error) {
                    callback.onFail(error);
                }
            });
        }
    }

    private void uploadOne(Uri uri, InnerCallback callback) {
        try {
            File file = uriToFile(uri);

            String cosPath = "community/" + UUID.randomUUID() + ".jpg";

            PutObjectRequest request =
                    new PutObjectRequest(BUCKET, cosPath, file.getAbsolutePath());

            cosXmlService.putObjectAsync(request, new CosXmlResultListener() {
                @Override
                public void onSuccess(CosXmlRequest request, CosXmlResult result) {

                    String url = ((PutObjectResult) result).accessUrl;

                    Log.d(TAG, "上传成功：" + url);

                    file.delete();
                    callback.onSuccess(url);
                }

                @Override
                public void onFail(CosXmlRequest request,
                                   CosXmlClientException clientException,
                                   CosXmlServiceException serviceException) {

                    String error = clientException != null
                            ? clientException.getMessage()
                            : serviceException != null
                            ? serviceException.getMessage()
                            : "unknown error";

                    Log.e(TAG, "上传失败：" + error);

                    file.delete();
                    callback.onFail(error);
                }
            });

        } catch (Exception e) {
            callback.onFail("文件处理失败");
        }
    }

    private File uriToFile(Uri uri) throws Exception {

        InputStream inputStream =
                context.getContentResolver().openInputStream(uri);

        File file = File.createTempFile(
                "cos_" + UUID.randomUUID(),
                ".jpg",
                context.getCacheDir()
        );

        FileOutputStream fos = new FileOutputStream(file);

        byte[] buffer = new byte[4096];
        int len;

        while ((len = inputStream.read(buffer)) != -1) {
            fos.write(buffer, 0, len);
        }

        fos.close();
        inputStream.close();

        return file;
    }

    private interface InnerCallback {
        void onSuccess(String url);
        void onFail(String error);
    }
}