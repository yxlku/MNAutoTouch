package com.zhang.autotouch;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class TestCountActivity extends AppCompatActivity {
    Button bt;
    Button bt2;
    int clickCount = 0;
    int clickCount2 = 0;
    Long time = System.currentTimeMillis();
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test_count);


        bt = findViewById(R.id.bt);
        bt2 = findViewById(R.id.bt2);
        bt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Long newTime = System.currentTimeMillis();
                Long xin = newTime - time;
                Log.e("testCount", "时间差：" + xin + "毫秒");
                bt.setText("测试点击次数" + clickCount++);
                time = newTime;
            }
        });
        bt2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Long newTime = System.currentTimeMillis();
                Long xin = newTime - time;
                Log.e("testCount", "时间差：" + xin + "毫秒");
                bt2.setText("2测试点击次数" + clickCount2++);
                time = newTime;
            }
        });
    }
}
