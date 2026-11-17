package com.hyperalarm.home;

import android.content.Context;

import com.hyperalarm.R;
import com.hyperalarm.common.alarm.AlarmItem;

/**
 * Formats the "N days · M shifts" label shown instead of the repeat text for a shift row.
 *
 * <p>The numbers come from the row itself, so the label is rendered by the app in the app language
 * instead of travelling through the bridge as a pre-formatted string.</p>
 */
final class ShiftSummary {
    private ShiftSummary() {
    }

    static String of(Context context, AlarmItem item) {
        if (context == null || item == null || !item.isShift()) return "";
        int shifts = Math.max(item.shiftAlarmCount,
                item.shiftHours == null ? 0 : item.shiftHours.length);
        if (item.shiftDuration <= 0 || shifts <= 0) return "";
        return context.getString(R.string.shift_info, item.shiftDuration, shifts);
    }
}
