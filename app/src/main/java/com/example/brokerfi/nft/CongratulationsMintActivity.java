package com.example.brokerfi.nft;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.example.brokerfi.R;
import com.example.brokerfi.nft.model.NFT;


public class CongratulationsMintActivity extends AppCompatActivity {
    private Button btn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_congratulations_mint);
        intView();
        intEvent();
    }

    private void intView() {
        btn = findViewById(R.id.btn_back);
    }

    private void intEvent(){
        btn.setOnClickListener(view -> {
            // Create intent object
            Intent intent = new Intent();
            intent.setClass(CongratulationsMintActivity.this, NFTMainActivity.class);
            // Navigate
            startActivity(intent);
        });

    }
}
