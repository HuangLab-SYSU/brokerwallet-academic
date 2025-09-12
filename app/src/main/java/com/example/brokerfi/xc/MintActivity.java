package com.example.brokerfi.xc;

import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONArray;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;


import android.widget.RelativeLayout;
import android.widget.EditText;


import com.example.brokerfi.R;
import com.example.brokerfi.xc.menu.NavigationHelper;


import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import java.math.BigInteger;

import android.os.Handler;

import com.example.brokerfi.xc.net.ABIUtils;


public class MintActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "MyPrefsFile";
    private static final String PREF_ACCOUNT_NUMBER = "accountNumber";
    private ImageView menu;
    private ImageView notificationBtn;
    private RelativeLayout action_bar;

    private EditText edt_nft_name, edt_shares, edt_gas;

    private NavigationHelper navigationHelper;
    private Button btn_doCamera, btn_doFile, btn_cancel, btn_mint;

    private Uri imageUri; //保存用户选择的图片
    private ImageView uploadView;
    private int hasImage = 0; // 0-无图片 1-有图片

    private TextView warning;

    private static final int PERMISSION_CAMERA = 2001;
    private static final int PERMISSION_STORAGE = 2002;

    private ActivityResultLauncher<Intent> cameraLauncher;
    private ActivityResultLauncher<Intent> galleryLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mint);
        initResultLaunchers();
        intView();
        intEvent();
        findViewById(R.id.dashedBorderView).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setClass(MintActivity.this, MainActivity.class);
                startActivity(intent);
            }
        });
    }

    private void initResultLaunchers() {
        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        handleCameraImage();
                    }
                });
        galleryLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        handleGalleryImage(result.getData());
                    }
                });
    }

    private void intView() {
        menu = findViewById(R.id.menu);
        notificationBtn = findViewById(R.id.notificationBtn);
        action_bar = findViewById(R.id.action_bar);
        edt_nft_name = findViewById(R.id.edt_nft_name);
        edt_shares = findViewById(R.id.edt_shares);
        btn_doCamera = findViewById(R.id.btn_doCamera);
        btn_doFile = findViewById(R.id.btn_doFile);
        btn_cancel = findViewById(R.id.btn_cancel);
        btn_mint = findViewById(R.id.btn_mint);
        uploadView = findViewById(R.id.uploadIcon);
        edt_gas = findViewById(R.id.edt_gas);
        warning = findViewById(R.id.warning);
    }

    private void intEvent() {
        navigationHelper = new NavigationHelper(menu, action_bar, this,notificationBtn);
        btn_doCamera.setOnClickListener(v -> handleCameraClick());
        btn_doFile.setOnClickListener(v -> handleGalleryClick());

        btn_cancel.setOnClickListener(view -> {
            Intent intent = new Intent();
            intent.setClass(MintActivity.this, NFTMainActivity.class);
            startActivity(intent);
        });

        btn_mint.setOnClickListener(v -> {
            String nftname = edt_nft_name.getText().toString();
            String sharesStr = edt_shares.getText().toString();
//            if (hasImage == 0) {
//                Toast.makeText(this, "请先上传图片", Toast.LENGTH_SHORT).show();
//                return;
//            }
            if (nftname.isEmpty()) {
                edt_nft_name.setError("必填字段");
                edt_nft_name.requestFocus();
                Toast.makeText(MintActivity.this, "请输入NFT名字", Toast.LENGTH_SHORT).show();
                return;
            }
            if (sharesStr.isEmpty()) {
                edt_shares.setError("必填字段");
                edt_shares.requestFocus();
                Toast.makeText(MintActivity.this, "请输入NFT总份数", Toast.LENGTH_SHORT).show();
                return;
            }
            try {
                int share = Integer.parseInt(sharesStr);
                if (share < 1) {
                    edt_shares.setError("最小为1份");
                    Toast.makeText(MintActivity.this, "份数不能小于1", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (share > 10000) {
                    edt_shares.setError("超过最大限制");
                    Toast.makeText(MintActivity.this, "最多将NFT分为10000份", Toast.LENGTH_SHORT).show();
                    return;
                }
            } catch (NumberFormatException e) {
                edt_shares.setError("格式示例：100");
                Toast.makeText(MintActivity.this, "请输入有效的整数份数", Toast.LENGTH_SHORT).show();
                return;
            }


            try {
                String base64Image;
                if (hasImage == 1) {
                    InputStream in = getContentResolver().openInputStream(imageUri);
                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inSampleSize = 4; // 直接缩小4倍
                    Bitmap bitmap = BitmapFactory.decodeStream(in, null, options);
                    in.close();
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    int quality = 70;
                    bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out);
                    while (out.toByteArray().length > 200 * 1024 && quality > 50) {
                        out.reset();
                        quality -= 10;
                        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out);
                    }
                    byte[] imageBytes = out.toByteArray();
                    base64Image = Base64.encodeToString(imageBytes, Base64.NO_WRAP);
                } else {
                    base64Image = "123";
                }
                BigInteger share = new BigInteger(sharesStr);
                String data = ABIUtils.encodeMint(nftname, base64Image, share);
                sendMintTransaction(data, BigInteger.TEN, edt_gas.getText().toString());
            } catch (Exception e) {
                Toast.makeText(this, " " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void handleCameraClick() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.CAMERA}, PERMISSION_CAMERA);
                return;
            }
        }
        startCamera();
    }

    private void handleGalleryClick() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.READ_MEDIA_IMAGES}, PERMISSION_STORAGE);
                return;
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSION_STORAGE);
                return;
            }
        }
        startGallery();
    }

    private void startCamera() {
        try {
            File photoFile = createImageFile();
            imageUri = FileProvider.getUriForFile(this, "com.example.brokerfi.fileprovider", photoFile);
            Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
            cameraLauncher.launch(cameraIntent);
        } catch (IOException e) {
            Toast.makeText(this, "创建文件失败", Toast.LENGTH_SHORT).show();
        }
    }

    private void startGallery() {
        try {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            galleryLauncher.launch(intent);
        } catch (Exception e) {
            Intent fallback = new Intent(Intent.ACTION_VIEW);
            fallback.setData(Uri.parse(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString()));
            startActivity(fallback);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_STORAGE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startGallery();
            } else {
                Toast.makeText(this, "需要权限才能访问相册", Toast.LENGTH_SHORT).show();
            }
        }

        if (requestCode == PERMISSION_CAMERA) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera();
            } else {
                Toast.makeText(this, "需要权限访问相机", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void handleCameraImage() {
        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
            uploadView.setImageBitmap(bitmap);
            hasImage = 1;
        } catch (IOException e) {
            Toast.makeText(this, "加载图片失败", Toast.LENGTH_SHORT).show();
        }
    }

    private void handleGalleryImage(Intent data) {
        if (data != null && data.getData() != null) {
            imageUri = data.getData();
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
                uploadView.setImageBitmap(bitmap);
                hasImage = 1;
            } catch (IOException e) {
                Toast.makeText(this, "加载图片失败", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String fileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        return File.createTempFile(fileName, ".jpg", storageDir);
    }

    private void sendMintTransaction(String data, BigInteger value, String gas) {
        try {
            String privatekey = StorageUtil.getCurrentPrivatekey(this);
            if (privatekey == null) {
                runOnUiThread(() ->
                        Toast.makeText(MintActivity.this,
                                "铸造失败",
                                Toast.LENGTH_LONG).show()
                );
                return;
            }
            String result = MyUtil.sendethtx2(data, privatekey, gas);
            if (result == null) {
                runOnUiThread(() -> {
                            Toast.makeText(MintActivity.this,
                                    "铸造失败,服务器响应为空",
                                    Toast.LENGTH_LONG).show();

                        warning.setText("铸造失败,服务器响应为空");

                        }
                );
                return;
            }
            try {
                if (result.trim().startsWith("{")) {
                    JSONObject response = new JSONObject(result);
                    if (response.has("error")) {
                        String error = response.getString("error");
                        Toast.makeText(MintActivity.this,
                                "铸造失败: " + error,
                                Toast.LENGTH_LONG).show();
                        runOnUiThread(() -> {
                            warning.setText(error);
                        });

                    } else {
                        String txHash = response.getString("result");
                        checkTransactionStatus(txHash);
                    }
                } else {
                    runOnUiThread(() -> {
                                Toast.makeText(MintActivity.this,
                                        "服务器错误: " + result,
                                        Toast.LENGTH_LONG).show();

                                warning.setText(result);
                            }
                    );
                }
            } catch (JSONException e) {
                e.printStackTrace();
                runOnUiThread(() ->
                        Toast.makeText(MintActivity.this,
                                "响应格式错误",
                                Toast.LENGTH_LONG).show()
                );
            }

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "交易请求失败", Toast.LENGTH_SHORT).show();
        }
    }

    private void checkTransactionStatus(String txHash) {
        if (txHash.startsWith("0x")) {
            txHash = txHash.substring(2);
        }
        try {
            JSONArray params = new JSONArray();
            params.put(txHash);
            String result = MyUtil.getTransactionReceipt(txHash, StorageUtil.getCurrentPrivatekey(this));
            if (result == null) {
                runOnUiThread(() ->
                        Toast.makeText(MintActivity.this, "交易失败！", Toast.LENGTH_SHORT).show()
                );
                return;
            }
            JSONObject response = new JSONObject(result);
            JSONObject receipt = response.optJSONObject("result");

            if (receipt != null) {
                String status = receipt.optString("status", "0x0");
                if ("0x1".equals(status)) {
                    runOnUiThread(() -> {
                        Toast.makeText(MintActivity.this, "铸造成功", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent();
                        intent.setClass(MintActivity.this, CongratulationsMintActivity.class);
                        //跳转
                        startActivity(intent);
                    });
                } else {
                    runOnUiThread(() ->
                            Toast.makeText(MintActivity.this, "交易失败！", Toast.LENGTH_SHORT).show()
                    );
                }
            } else {
                // 交易尚未上链，继续轮询
                String finalTxHash = txHash;
                new Handler().postDelayed(() -> checkTransactionStatus(finalTxHash), 2000);
            }
        } catch (JSONException e) {
            e.printStackTrace();
            Toast.makeText(this, "查询请求失败", Toast.LENGTH_SHORT).show();
        }
    }
}