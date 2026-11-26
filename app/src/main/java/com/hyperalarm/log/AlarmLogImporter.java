package com.hyperalarm.log;

import android.content.Context;
import android.content.SharedPreferences;

import com.hyperalarm.common.log.ModuleLog;
import com.hyperalarm.common.utils.RootShell;
import com.hyperalarm.log.db.LogEntry;
import com.hyperalarm.log.db.LogRepository;

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
    /** Set once the mirror tree has been handed back to this app's uid. */
    private static final String KEY_MIRROR_OWNER_FIXED = "mirror_owner_fixed_v2";
    /** A failed root call is retried on the next app process, not on every page refresh. */
    private static final java.util.concurrent.atomic.AtomicBoolean OWNER_REPAIR_TRIED =
            new java.util.concurrent.atomic.AtomicBoolean(false);
    /** One extra repair per process for directories recreated by an old clock process. */
    private static final java.util.concurrent.atomic.AtomicBoolean FORCED_OWNER_REPAIR_TRIED =
            new java.util.concurrent.atomic.AtomicBoolean(false);
    private static final String TAG = "AlarmLogImporter";

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
            List<String> files = new java.util.ArrayList<>();
            File external = context.getExternalFilesDir(null);
            if (external == null) return; // external storage unavailable; nothing can be read
            File mirror = new File(external, "script_logs");
            boolean unreadable = collectFiles(mirror, files);
            boolean repairedOwner = false;
            if (unreadable) {
                // A clock process still running the old module can recreate root-owned directories
                // after the one-time repair flag was set. Repair again when listing actually failed.
                repairedOwner = repairMirrorOwner(prefs, mirror, true);
                if (repairedOwner) {
                    files.clear();
                    collectFiles(mirror, files);
                }
            } else {
                repairedOwner = repairMirrorOwner(prefs, mirror, false);
            }
            // Files that were unreadable when they were skipped may now be readable. Rescan from the
            // beginning; the unique index and insertImportedLogs(IGNORE) keep already stored runs out.
            if (repairedOwner) prefs.edit().remove(KEY_LAST_TS).apply();
            long last = repairedOwner ? 0L : prefs.getLong(KEY_LAST_TS, 0L);
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
                if (known.contains(key) || IMPORTED.contains(key)) {
                    // Already stored: advance the bookmark too. Without this a repair rescan (or a
                    // restored database) left last_ts at 0 and walked every old file on every refresh.
                    if (ts > newest) newest = ts;
                    continue;
                }
                try {
                    // The key is remembered only once the row is really in the database: adding it
                    // first means a failed insert silently drops that run for good.
                    // IGNORE, not ABORT: a repair rescan walks already imported files again, and
                    // the unique (module, timestamp, message) index is the real duplicate guard.
                    LogRepository.getInstance().getDao().insertImportedLogs(
                            Collections.singletonList(new LogEntry("Script", level, "AlarmTask",
                                    body, ts, LogEntry.SOURCE_GROUP_CURRENT, "")));
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

    /**
     * Hands mirrors written by older builds back to this app.
     *
     * <p>Older code chowned the mirror to the owner of
     * {@code /sdcard/Android/data/<pkg>/files}. When root's {@code mkdir -p} created that directory
     * before the app did, the owner was root, every mirror stayed mode 700 root, and the importer
     * could not read a single run. The app process already knows its own uid, so a one-time
     * recursive chown repairs those old directories without needing to guess from the external
     * directory.</p>
     */
    private static boolean repairMirrorOwner(SharedPreferences prefs, File mirror, boolean force) {
        if (!force && prefs.getBoolean(KEY_MIRROR_OWNER_FIXED, false)) return false;
        if (mirror == null) {
            prefs.edit().putBoolean(KEY_MIRROR_OWNER_FIXED, true).apply();
            return false;
        }
        if (force) {
            if (!FORCED_OWNER_REPAIR_TRIED.compareAndSet(false, true)) return false;
        } else if (!OWNER_REPAIR_TRIED.compareAndSet(false, true)) {
            return false;
        }
        int uid = android.os.Process.myUid();
        // The app may not even be able to stat script_logs when files itself is root-owned. Hand
        // the parent back first, then recursively fix the mirror if it is visible now.
        File parent = mirror.getParentFile();
        boolean fixed = parent == null || RootShell.succeeded(
                "chown " + uid + ":" + uid + " " + RootShell.quote(parent.getAbsolutePath()));
        if (fixed && mirror.exists()) {
            fixed = RootShell.succeeded("chown -R " + uid + ":" + uid + " "
                    + RootShell.quote(mirror.getAbsolutePath()));
        }
        if (fixed) {
            prefs.edit().putBoolean(KEY_MIRROR_OWNER_FIXED, true).apply();
            return true;
        }
        ModuleLog.w(TAG, "could not hand the script log mirror back to uid " + uid);
        return false;
    }

    /** @return true when some directory under {@code dir} could not be listed */
    private static boolean collectFiles(File dir, List<String> out) {
        if (dir == null || !dir.exists()) return false;
        File[] files = dir.listFiles();
        if (files == null) {
            // A directory we may not read used to look exactly like an empty one, which is how the
            // root-owned mirror stayed invisible.
            ModuleLog.w(TAG, "cannot list " + dir.getAbsolutePath());
            return true;
        }
        boolean unreadable = false;
        for (File file : files) {
            if (file.isDirectory()) {
                unreadable |= collectFiles(file, out);
            } else {
                out.add(file.getAbsolutePath());
            }
        }
        return unreadable;
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
            ModuleLog.w(TAG, "cannot read " + path, t);
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
