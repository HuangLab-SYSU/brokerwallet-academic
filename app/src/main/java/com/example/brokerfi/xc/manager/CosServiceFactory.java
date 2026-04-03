package com.example.brokerfi.xc.manager;

import android.content.Context;

import com.tencent.cos.xml.CosXmlService;
import com.tencent.cos.xml.CosXmlServiceConfig;
import com.tencent.qcloud.core.auth.BasicLifecycleCredentialProvider;
import com.tencent.qcloud.core.auth.QCloudLifecycleCredentials;
import com.tencent.qcloud.core.auth.SessionQCloudCredentials;

public class CosServiceFactory {

    private static final String REGION = "ap-guangzhou";

    public static CosXmlService create(Context context,
                                       String tmpSecretId,
                                       String tmpSecretKey,
                                       String sessionToken,
                                       long expiredTime) {

        CosXmlServiceConfig config = new CosXmlServiceConfig.Builder()
                .setRegion(REGION)
                .isHttps(true)
                .builder();

        BasicLifecycleCredentialProvider provider = new BasicLifecycleCredentialProvider() {
            @Override
            protected QCloudLifecycleCredentials fetchNewCredentials() {
                return new SessionQCloudCredentials(
                        tmpSecretId,
                        tmpSecretKey,
                        sessionToken,
                        expiredTime
                );
            }
        };

        return new CosXmlService(context.getApplicationContext(), config, provider);
    }
}
