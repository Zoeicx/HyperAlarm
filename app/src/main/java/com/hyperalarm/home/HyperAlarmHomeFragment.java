package com.hyperalarm.home;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.hyperalarm.R;
import com.hyperalarm.common.utils.AppSettingsStore;
import com.hyperalarm.common.utils.RootShell;
import com.hyperalarm.common.alarm.BridgeContract;

import fan.appcompat.app.AlertDialog;
import fan.appcompat.app.Fragment;
import com.hyperalarm.common.utils.ThreadUtils;

public class HyperAlarmHomeFragment extends Fragment {

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setThemeRes(R.style.NavigatorContentTheme);
        AlarmListFragment.preload(requireContext().getApplicationContext());
    }

    @Override
    public View onInflateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                              @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewInflated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewInflated(view, savedInstanceState);
        getActionBar().setTitle("HyperAlarm");
        restoreActionBar();
        getChildFragmentManager().beginTransaction()
                .replace(R.id.prefs_container, new AlarmListFragment())
                .commit();


    }

    public void restoreActionBar() {
        if (getActivity() instanceof com.hyperalarm.ui.HomePageActivity) {
            ((com.hyperalarm.ui.HomePageActivity) getActivity()).hideSelectionTrash();
        }
        if (getActionBar() == null) return;
        LinearLayout actions = new LinearLayout(requireContext());
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setGravity(android.view.Gravity.CENTER_VERTICAL);
        ImageView add = new ImageView(requireContext());
        add.setImageResource(R.drawable.ic_alarm_add);
        add.setImageTintList(ColorStateList.valueOf(Color.WHITE));
        add.setPadding(18, 12, 18, 12);
        add.setContentDescription(requireContext().getString(R.string.alarm_add));
        add.setClickable(true);
        add.setFocusable(true);
        add.setOnClickListener(v -> addAlarm());
        ImageView restart = new ImageView(requireContext());
        restart.setImageResource(com.hyperalarm.core.R.drawable.ic_reboot_small);
        restart.setImageTintList(ColorStateList.valueOf(Color.WHITE));
        restart.setPadding(18, 12, 18, 12);
        restart.setContentDescription(requireContext().getString(R.string.clock_restart));
        restart.setOnClickListener(v -> confirmStop());
        actions.addView(add);
        actions.addView(restart);
        getActionBar().setEndView(actions);
    }

    public void showSelectionActionBar(int count, Runnable delete, Runnable cancel) {
        if (AppSettingsStore.isFloatNavEnabled(requireContext())) {
            if (getActionBar() != null) getActionBar().setEndView(null);
            if (getActivity() instanceof com.hyperalarm.ui.HomePageActivity) {
                ((com.hyperalarm.ui.HomePageActivity) getActivity()).showSelectionTrash(delete);
            }
            return;
        }
        if (getActionBar() == null) return;
        ImageView deleteView = new ImageView(requireContext());
        deleteView.setImageResource(R.drawable.ic_alarm_delete);
        deleteView.setImageTintList(ColorStateList.valueOf(Color.WHITE));
        deleteView.setPadding(18, 12, 18, 12);
        deleteView.setContentDescription(requireContext().getString(R.string.action_delete));
        deleteView.setOnClickListener(v -> delete.run());
        getActionBar().setEndView(deleteView);
    }

    private void addAlarm() {
        final Context app = requireContext().getApplicationContext();
        ThreadUtils.runOnBackground(() -> {
            ClockBridge.Reply reply = ClockBridge.call(app, BridgeContract.ACTION_ADD_ALARM, null);
            int newId = 0;
            try {
                newId = Integer.parseInt(reply.data);
            } catch (Throwable ignored) {
            }
            final int created = reply.ok() ? newId : 0;
            Activity activity = getActivity();
            if (!isAdded() || activity == null) return;
            activity.runOnUiThread(() -> {
                if (!isAdded()) return;
                if (created <= 0) {
                    Toast.makeText(requireContext(), R.string.alarm_add_failed, Toast.LENGTH_LONG).show();
                    return;
                }
                AlarmDetailBottomSheet sheet = new AlarmDetailBottomSheet(requireActivity());
                sheet.setNewAlarm(true);
                sheet.showAlarm(created, 8, 0, "", 0, "", 0, 0, 0, 0,
                        null, null, null, null);
            });
        });
    }

    private void confirmStop() {
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.clock_restart)
                .setMessage(R.string.clock_restart_desc)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok, (d, w) -> {
                    final Context app = requireContext().getApplicationContext();
                    ThreadUtils.runOnBackground(() -> {
                        // probeRootNow() rather than isRoot(): the non-blocking answer is still "not
                        // yet" for a moment after a cold start, and the user pressing this right after
                        // opening the app used to get a "root required" toast on a rooted device.
                        if (!RootShell.probeRootNow()) {
                            ThreadUtils.postOnMainThread(() -> {
                                if (isAdded()) {
                                    Toast.makeText(requireContext(), R.string.root_required, Toast.LENGTH_LONG).show();
                                }
                            });
                            return;
                        }
                        RootShell.run("am force-stop com.android.deskclock; sleep 1; "
                                + "am startservice -n com.android.deskclock/com.xiaomi.smarthome.ClockService");
                    });
                })
                .show();
    }
}
