/*
 * This file is part of HyperAlarm.
 *
 * HyperAlarm is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (C) 2023-2026 HyperAlarm Contributions
 */
package com.hyperalarm.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;

import com.hyperalarm.home.safemode.AppCrashStore;

import fan.appcompat.app.AppCompatActivity;
import fan.provision.OobeUtils;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(com.hyperalarm.utils.LanguageHelper.wrapContext(newBase));
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (AppCrashStore.hasPendingCrash(this)) {
            startActivity(AppCrashStore.createIntent(this));
            finish();
            return;
        }

        Intent intent;
        // 根据逻辑分发
        intent = new Intent(this, HomePageActivity.class);
        startActivity(intent);
        // 3. 必须 finish，否则用户按返回键会回到空白的 Splash 页面
        finish();
    }
}
