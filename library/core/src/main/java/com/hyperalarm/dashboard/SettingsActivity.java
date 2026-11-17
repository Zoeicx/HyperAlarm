/*
 * This file is part of HyperAlarm.

 * HyperAlarm is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.

 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.

 * Copyright (C) 2023-2026 HyperAlarm Contributions
 */
package com.hyperalarm.dashboard;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.hyperalarm.core.R;
import com.hyperalarm.dashboard.base.SettingsBaseActivity;
import com.hyperalarm.utils.PersistConfig;

import fan.preference.PreferenceFragment;

public abstract class SettingsActivity extends SettingsBaseActivity
    implements PreferenceFragment.OnPreferenceStartFragmentCallback {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        if (PersistConfig.isAprilFoolsThemeView) setTheme(R.style.AprilFoolsTheme);
        super.onCreate(savedInstanceState);
        initCreate();
    }

    public void initCreate() {}

    @Override
    public boolean onPreferenceStartFragment(@NonNull PreferenceFragmentCompat caller, @NonNull Preference pref) {
        // The HyperCeiler category screens are gone; the only nested screen left is the
        // multi-action picker, which never adds a preference key.
        onStartSettingsForArguments(pref, false);
        return true;
    }

    public void onStartSettingsForArguments(Preference preference, boolean isAddPreferenceKey) {
        onStartSubSettingsForArguments(this, preference, isAddPreferenceKey);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}
