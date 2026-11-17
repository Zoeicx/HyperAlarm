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
package com.hyperalarm.settings;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Rect;
import android.net.Uri;
import android.provider.Settings;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.preference.Preference;
import androidx.preference.SwitchPreference;

import com.hyperalarm.R;
import com.hyperalarm.common.utils.RootShell;
import com.hyperalarm.alarm.core.SettingsKeys;
import com.hyperalarm.common.base.BasePreferenceFragment;
import com.hyperalarm.common.log.AndroidLog;
import com.hyperalarm.common.log.LogLevelManager;
import com.hyperalarm.common.utils.AppSettingsStore;
import com.hyperalarm.common.utils.PrefsBridge;
import com.hyperalarm.common.utils.api.ProjectApi;
import com.hyperalarm.libhook.utils.api.BackupUtils;
import com.hyperalarm.ui.LauncherActivity;
import com.hyperalarm.ui.SplashActivity;
import com.hyperalarm.utils.DialogHelper;
import com.hyperalarm.utils.HotReloadDialogHelper;
import com.hyperalarm.utils.HotReloadManager;
import com.hyperalarm.utils.LanguageHelper;
import com.hyperalarm.utils.ScopeManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import fan.appcompat.app.AlertDialog;
import fan.internal.utils.ViewUtils;
import fan.preference.DropDownPreference;
import fan.provision.OobeUtils;
import com.hyperalarm.common.utils.ThreadUtils;

public class SettingsFragment extends BasePreferenceFragment
    implements Preference.OnPreferenceClickListener, Preference.OnPreferenceChangeListener {

    /**
     * Clock list filter. This switch has exactly one storage: the system global key. The clock
     * process reads it inside its own list adapter, the app reads and writes the same key, so the
     * displayed state and the actual behaviour cannot drift apart (which used to happen when the
     * module prefs said "on" but the global write had failed).
     */

    SwitchPreference mFloatBottomPreference;
    DropDownPreference mIconModePreference;
    DropDownPreference mIconModeValue;
    SwitchPreference mHideAppIcon;
    SwitchPreference mShowAllAlarms;
    Preference mLockPassword;
    SwitchPreference mHideScriptAlarmsClock;
    SwitchPreference mHideRingingTaskClock;

    DropDownPreference mLogLevel;
    DropDownPreference mLanguage;
    Preference mHotReloadPreference;
    private final ScopeManager.ServiceStateListener mServiceStateListener =
        service -> updateHotReloadPreferenceVisibility();

    // 记录当前操作类型：0-备份，1-恢复
    private final int currentAction = -1;
    private static final String TAG = "SettingsFragment";
    private static final int ACTION_BACKUP = 0;
    private static final int ACTION_RESTORE = 1;

    private final ActivityResultLauncher<Intent> mBackupLauncher = registerForActivityResult(
        new ActivityResultContracts.StartActivityForResult(),
        result -> {
            if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                processBackup(result.getData().getData());
            }
        }
    );

    // 2. 注册恢复启动器
    private final ActivityResultLauncher<Intent> mRestoreLauncher = registerForActivityResult(
        new ActivityResultContracts.StartActivityForResult(),
        result -> {
            if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                processRestore(result.getData().getData());
            }
        }
    );

    @Override
    public int getPreferenceScreenResId() {
        return R.xml.prefs_settings;
    }

    @Override
    public void onResume() {
        super.onResume();
        // The filter is a system global, so anything (root shell, another tool) can change it while
        // this page is alive. Re-read on resume instead of showing the value captured at creation.
        if (mHideScriptAlarmsClock != null) {
            mHideScriptAlarmsClock.setChecked(isClockScriptFilterEnabled(requireContext()));
        }
        if (mHideRingingTaskClock != null) {
            mHideRingingTaskClock.setChecked(isClockRingingTaskHidden(requireContext()));
        }
    }

    @Override
    public void initPrefs() {
        int mIconMode = AppSettingsStore.getIconIndex(requireContext());
        int iconModeStyle = AppSettingsStore.getIconModeIndex(requireContext());
        int languageIndex = LanguageHelper.getCurrentLanguageIndex(requireContext());
        boolean hideAppIconEnabled = AppSettingsStore.isHideAppIconEnabled(requireContext());
        boolean isFloating = AppSettingsStore.isFloatNavEnabled(requireContext());
        boolean showAllAlarms = AppSettingsStore.isShowAllAlarmsEnabled(requireContext());
        boolean hideScriptAlarmsClock = isClockScriptFilterEnabled(requireContext());

        mIconModePreference = findPreference("prefs_key_settings_icon");
        mIconModeValue = findPreference("prefs_key_settings_icon_mode");
        mLanguage = findPreference("prefs_key_settings_app_language");
        mHideAppIcon = findPreference("prefs_key_settings_hide_app_icon");
        mFloatBottomPreference = findPreference("prefs_key_settings_float_nav");
        mShowAllAlarms = findPreference("prefs_key_settings_show_all_alarms");
        mHideScriptAlarmsClock = findPreference("prefs_key_settings_hide_script_alarms_clock");
        mHideRingingTaskClock = findPreference("prefs_key_settings_hide_ringing_task_clock");
        mLockPassword = findPreference("prefs_key_lock_password");
        Preference tutorial = findPreference("prefs_key_restart_tutorial");
        if (tutorial != null) {
            tutorial.setPersistent(false);
            tutorial.setOnPreferenceClickListener(preference -> {
                // Replays the setup wizard in its debug mode: it is skipped on an already provisioned
                // install, and debug mode does not touch the real provisioning state.
                Intent intent = new Intent();
                intent.setClassName(requireContext(),
                        "com.hyperalarm.provision.activity.DefaultActivity");
                intent.putExtra("extra_debug_oobe", true);
                startActivity(intent);
                return true;
            });
        }
        mLogLevel = findPreference("prefs_key_log_level_v2");
        mHotReloadPreference = findPreference("prefs_key_settings_hot_reload");
        updateHotReloadPreferenceVisibility();

        if (mHideAppIcon != null) {
            mHideAppIcon.setPersistent(false);
        }
        if (mFloatBottomPreference != null) {
            mFloatBottomPreference.setPersistent(false);
        }
        if (mShowAllAlarms != null) {
            mShowAllAlarms.setPersistent(false);
        }
        if (mHideScriptAlarmsClock != null) {
            mHideScriptAlarmsClock.setPersistent(false);
        }
        if (mHideRingingTaskClock != null) {
            mHideRingingTaskClock.setPersistent(false);
        }
        if (mLockPassword != null) {
            mLockPassword.setPersistent(false);
            mLockPassword.setOnPreferenceClickListener(preference -> {
                showLockPasswordDialog();
                return true;
            });
            refreshLockPasswordSummary();
        }
        if (mIconModePreference != null) {
            mIconModePreference.setPersistent(false);
        }
        if (mIconModeValue != null) {
            mIconModeValue.setPersistent(false);
        }
        if (mLanguage != null) {
            mLanguage.setPersistent(false);
            mLanguage.setEntries(LanguageHelper.getLanguageEntries(requireContext()));
            mLanguage.setEntryValues(LanguageHelper.getLanguageEntryValues());
        }

        if (mHideAppIcon != null) {
            mHideAppIcon.setChecked(hideAppIconEnabled);
        }
        if (mFloatBottomPreference != null) {
            mFloatBottomPreference.setChecked(isFloating);
        }
        if (mShowAllAlarms != null) {
            mShowAllAlarms.setChecked(showAllAlarms);
            mShowAllAlarms.setOnPreferenceChangeListener((preference, o) -> {
                final boolean value = Boolean.TRUE.equals(o);
                final Context app = requireContext().getApplicationContext();
                ThreadUtils.runOnBackground(() -> {
                    AppSettingsStore.setShowAllAlarmsEnabled(app, value);
                    // The alarm page is kept alive behind the settings page, so its own onResume may
                    // never run for this change.
                    com.hyperalarm.home.AlarmListFragment.refreshFiltersActive();
                });
                return true;
            });
        }
        if (mHideScriptAlarmsClock != null) {
            mHideScriptAlarmsClock.setChecked(hideScriptAlarmsClock);
            mHideScriptAlarmsClock.setOnPreferenceChangeListener((preference, o) -> {
                writeClockFilter(mHideScriptAlarmsClock, SettingsKeys.CLOCK_FILTER,
                        Boolean.TRUE.equals(o));
                return true;
            });
        }
        if (mHideRingingTaskClock != null) {
            mHideRingingTaskClock.setOnPreferenceChangeListener((preference, o) -> {
                writeClockFilter(mHideRingingTaskClock, SettingsKeys.CLOCK_FILTER_RINGING,
                        Boolean.TRUE.equals(o));
                return true;
            });
        }
        if (mIconModePreference != null) {
            mIconModePreference.setValueIndex(mIconMode);
        }
        if (mIconModeValue != null) {
            mIconModeValue.setValueIndex(iconModeStyle);
        }
        if (mLanguage != null) {
            mLanguage.setValueIndex(languageIndex);
        }

        setIconMode(mIconMode);
        mIconModePreference.setOnPreferenceChangeListener(this);

        mLanguage.setOnPreferenceChangeListener((preference, o) -> {
            int index = Integer.parseInt((String) o);
            LanguageHelper.setIndexLanguage(getActivity(), index, true);
            return true;
        });

        mIconModeValue.setOnPreferenceChangeListener((preference, newValue) -> {
            AppSettingsStore.setIconModeIndex(requireContext(), Integer.parseInt((String) newValue));
            return true;
        });

        // 根据构建类型设置日志等级选项
        setupLogLevelPreference();

        if (mHideAppIcon != null) {
            mHideAppIcon.setOnPreferenceChangeListener((preference, o) -> {
                boolean enabled = Boolean.TRUE.equals(o);
                AppSettingsStore.setHideAppIconEnabled(requireContext(), enabled);

                PackageManager pm = requireActivity().getPackageManager();
                int mComponentEnabledState;

                if (enabled) {
                    mComponentEnabledState = PackageManager.COMPONENT_ENABLED_STATE_ENABLED;
                } else {
                    mComponentEnabledState = PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
                }

                pm.setComponentEnabledSetting(new ComponentName(requireActivity(), LauncherActivity.class), mComponentEnabledState, PackageManager.DONT_KILL_APP);
                return true;
            });
        }

        mFloatBottomPreference.setOnPreferenceChangeListener((preference, newValue) -> {
            boolean enabled = Boolean.TRUE.equals(newValue);
            AppSettingsStore.setFloatNavEnabled(requireContext(), enabled);
            return true;
        });

        if (mHotReloadPreference != null) {
            mHotReloadPreference.setOnPreferenceClickListener(preference -> {
                showHotReloadEntry();
                return true;
            });
        }

        Preference backPreference = findPreference("prefs_key_back");
        Preference restPreference = findPreference("prefs_key_rest");
        Preference resetPreference = findPreference("prefs_key_reset");

        backPreference.setOnPreferenceClickListener(this);
        restPreference.setOnPreferenceClickListener(this);
        resetPreference.setOnPreferenceClickListener(this);
    }

    @Override
    public void onStart() {
        super.onStart();
        ScopeManager.addServiceStateListener(mServiceStateListener, true);
    }

    @Override
    public void onStop() {
        ScopeManager.removeServiceStateListener(mServiceStateListener);
        super.onStop();
    }

    @Override
    public boolean onPreferenceClick(@NonNull Preference preference) {
        switch (preference.getKey()) {
            case "prefs_key_back" -> mBackupLauncher.launch(BackupUtils.getCreateDocumentIntent());
            case "prefs_key_rest" -> mRestoreLauncher.launch(BackupUtils.getOpenDocumentIntent());
            case "prefs_key_reset" -> {
                DialogHelper.showDialog(
                    requireActivity(),
                    getString(com.hyperalarm.core.R.string.reset_title),
                    getString(com.hyperalarm.core.R.string.reset_desc),
                    (dialog, which) -> {
                        PrefsBridge.clearAllByApp();
                        AppSettingsStore.resetGlobalToDefaults(requireContext());
                        OobeUtils.resetOobeState(requireContext());
                        LanguageHelper.clearLanguage(requireContext());
                        Toast.makeText(getActivity(), com.hyperalarm.core.R.string.reset_okay, Toast.LENGTH_LONG).show();
                    }
                );
            }
        }
        return true;
    }

    private void updateHotReloadPreferenceVisibility() {
        if (mHotReloadPreference != null) {
            mHotReloadPreference.setVisible(HotReloadManager.isHotReloadAvailable());
        }
    }

    private void showHotReloadEntry() {
        // 开发入口与功能页使用同一套 scope 应用多选逻辑，避免再按单个 PID 重载。
        HotReloadDialogHelper.showScopedAppPicker(requireActivity(), null);
    }

    @Override
    public boolean onPreferenceChange(@NonNull Preference preference, Object o) {
        if (preference == mIconModePreference) {
            int index = Integer.parseInt((String) o);
            AppSettingsStore.setIconIndex(requireContext(), index);
            setIconMode(index);
        }
        return true;
    }

    /**
     * 日志等级统一为三档：
     * 0: 禁用日志输出
     * 1: 仅输出 error
     * 2: 输出全部日志
     * 设置页按构建类型裁剪展示：
     * release -> 0 / 1
     * others  -> 1 / 2
     */
    private void setupLogLevelPreference() {
        boolean isRelease = ProjectApi.isRelease();
        CharSequence[] entries = isRelease
            ? new CharSequence[]{
                getString(R.string.log_level_none),
                getString(R.string.log_level_general)
            }
            : new CharSequence[]{
                getString(R.string.log_level_general),
                getString(R.string.log_level_detailed)
            };
        CharSequence[] entryValues = isRelease
            ? new CharSequence[]{"0", "1"}
            : new CharSequence[]{"1", "2"};
        String defaultValue = String.valueOf(LogLevelManager.getDefaultLogLevel());

        mLogLevel.setEntries(entries);
        mLogLevel.setEntryValues(entryValues);
        mLogLevel.setDefaultValue(defaultValue);

        String currentValue = mLogLevel.getValue();
        if (currentValue != null) {
            try {
                currentValue = String.valueOf(LogLevelManager.getEffectiveLogLevel(Integer.parseInt(currentValue)));
            } catch (NumberFormatException e) {
                currentValue = defaultValue;
            }
        }
        boolean isValidValue = false;
        if (currentValue != null) {
            for (CharSequence value : entryValues) {
                if (value.toString().equals(currentValue)) {
                    isValidValue = true;
                    break;
                }
            }
        }
        if (!isValidValue) {
            mLogLevel.setValue(defaultValue);
        } else {
            mLogLevel.setValue(currentValue);
        }

        mLogLevel.setOnPreferenceChangeListener((preference, o) -> {
            return true;
        });
    }

    private void setIconMode(int mode) {
        mIconModeValue.setVisible(mode != 0);
    }

    @Override
    public void onContentInsetChanged(Rect rect) {
        super.onContentInsetChanged(rect);
        if (getListView() != null) {
            ViewUtils.RelativePadding relativePadding = new ViewUtils.RelativePadding(getListView());
            boolean isLayoutRtl = ViewUtils.isLayoutRtl(getListView());
            relativePadding.start += isLayoutRtl ? rect.right : rect.left;
            relativePadding.end += isLayoutRtl ? rect.left : rect.right;
            relativePadding.bottom = rect.top;
            relativePadding.applyToView(getListView());
        }
    }

    private void processBackup(Uri uri) {
        // ContentResolver writes and the per-key preference copy are both slow; the SAF callback runs
        // on the main thread and must not wait for them.
        final Context context = requireContext().getApplicationContext();
        ThreadUtils.runOnBackground(() -> {
            String failure = null;
            try {
                BackupUtils.handleCreateDocument(context, uri);
            } catch (Exception e) {
                AndroidLog.e(TAG, "backup failed", e);
                failure = getString(com.hyperalarm.core.R.string.backup_failed);
            }
            final String message = failure;
            ThreadUtils.postOnMainThread(() -> {
                if (!isAdded()) return;
                if (message == null) {
                    showDialog(getString(com.hyperalarm.core.R.string.backup_success), null);
                } else {
                    // The exception text is for the log, not for the dialog.
                    showDialog(message, getString(com.hyperalarm.core.R.string.backup_failed_detail));
                }
            });
        });
    }

    private void processRestore(Uri uri) {
        final Context context = requireContext().getApplicationContext();
        ThreadUtils.runOnBackground(() -> {
            boolean ok = true;
            try {
                BackupUtils.handleReadDocument(context, uri);
                AppSettingsStore.syncGlobalFromPrefs(context);
            } catch (Exception e) {
                AndroidLog.e(TAG, "restore failed", e);
                ok = false;
            }
            final boolean restored = ok;
            ThreadUtils.postOnMainThread(() -> {
                if (!isAdded()) return;
                // One scope, so a restore only brings back preferences: nothing to reconcile.
                if (restored) {
                    showDialog(
                        getString(com.hyperalarm.core.R.string.rest_success),
                        getString(com.hyperalarm.core.R.string.rest_success_message),
                        (dialog, which) -> restartApp()
                    );
                } else {
                    showDialog(getString(com.hyperalarm.core.R.string.rest_failed),
                            getString(com.hyperalarm.core.R.string.rest_failed_detail));
                }
            });
        });
    }

    private void showDialog(String title, String message) {
        showDialog(title, message, null);
    }

    private void showDialog(String title, String message, android.content.DialogInterface.OnClickListener onClickListener) {
        new AlertDialog.Builder(requireContext())
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(android.R.string.ok, onClickListener)
            .show();
    }

    private void restartApp() {
        Context context = requireContext().getApplicationContext();
        Intent restartIntent = new Intent(context, SplashActivity.class)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        PendingIntent pendingIntent = PendingIntent.getActivity(
            context,
            0,
            restartIntent,
            PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            alarmManager.set(AlarmManager.RTC, System.currentTimeMillis() + 200, pendingIntent);
        } else {
            context.startActivity(restartIntent);
        }

        Activity activity = getActivity();
        if (activity != null) {
            activity.finishAffinity();
        }
        android.os.Process.killProcess(android.os.Process.myPid());
    }

    /** Shows whether a lock screen password is configured for the silent tasks. */
    private void refreshLockPasswordSummary() {
        if (mLockPassword == null) return;
        ThreadUtils.runOnBackground(() -> {
            String stored = com.hyperalarm.common.utils.RootShell.readFile(
                    "/data/adb/alarm_shell/password").trim();
            boolean set = !stored.isEmpty();
            ThreadUtils.postOnMainThread(() -> {
                if (mLockPassword == null || !isAdded()) return;
                mLockPassword.setSummary(set
                        ? R.string.lock_password_summary_set
                        : R.string.lock_password_summary_unset);
            });
        });
    }

    /**
     * Asks for the lock screen password the silent tasks use to unlock.
     *
     * <p>The value goes straight into the root-only password file; it is never logged, shown or put
     * into a notification, and the dialog masks what is typed.</p>
     */
    private void showLockPasswordDialog() {
        // The dialog content is a layout rather than a bare EditText: the explanation line and the
        // rounded field are what makes it look like the rest of the app.
        android.view.View content = android.view.LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_lock_password, null, false);
        final android.widget.EditText input = content.findViewById(R.id.lock_password_input);

        new fan.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle(R.string.lock_password_dialog_title)
                .setView(content)
                .setPositiveButton(R.string.lock_password_save, (d, w) -> {
                    String typed = input.getText().toString();
                    // "Leave empty to keep it" is what the hint promises: an empty save must not
                    // wipe the password. Clearing is its own button.
                    if (typed.isEmpty()) {
                        Toast.makeText(requireContext(), R.string.lock_password_unchanged,
                                Toast.LENGTH_SHORT).show();
                        return;
                    }
                    writeLockPassword(typed);
                })
                .setNeutralButton(R.string.lock_password_clear, (d, w) -> writeLockPassword(""))
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }

    /** Writes (or clears, when blank) the password file and refreshes the summary. */
    private void writeLockPassword(String password) {
        final String value = password == null ? "" : password;
        ThreadUtils.runOnBackground(() -> {
            // printf keeps the value out of the command line only for the shell builtin; the file is
            // root-only and rewritten atomically enough for a single small string.
            boolean ok = com.hyperalarm.common.utils.RootShell.succeeded(
                    "mkdir -p " + com.hyperalarm.common.utils.RootShell.quote("/data/adb/alarm_shell")
                            + " && printf '%s' " + com.hyperalarm.common.utils.RootShell.quote(value)
                            + " > " + com.hyperalarm.common.utils.RootShell.quote("/data/adb/alarm_shell/password")
                            + " && chmod 600 " + com.hyperalarm.common.utils.RootShell.quote("/data/adb/alarm_shell/password"));
            ThreadUtils.postOnMainThread(() -> {
                if (!isAdded()) return;
                Toast.makeText(requireContext(),
                        ok ? (value.isEmpty() ? R.string.lock_password_cleared : R.string.lock_password_saved)
                           : R.string.root_required,
                        Toast.LENGTH_SHORT).show();
                refreshLockPasswordSummary();
            });
        });
    }

    /**
     * Writes one of the clock list filters to the system global.
     *
     * <p>The switch is the global, so there is no local copy to fall back on: the write runs in the
     * background and the switch is put back when it fails (no root, or a denied shell). Checking root
     * before flipping would block the UI thread on the authorisation prompt.</p>
     */
    private void writeClockFilter(SwitchPreference preference, String key, boolean value) {
        final Context context = requireContext().getApplicationContext();
        ThreadUtils.runOnBackground(() -> {
            boolean ok = RootShell.succeeded("settings put global " + key + " " + (value ? "1" : "0"));
            ThreadUtils.postOnMainThread(() -> {
                if (!isAdded()) return;
                // Repaint from the stored value either way: the switch does not follow the async
                // write on its own, and on failure it has to be put back.
                preference.setChecked(ok ? value : !value);
                if (!ok) {
                    Toast.makeText(requireContext(), R.string.clock_filter_no_root,
                            Toast.LENGTH_LONG).show();
                }
            });
        });
    }

    /** True when the clock is told to hide ringing tasks ({@code @x.sh}) as well. */
    private static boolean isClockRingingTaskHidden(Context context) {
        try {
            return Settings.Global.getInt(context.getContentResolver(),
                    SettingsKeys.CLOCK_FILTER_RINGING, 0) == 1;
        } catch (Throwable t) {
            return false;
        }
    }

    /** True when the clock is told to hide script alarms from its own list. */
    private static boolean isClockScriptFilterEnabled(Context context) {
        try {
            return Settings.Global.getInt(context.getContentResolver(), SettingsKeys.CLOCK_FILTER, 0) == 1;
        } catch (Throwable t) {
            return false;
        }
    }
}
