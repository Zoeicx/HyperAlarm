package com.hyperalarm.home;

import android.content.Context;
import android.content.res.Configuration;

import com.hyperalarm.R;

import java.text.DateFormatSymbols;
import java.util.Calendar;
import java.util.Locale;

/**
 * Turns the clock's {@code daysofweek} bit mask into the text shown in the alarm list.
 *
 * <p>Bit 0 is Monday. The special masks are the ones Mi Clock itself uses: 127 is "every day",
 * 128/256 are the calendar-based workday/holiday sets and 512 is a shift schedule. Weekday names
 * come from the platform, so they follow the app language without a translation table here.</p>
 */
public final class AlarmRepeatText {
    private AlarmRepeatText() {
    }

    public static String get(Context context, int days) {
        if (days == 0) return context.getString(R.string.repeat_once);
        if (days == 127 || days == 255) return context.getString(R.string.repeat_every_day);
        if (days == 128) return context.getString(R.string.repeat_workdays);
        if (days == 256) return context.getString(R.string.repeat_holidays);
        if (days == 512) return context.getString(R.string.repeat_shift);
        if (days == 31 || days == 62) return context.getString(R.string.repeat_mon_fri);
        if (days == 96) return context.getString(R.string.repeat_weekend);
        return weekdays(context, days);
    }

    public static String weekdays(Context context, int days) {
        String[] names = shortWeekdayNames(context);
        StringBuilder sb = new StringBuilder();
        for (int bit = 0; bit < 7; bit++) {
            if ((days & (1 << bit)) == 0) continue;
            // bit 0 is Monday; DateFormatSymbols indexes Sunday first (1..7).
            int calendarDay = bit == 6 ? Calendar.SUNDAY : bit + Calendar.MONDAY;
            if (sb.length() > 0) sb.append(' ');
            sb.append(names[calendarDay]);
        }
        return sb.length() == 0 ? context.getString(R.string.repeat_once) : sb.toString();
    }

    /** Monday-first short weekday names, for pickers that let the user tick days. */
    public static String[] weekdayOptions(Context context) {
        String[] all = shortWeekdayNames(context);
        String[] mondayFirst = new String[7];
        for (int bit = 0; bit < 7; bit++) {
            int calendarDay = bit == 6 ? Calendar.SUNDAY : bit + Calendar.MONDAY;
            mondayFirst[bit] = all[calendarDay];
        }
        return mondayFirst;
    }

    private static String[] shortWeekdayNames(Context context) {
        Configuration configuration = context.getResources().getConfiguration();
        Locale locale = configuration.getLocales().isEmpty()
                ? Locale.getDefault() : configuration.getLocales().get(0);
        return DateFormatSymbols.getInstance(locale).getShortWeekdays();
    }

    public static int fromCalendarDay(int calendarDay) {
        // Calendar.SUNDAY=1 ... SATURDAY=7; bit 0 is Monday.
        return 1 << ((calendarDay + 5) % 7);
    }

    public static boolean[] booleanDays(int days) {
        boolean[] result = new boolean[7];
        for (int i = 0; i < 7; i++) result[i] = (days & (1 << i)) != 0;
        return result;
    }

    public static int fromBooleans(boolean[] checked) {
        int days = 0;
        for (int i = 0; i < checked.length && i < 7; i++) {
            if (checked[i]) days |= 1 << i;
        }
        return days;
    }
}
