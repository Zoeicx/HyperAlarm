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
package com.hyperalarm.log.db;

import androidx.room.Database;
import androidx.room.RoomDatabase;

@Database(entities = {LogEntry.class}, version = 5, exportSchema = true)
public abstract class LogDatabase extends RoomDatabase {
    public abstract LogDao logDao();
    public static final androidx.room.migration.Migration DIAGNOSTICS_MIGRATION =
        new androidx.room.migration.Migration(2,3) {
            @Override public void migrate(androidx.sqlite.db.SupportSQLiteDatabase db) {
                db.execSQL("CREATE TABLE IF NOT EXISTS hook_diagnostics (fingerprint TEXT NOT NULL PRIMARY KEY, timestamp INTEGER NOT NULL, report TEXT NOT NULL)");
            }
        };
    /** The launcher diagnostics page is gone, so its table goes with it. */
    public static final androidx.room.migration.Migration DROP_DIAGNOSTICS_MIGRATION =
        new androidx.room.migration.Migration(3,4) {
            @Override public void migrate(androidx.sqlite.db.SupportSQLiteDatabase db) {
                db.execSQL("DROP TABLE IF EXISTS hook_diagnostics");
            }
        };
    /**
     * Adds the unique index the importers need. SQLite cannot add one to a table that already holds
     * duplicates, and the importers produced plenty of them, so the duplicates are dropped first.
     */
    public static final androidx.room.migration.Migration UNIQUE_INDEX_MIGRATION =
        new androidx.room.migration.Migration(4,5) {
            @Override public void migrate(androidx.sqlite.db.SupportSQLiteDatabase db) {
                db.execSQL("DELETE FROM logs WHERE id NOT IN "
                        + "(SELECT MIN(id) FROM logs GROUP BY module, timestamp, message)");
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS "
                        + "index_logs_module_timestamp_message ON logs (module, timestamp, message)");
            }
        };
}
