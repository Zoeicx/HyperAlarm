package com.hyperalarm.home;

import android.content.Context;
import android.content.Intent;

import com.hyperalarm.common.alarm.BridgeContract;
import com.hyperalarm.common.utils.ThreadUtils;

/**
 * Alarm writes, all of them through {@link ClockBridge}.
 *
 * <p>The clock process owns the alarm tables and keeps its own cache in front of them, so a write
 * has to happen inside that process: touching the provider from here would leave the clock showing
 * stale rows and never reschedule the alarm.</p>
 *
 * <p>Two flavours: the {@code async} helpers post the write and forget the answer (used where a
 * list reload follows anyway), and the {@code *Now} methods run on the caller's thread and return
 * the clock's acknowledgement. A caller that tells the user "saved" has to use the second kind -
 * a rejected write followed by a success toast is how the module used to look broken.</p>
 */
final class AlarmHookWriter {
    private AlarmHookWriter() {
    }

    static void update(Context context, int id, int hour, int minute, int days,
                       String label, boolean shift) {
        write(context, BridgeContract.ACTION_UPDATE_ALARM, intent -> intent
                .putExtra(BridgeContract.EXTRA_ID, id)
                .putExtra(BridgeContract.EXTRA_HOUR, hour)
                .putExtra(BridgeContract.EXTRA_MINUTE, minute)
                .putExtra(BridgeContract.EXTRA_DAYS, days)
                .putExtra(BridgeContract.EXTRA_LABEL, label)
                .putExtra(BridgeContract.EXTRA_SHIFT, shift));
    }

    static void delete(Context context, int id, boolean shift) {
        write(context, BridgeContract.ACTION_DELETE_ALARM, intent -> intent
                .putExtra(BridgeContract.EXTRA_ID, id)
                .putExtra(BridgeContract.EXTRA_SHIFT, shift));
    }

    static void setRingtone(Context context, int id, String uri, boolean shift) {
        write(context, BridgeContract.ACTION_SET_RINGTONE, intent -> intent
                .putExtra(BridgeContract.EXTRA_ID, id)
                .putExtra(BridgeContract.EXTRA_RINGTONE, uri)
                .putExtra(BridgeContract.EXTRA_SHIFT, shift));
    }

    static void setEnabled(Context context, int id, boolean enabled, boolean shift) {
        write(context, BridgeContract.ACTION_SET_ENABLED, intent -> intent
                .putExtra(BridgeContract.EXTRA_ID, id)
                .putExtra(BridgeContract.EXTRA_ENABLED, enabled)
                .putExtra(BridgeContract.EXTRA_SHIFT, shift));
    }

    /** Runs on a background thread: the bridge call blocks for its timeout. */
    private static void write(Context context, String action, ClockBridge.Filler filler) {
        if (context == null) return;
        Context app = context.getApplicationContext();
        ThreadUtils.runOnBackground(() -> ClockBridge.write(app, action, filler));
    }

    /** Blocks through the bridge timeout; call from a background thread. */
    static boolean updateNow(Context context, int id, int hour, int minute, int days,
                             String label, boolean shift) {
        return callNow(context, BridgeContract.ACTION_UPDATE_ALARM, intent -> intent
                .putExtra(BridgeContract.EXTRA_ID, id)
                .putExtra(BridgeContract.EXTRA_HOUR, hour)
                .putExtra(BridgeContract.EXTRA_MINUTE, minute)
                .putExtra(BridgeContract.EXTRA_DAYS, days)
                .putExtra(BridgeContract.EXTRA_LABEL, label)
                .putExtra(BridgeContract.EXTRA_SHIFT, shift));
    }

    /** Blocks through the bridge timeout; call from a background thread. */
    static boolean setRingtoneNow(Context context, int id, String uri, boolean shift) {
        return callNow(context, BridgeContract.ACTION_SET_RINGTONE, intent -> intent
                .putExtra(BridgeContract.EXTRA_ID, id)
                .putExtra(BridgeContract.EXTRA_RINGTONE, uri)
                .putExtra(BridgeContract.EXTRA_SHIFT, shift));
    }

    /** Blocks through the bridge timeout; call from a background thread. */
    static boolean deleteNow(Context context, int id, boolean shift) {
        return callNow(context, BridgeContract.ACTION_DELETE_ALARM, intent -> intent
                .putExtra(BridgeContract.EXTRA_ID, id)
                .putExtra(BridgeContract.EXTRA_SHIFT, shift));
    }

    private static boolean callNow(Context context, String action, ClockBridge.Filler filler) {
        if (context == null) return false;
        return ClockBridge.write(context.getApplicationContext(), action, filler);
    }
}
