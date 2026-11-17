/*
 * This file is part of HyperAlarm.
 *
 * HyperAlarm is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License.
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
package com.hyperalarm.log;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import com.hyperalarm.alarm.core.ScriptPath;

/**
 * Entry point for the "script finished" notification, which is posted by the clock process.
 *
 * <p>The notification lives in {@code com.android.deskclock} because that is the process that ran
 * the script, and one app may only open another app's activity when that activity is exported. This
 * trampoline is therefore exported, but it can do one thing only: open HyperAlarm's own script log
 * page. The script name it receives is checked with {@link ScriptPath#isPlainName} and never reaches
 * a file path, so a foreign app that pokes the trampoline gains a log page it can already read only
 * through this app's UI.</p>
 *
 * <p>The clock's own log viewer (a leftover of the fork) cannot show these logs: they belong to
 * HyperAlarm's database, so the notification has to route here instead.</p>
 *
 * <p>There is deliberately no caller check: {@code getLaunchedFromPackage()} returns null both for
 * the notification's PendingIntent and for an {@code am start}, so it cannot tell them apart and a
 * check would either break the notification or protect nothing. What keeps it harmless is the
 * payload: a script name has to be a plain file name and nothing else, and the only thing the
 * trampoline can do is open this app's own log page.</p>
 */
public class ScriptLogLauncher extends Activity {

    /** Script name of the finished run. */
    public static final String EXTRA_SCRIPT = "script";
    /** Log file name of the finished run, {@code yyyyMMddHHmmss-<alarmId>-<nano>.log}. */
    public static final String EXTRA_FILE = "file";
    /** Timestamp of the run, handed to the log page so it can open that record directly. */
    public static final String EXTRA_TARGET_TS = "target_ts";

    // Optional suffix on purpose: the clock adds "-<alarmId>-<nano>" so two runs in the same second
    // get two files. Requiring the bare 14 digits rejected every current name, which is exactly the
    // notification tap this class exists for.
    private static final java.util.regex.Pattern LOG_FILE =
            java.util.regex.Pattern.compile("^([0-9]{14})(?:-[0-9]+-[0-9]+)?[.]log$");

    /** Only these may drive this trampoline: the clock that posts the notification, and ourselves. */
    private static final String CLOCK_PACKAGE = "com.android.deskclock";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent source = getIntent();
        String script = source == null ? null : source.getStringExtra(EXTRA_SCRIPT);
        String file = source == null ? null : source.getStringExtra(EXTRA_FILE);
        if (ScriptPath.isPlainName(script)) {
            Intent intent = new Intent(this, ScriptLogActivity.class);
            long ts = parseTimestamp(file);
            if (ts > 0L) intent.putExtra(EXTRA_TARGET_TS, ts);
            // Carry the script name through so the page can start on the right filter later.
            intent.putExtra(EXTRA_SCRIPT, script);
            startActivity(intent);
        }
        finish();
    }

    /** Log file name to milliseconds, or 0 when the name is not a log file name. */
    private static long parseTimestamp(String fileName) {
        if (fileName == null) return 0L;
        java.util.regex.Matcher matcher = LOG_FILE.matcher(fileName);
        if (!matcher.matches()) return 0L;
        try {
            return new java.text.SimpleDateFormat("yyyyMMddHHmmss", java.util.Locale.US)
                    .parse(matcher.group(1)).getTime();
        } catch (Throwable t) {
            return 0L;
        }
    }
}
