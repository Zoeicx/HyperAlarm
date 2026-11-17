package com.hyperalarm.log;

import android.content.Context;
import android.content.SharedPreferences;

import com.hyperalarm.common.log.ModuleLog;
import com.hyperalarm.common.utils.RootShell;
import com.hyperalarm.log.db.LogEntry;
import com.hyperalarm.log.db.LogRepository;

import com.topjohnwu.superuser.Shell;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The script runner writes logs in the clock process cache. The HyperAlarm log
 * page reads the app database, so import new clock log files before querying.
 */
final class AlarmLogImporter {
    private static final String PREFS = "hyperalarm_alarm_log_import";
    private static final String KEY_LAST_TS = "last_ts";
    /** Set once the rows that older builds imported twice have been collapsed. */
    private static final String KEY_DEDUPED = "deduped_v1";
    private static final String TAG = "AlarmLogImporter";
    private static final String LOG_ROOT = "/data/data/com.android.deskclock/cache/log";

    /**
     * The log page imports on every refresh, and a refresh can start while the previous import is
     * still reading the same directory. Both passes then saw the same {@code last_ts}, imported the
     * same files and inserted every row twice, which is what the duplicate log entries came from.
     * One lock for the whole pass makes the import serial, and the keys of this process' own
     * insertions are remembered so a file cannot be imported again even if the timestamp bookkeeping
     * is ever lost.
     */
    private static final Object IMPORT_LOCK = new Object();
    private static final Set<String> IMPORTED = new HashSet<>();

    private AlarmLogImporter() {
    }

    static void importNew(Context context) {
        if (context == null) return;
        synchronized (IMPORT_LOCK) {
            importNewLocked(context);
        }
    }

    private static void importNewLocked(Context context) {
        try {
            SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            long last = prefs.getLong(KEY_LAST_TS, 0L);
            List<String> files = new java.util.ArrayList<>();
            File mirror = new File(context.getExternalFilesDir(null), "script_logs");
            collectFiles(mirror, files);
            // Order by the timestamp in the name, not by path. Paths sort per script directory, so a
            // newer run of an alphabetically earlier script advanced the single last_ts past every
            // older, still unimported run of the others - those were then skipped for good.
            Collections.sort(files, (left, right) -> Long.compare(parseTimestamp(left), parseTimestamp(right)));
            Set<String> known = new HashSet<>();
            String NL = String.valueOf((char) 10);
            for (com.hyperalarm.log.db.LogEntry entry :
                    LogRepository.getInstance().getDao().queryLogs("Script", "ALL", "AlarmTask", "")) {
                known.add(entry.getTimestamp() + "|" + entry.getMessage());
            }
            long newest = last;
            final long now = System.currentTimeMillis();
            for (String path : files) {
                if (path == null || path.trim().isEmpty()) continue;
                path = path.trim();
                long ts = parseTimestamp(path);
                if (ts <= last) continue;
                // A file name is the run's timestamp, and a file from the future would move
                // last_ts forward permanently: every run after it would then look older and be
                // skipped. The clock cannot write such a name, so treat it as garbage.
                if (ts > now) continue;
                String text = readFile(path);
                if (text.isEmpty()) continue;
                String level = "I";
                String script = "";
                String code = "";
                String stdout = "";
                String stderr = "";
                int outStart = text.indexOf("--- stdout ---");
                int errStart = text.indexOf("--- stderr ---");
                if (outStart >= 0) {
                    int from = outStart + "--- stdout ---".length();
                    stdout = errStart > from ? text.substring(from, errStart).trim() : text.substring(from).trim();
                }
                if (errStart >= 0) {
                    stderr = text.substring(errStart + "--- stderr ---".length()).trim();
                }
                for (String line : text.split(NL, -1)) {
                    if (line.startsWith("script: ")) {
                        script = line.substring("script: ".length());
                    } else if (line.startsWith("exitCode: ")) {
                        code = line.substring("exitCode: ".length());
                        level = "0".equals(code) ? "I" : "E";
                    }
                }
                String title = script.isEmpty() ? path : script;
                String output;
                if (outStart >= 0 || errStart >= 0) {
                    output = stdout.isEmpty() ? stderr : (stdout + (stderr.isEmpty() ? "" : NL + stderr));
                } else {
                    output = text;
                }
                String body = title + (code.isEmpty() ? "" : "  exit=" + code)
                        + NL + "--- output ---" + NL + output;
                String key = ts + "|" + body;
                if (known.contains(key) || IMPORTED.contains(key)) continue;
                try {
                    // The key is remembered only once the row is really in the database: adding it
                    // first means a failed insert silently drops that run for good.
                    LogRepository.getInstance().getDao().insert(
                            new LogEntry("Script", level, "AlarmTask", body, ts,
                                    LogEntry.SOURCE_GROUP_CURRENT, ""));
                } catch (Throwable t) {
                    ModuleLog.w(TAG, "import failed for " + path, t);
                    continue;
                }
                known.add(key);
                IMPORTED.add(key);
                // Advance per file rather than at the end of the pass: a pass that dies halfway
                // then at worst repeats the file it was working on, and one already imported file
                // can never be picked up again by the next pass.
                if (ts > newest) {
                    newest = ts;
                    prefs.edit().putLong(KEY_LAST_TS, newest).apply();
                }
            }
            if (newest > last) prefs.edit().putLong(KEY_LAST_TS, newest).apply();
            if (!prefs.getBoolean(KEY_DEDUPED, false)) {
                LogRepository.getInstance().getDao().deleteDuplicateScriptLogs();
                prefs.edit().putBoolean(KEY_DEDUPED, true).apply();
            }
        } catch (Throwable t) {
            ModuleLog.w(TAG, "script log import failed", t);
        }
    }

    private static void collectFiles(File dir, List<String> out) {
        if (dir == null || !dir.exists()) return;
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File file : files) {
            if (file.isDirectory()) collectFiles(file, out);
            else out.add(file.getAbsolutePath());
        }
    }

    /** A run that printed more than this is truncated: the page is for reading, not for log dumps. */
    private static final int MAX_IMPORT_CHARS = 256 * 1024;

    private static String readFile(String path) {
        try (BufferedReader r = new BufferedReader(new InputStreamReader(
                new FileInputStream(path), StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = r.readLine()) != null) {
                if (sb.length() > 0) sb.append((char) 10);
                sb.append(line);
                if (sb.length() >= MAX_IMPORT_CHARS) {
                    sb.append((char) 10).append("… (output truncated)");
                    break;
                }
            }
            return sb.toString();
        } catch (Throwable t) {
            return "";
        }
    }

    private static long parseTimestamp(String path) {
        try {
            String name = path.substring(path.lastIndexOf('/') + 1);
            if (name.endsWith(".log")) name = name.substring(0, name.length() - 4);
            return new java.text.SimpleDateFormat("yyyyMMddHHmmss", java.util.Locale.US)
                    .parse(name).getTime();
        } catch (Throwable t) {
            return 0L;
        }
    }
}
