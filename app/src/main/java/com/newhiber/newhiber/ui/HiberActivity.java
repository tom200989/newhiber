package com.newhiber.newhiber.ui;

import android.app.Activity;
import android.os.Bundle;

import com.newhiber.newhiber.R;


/**
 * 默认加载的Activity,如果使用者没有设置启动的Activity的话
 */
public class HiberActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hiber);
    }
}
