package com.example.brokerfi.nft;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.example.brokerfi.R;
import com.example.brokerfi.main.menu.NavigationHelper;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import com.example.brokerfi.nft.model.NFT;
import com.example.brokerfi.send.SendActivity;


public class NFTMintingActivity extends AppCompatActivity {
    private ImageView menu;
    private ImageView notificationBtn;
    private RelativeLayout action_bar;
    private NavigationHelper navigationHelper;

    private EditText nftNameEditText;
    private EditText nftDescriptionEditText;
    private Spinner imageTypeSpinner;
    private Button selectImageButton;
    private Button mintButton;
    private TextView selectedImageText;
    private ImageView previewImageView;

    private Uri selectedImageUri;
    private String selectedImagePath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nft_minting);

        intView();
        intEvent();
    }

    private void intView() {
        menu = findViewById(R.id.menu);
        notificationBtn = findViewById(R.id.notificationBtn);
        action_bar = findViewById(R.id.action_bar);

        nftNameEditText = findViewById(R.id.nftNameEditText);
        nftDescriptionEditText = findViewById(R.id.nftDescriptionEditText);
        imageTypeSpinner = findViewById(R.id.imageTypeSpinner);
        selectImageButton = findViewById(R.id.selectImageButton);
        mintButton = findViewById(R.id.mintButton);
        selectedImageText = findViewById(R.id.selectedImageText);
        previewImageView = findViewById(R.id.previewImageView);
    }

    private void intEvent() {
        navigationHelper = new NavigationHelper(menu, action_bar, this, notificationBtn);

        selectImageButton.setOnClickListener(v -> selectImage());
        mintButton.setOnClickListener(v -> mintNFT());
    }

    private void selectImage() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(Intent.createChooser(intent, getString(R.string.nft_minting_chooser_select_image)), 1002);
    }

    private void mintNFT() {
        String nftName = nftNameEditText.getText().toString().trim();
        String nftDescription = nftDescriptionEditText.getText().toString().trim();
        String imageType = imageTypeSpinner.getSelectedItem().toString();

        if (nftName.isEmpty()) {
            Toast.makeText(this, R.string.activity_nft_minting_hint_name, Toast.LENGTH_SHORT).show();
            return;
        }

        if (nftDescription.isEmpty()) {
            Toast.makeText(this, R.string.activity_nft_minting_hint_description, Toast.LENGTH_SHORT).show();
            return;
        }

        if (getString(R.string.nft_minting_image_type_custom).equals(imageType) && selectedImageUri == null) {
            Toast.makeText(this, R.string.nft_minting_toast_select_image, Toast.LENGTH_SHORT).show();
            return;
        }

        //Display casting status
        mintButton.setEnabled(false);
        mintButton.setText(R.string.nft_minting_minting);

        new Thread(() -> {
            try {
                // Call NFT casting API
                String result = NFTApiUtil.mintNFT(
                    nftName,
                    nftDescription,
                    imageType,
                    selectedImagePath
                );

                runOnUiThread(() -> {
                    mintButton.setEnabled(true);
                    mintButton.setText(R.string.activity_nft_minting_button_mint_nft);

                    if (result != null && result.contains("success")) {
                        Toast.makeText(this, R.string.nft_minting_toast_nft_mint_successful, Toast.LENGTH_LONG).show();
                        clearForm();
                    } else {
                        Toast.makeText(this, getString(R.string.mint_toast_mint_failed_message_prefix) + " " + result, Toast.LENGTH_LONG).show();
                    }
                });
            } catch (Exception e) {
                Log.e("NFTMinting", "铸造失败", e);
                runOnUiThread(() -> {
                    mintButton.setEnabled(true);
                    mintButton.setText(R.string.activity_nft_minting_button_mint_nft);
                    Toast.makeText(this, getString(R.string.mint_toast_mint_failed_message_prefix) + " " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    private void clearForm() {
        nftNameEditText.setText("");
        nftDescriptionEditText.setText("");
        selectedImageText.setText(R.string.activity_nft_minting_no_image_selected);
        previewImageView.setImageResource(android.R.color.transparent);
        selectedImageUri = null;
        selectedImagePath = null;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 1002 && resultCode == RESULT_OK && data != null) {
            selectedImageUri = data.getData();
            if (selectedImageUri != null) {
                try {
                    //Copy the image to the application's internal storage.
                    selectedImagePath = copyImageToInternalStorage(selectedImageUri);
                    selectedImageText.setText(selectedImageText.getContext().getString(R.string.nft_minting_image_selected) + " " + getImageName(selectedImageUri));

                    // show preview
                    previewImageView.setImageURI(selectedImageUri);
                } catch (Exception e) {
                    Log.e("NFTMinting", "图片处理失败", e);
                    Toast.makeText(this, R.string.nft_minting_toast_image_failed, Toast.LENGTH_SHORT).show();
                }
            }
        }

        IntentResult intentResult = IntentIntegrator.parseActivityResult(
                requestCode, resultCode, data
        );
        if (intentResult.getContents() != null) {
            String scannedData = intentResult.getContents();
            Intent intent = new Intent(this, SendActivity.class);
            intent.putExtra("scannedData", scannedData);
            startActivity(intent);
        }
    }

    private String copyImageToInternalStorage(Uri uri) throws Exception {
        InputStream inputStream = getContentResolver().openInputStream(uri);
        File file = new File(getFilesDir(), "nft_image_" + System.currentTimeMillis() + ".jpg");
        FileOutputStream outputStream = new FileOutputStream(file);

        byte[] buffer = new byte[1024];
        int length;
        while ((length = inputStream.read(buffer)) > 0) {
            outputStream.write(buffer, 0, length);
        }

        inputStream.close();
        outputStream.close();

        return file.getAbsolutePath();
    }

    private String getImageName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            try (android.database.Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);
                    if (nameIndex >= 0) {
                        result = cursor.getString(nameIndex);
                    }
                }
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }

    @Override
    public void onBackPressed() {
        if (navigationHelper != null && navigationHelper.isPopupVisible()) {
            navigationHelper.hidePopup();
        } else {
            super.onBackPressed();
        }
    }

}
