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
package com.hyperalarm.provision.fragment;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.view.LayoutInflater;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.SwitchPreference;
import androidx.recyclerview.widget.RecyclerView;

import com.hyperalarm.alarm.core.SettingsKeys;
import com.hyperalarm.common.utils.AppLanguageHelper;
import com.hyperalarm.common.utils.AppSettingsStore;
import com.hyperalarm.common.utils.PrefsConfigurator;
import com.hyperalarm.common.utils.RootShell;
import com.hyperalarm.common.utils.ThreadUtils;
import com.hyperalarm.provision.R;

import fan.appcompat.app.AlertDialog;
import fan.preference.DropDownPreference;
import fan.preference.PreferenceFragment;

/**
 * The wizard's basic settings page.
 *
 * <p>Scope and icon placement used to live here too. Both describe how the module sits in the
 * launcher and in the Xposed manager, which the user does not need on a first run and which the
 * main settings page already covers, so this page keeps the two things a fresh install does need -
 * launcher tile and language - plus the three switches that decide how the clock behaves.</p>
 */
public class BasicSettingsFragment extends PreferenceFragment {

    private static final String LAUNCHER_ACTIVITY_CLASS_NAME = "com.hyperalarm.ui.LauncherActivity";
    private static final String PREF_APP_LANGUAGE = "prefs_key_settings_app_language";
    private static final String PREF_CLOCK_FILTER = "prefs_key_settings_hide_script_alarms_clock";
    private static final String PREF_CLOCK_FILTER_RINGING = "prefs_key_settings_hide_ringing_task_clock";
    private static final String PREF_LOCK_PASSWORD = "prefs_key_lock_password";
    private static final String PASSWORD_FILE = "/data/adb/alarm_shell/password";

    SwitchPreference mHideAppIcon;
    DropDownPreference mLanguagePreference;
    SwitchPreference mClockFilter;
    SwitchPreference mClockFilterRinging;
    Preference mLockPassword;

    private RecyclerView mRecyclerView;

    @Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
        PrefsConfigurator.setup(this);
        setPreferencesFromResource(R.xml.provision_basic_settings, rootKey);

        mHideAppIcon = findPreference("prefs_key_settings_hide_app_icon");
        mLanguagePreference = findPreference(PREF_APP_LANGUAGE);
        mClockFilter = findPreference(PREF_CLOCK_FILTER);
        mClockFilterRinging = findPreference(PREF_CLOCK_FILTER_RINGING);
        mLockPassword = findPreference(PREF_LOCK_PASSWORD);

        if (mHideAppIcon != null) mHideAppIcon.setPersistent(false);
        if (mClockFilter != null) mClockFilter.setPersistent(false);
        if (mClockFilterRinging != null) mClockFilterRinging.setPersistent(false);
        if (mLockPassword != null) mLockPassword.setPersistent(false);
        if (mLanguagePreference != null) {
            mLanguagePreference.setPersistent(false);
            mLanguagePreference.setEntries(AppLanguageHelper.getLanguageEntries(requireContext()));
            mLanguagePreference.setEntryValues(AppLanguageHelper.getLanguageEntryValues());
        }
    }

    @Override
    public void onViewCreated(View view, Bundle bundle) {
        super.onViewCreated(view, bundle);
        mRecyclerView = getListView();
        mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
            }
        });

        boolean hideAppIconEnabled = AppSettingsStore.isHideAppIconEnabled(requireContext());
        if (mHideAppIcon != null) mHideAppIcon.setChecked(hideAppIconEnabled);
        if (mLanguagePreference != null) {
            mLanguagePreference.setValueIndex(
                    AppLanguageHelper.getCurrentLanguageIndex(requireContext()));
        }
        applyLauncherIconState(hideAppIconEnabled);
        bindClockFilters();
        bindLockPassword();

        mHideAppIcon.setOnPreferenceChangeListener((preference, newValue) -> {
            boolean enabled = Boolean.TRUE.equals(newValue);
            AppSettingsStore.setHideAppIconEnabled(requireContext(), enabled);
            applyLauncherIconState(enabled);
            return true;
        });
        mLanguagePreference.setOnPreferenceChangeListener((preference, newValue) -> {
            int index = Integer.parseInt((String) newValue);
            AppLanguageHelper.setIndexLanguage(requireActivity(), index, true);
            return true;
        });
    }

    /**
     * The two clock switches write a system global setting through root, exactly like the main
     * settings page: the clock process reads it, and a normal app cannot write it.
     */
    private void bindClockFilters() {
        if (mClockFilter != null) {
            mClockFilter.setChecked(readGlobal(SettingsKeys.CLOCK_FILTER) == 1);
            mClockFilter.setOnPreferenceChangeListener((preference, newValue) ->
                    writeClockFilter((SwitchPreference) preference, SettingsKeys.CLOCK_FILTER,
                            Boolean.TRUE.equals(newValue)));
        }
        if (mClockFilterRinging != null) {
            mClockFilterRinging.setChecked(readGlobal(SettingsKeys.CLOCK_FILTER_RINGING) == 1);
            mClockFilterRinging.setOnPreferenceChangeListener((preference, newValue) ->
                    writeClockFilter((SwitchPreference) preference, SettingsKeys.CLOCK_FILTER_RINGING,
                            Boolean.TRUE.equals(newValue)));
        }
    }

    private int readGlobal(String key) {
        // Reading a global setting needs no permission; writing it does.
        try {
            return Settings.Global.getInt(requireContext().getContentResolver(), key, 0);
        } catch (Throwable t) {
            return 0;
        }
    }

    private boolean writeClockFilter(SwitchPreference preference, String key, boolean value) {
        final Context app = requireContext().getApplicationContext();
        ThreadUtils.runOnBackground(() -> {
            boolean ok = RootShell.succeeded("settings put global " + key + " " + (value ? "1" : "0"));
            ThreadUtils.postOnMainThread(() -> {
                if (!isAdded()) return;
                // Repaint from what is actually stored; on failure put the switch back.
                preference.setChecked(ok ? value : !value);
                if (!ok) {
                    Toast.makeText(requireContext(), R.string.clock_filter_no_root,
                            Toast.LENGTH_LONG).show();
                }
            });
        });
        return true;
    }

    /** Optional on purpose: a silent task only needs it when the screen is locked. */
    private void bindLockPassword() {
        if (mLockPassword == null) return;
        lockPasswordSummary();
        mLockPassword.setOnPreferenceClickListener(preference -> {
            showLockPasswordDialog();
            return true;
        });
    }

    private void lockPasswordSummary() {
        if (mLockPassword == null) return;
        final Preference preference = mLockPassword;
        ThreadUtils.runOnBackground(() -> {
            boolean set = !RootShell.runOutput("cat " + RootShell.quote(PASSWORD_FILE)).trim().isEmpty();
            ThreadUtils.postOnMainThread(() -> {
                if (!isAdded() || mLockPassword == null) return;
                preference.setSummary(set
                        ? R.string.lock_password_summary_set
                        : R.string.lock_password_summary_unset);
            });
        });
    }

    private void showLockPasswordDialog() {
        View content = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_lock_password, null, false);
        final EditText input = content.findViewById(R.id.lock_password_input);
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.lock_password_title)
                .setView(content)
                .setNegativeButton(android.R.string.cancel, null)
                .setNeutralButton(R.string.lock_password_clear, (dialog, which) -> writePassword(""))
                .setPositiveButton(R.string.lock_password_save, (dialog, which) -> {
                    String typed = input.getText() == null ? "" : input.getText().toString();
                    if (typed.isEmpty()) {
                        // Empty means "leave it alone": clearing is the explicit button.
                        Toast.makeText(requireContext(), R.string.lock_password_unchanged,
                                Toast.LENGTH_SHORT).show();
                        return;
                    }
                    writePassword(typed);
                })
                .show();
    }

    private void writePassword(String password) {
        final Context app = requireContext().getApplicationContext();
        final String value = password == null ? "" : password;
        ThreadUtils.runOnBackground(() -> {
            boolean ok;
            if (value.isEmpty()) {
                ok = RootShell.succeeded(": > " + RootShell.quote(PASSWORD_FILE)
                        + " && chmod 600 " + RootShell.quote(PASSWORD_FILE));
            } else {
                ok = RootShell.succeeded("mkdir -p " + RootShell.quote("/data/adb/alarm_shell")
                        + " && printf '%s' " + RootShell.quote(value) + " > "
                        + RootShell.quote(PASSWORD_FILE)
                        + " && chmod 600 " + RootShell.quote(PASSWORD_FILE));
            }
            final boolean done = ok;
            ThreadUtils.postOnMainThread(() -> {
                if (!isAdded()) return;
                Toast.makeText(requireContext(),
                        done ? (value.isEmpty()
                                ? R.string.lock_password_cleared : R.string.lock_password_saved)
                                : R.string.lock_password_failed_toast,
                        Toast.LENGTH_SHORT).show();
                lockPasswordSummary();
            });
        });
    }

    private void applyLauncherIconState(boolean enabled) {
        PackageManager packageManager = requireActivity().getPackageManager();
        int componentEnabledState = enabled
            ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            : PackageManager.COMPONENT_ENABLED_STATE_DISABLED;

        try {
            packageManager.setComponentEnabledSetting(
                new ComponentName(requireContext(), LAUNCHER_ACTIVITY_CLASS_NAME),
                componentEnabledState,
                PackageManager.DONT_KILL_APP
            );
        } catch (Exception ignored) {
        }
    }

    public void adjustNextView() {
    }

}
