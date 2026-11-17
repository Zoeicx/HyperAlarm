package com.hyperalarm.home;

import android.content.Context;
import android.net.Uri;

import com.hyperalarm.R;

/**
 * Maps the clock's internal ringtone URI titles to the names shown in the alarm sheet.
 *
 * <p>The clock stores a {@code title} query parameter for its built-in sounds (elemental/weather
 * dynamic rings, morning dew, fireflies, ...). Those are the clock's own branded names, which this
 * app cannot read, so the known ones are mapped here and anything else falls back to a generic
 * label instead of leaking an internal English identifier into the UI.</p>
 */
final class RingtoneNameMapper {
    private RingtoneNameMapper() {
    }

    static String display(Context context, String raw) {
        if (raw == null || raw.isEmpty()) return context.getString(R.string.ringtone_default);
        if ("silent".equals(raw)) return context.getString(R.string.ringtone_silent);

        String title = "";
        try {
            title = Uri.parse(raw).getQueryParameter("title");
        } catch (Throwable ignored) {
        }
        String key = title == null ? "" : title.toLowerCase(java.util.Locale.ROOT);
        if (key.contains("week")) return context.getString(R.string.ringtone_element);
        if (key.contains("weather")) return context.getString(R.string.ringtone_weather);
        if (key.contains("dew")) return context.getString(R.string.ringtone_morning_dew);
        if (key.contains("firefly") || key.contains("fireflies")) return context.getString(R.string.ringtone_fireflies);
        if (key.contains("dream")) return context.getString(R.string.ringtone_daydream);
        if (key.contains("digital") || key.contains("timer")) return context.getString(R.string.ringtone_digital_timer);
        if (key.contains("star")) return context.getString(R.string.ringtone_starry_sky);
        if (key.contains("music")) return context.getString(R.string.ringtone_music);
        if (key.contains("record")) return context.getString(R.string.ringtone_recording);
        // Avoid exposing an internal English identifier to the UI.
        if (title != null && !title.isEmpty() && !title.contains("_")) return title;
        if (raw.contains("settings/system/alarm_alert")) return context.getString(R.string.ringtone_default);
        return context.getString(R.string.ringtone_custom);
    }
}
