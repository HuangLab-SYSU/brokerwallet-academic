package com.example.brokerfi.token;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.brokerfi.R;

/** 娣诲姞浠ｅ竵璇存槑锛氬悎绾﹀湴鍧€鏍煎紡涓庡鍏ヨ姹傘€?*/
public class TokenAddHelpActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_token_add_help);

        TokenTopBarHelper.bind(this);

        TextView title = findViewById(R.id.token_toolbar_title);
        title.setText(R.string.token_add_help_title);
    }
}


