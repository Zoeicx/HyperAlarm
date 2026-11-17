package com.hyperalarm.ui;

import android.content.Context;

import com.hyperalarm.utils.LanguageHelper;

import fan.appcompat.app.AppCompatActivity;

public class LauncherActivity extends AppCompatActivity {

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LanguageHelper.wrapContext(newBase));
    }
}
