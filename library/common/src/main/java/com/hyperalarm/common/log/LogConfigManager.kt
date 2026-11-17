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
package com.hyperalarm.common.log

import com.hyperalarm.common.utils.PrefsBridge
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.channels.FileLock
import java.nio.file.StandardOpenOption

/**
 * 日志配置管理器
 */
object LogConfigManager {
    private const val LOG_CONFIG_FILENAME = "files/log/log_config_v2"
    // 应用私有目录，需要在初始化时设置
    private var appPrivateDir: String? = null

    @JvmStatic
    fun init(privateDir: String) {
        appPrivateDir = privateDir
    }

    @JvmStatic
    fun writeLogLevel(level: Int) {
        writeLogLevel(null, level)
    }

    @JvmStatic
    fun writeLogLevel(basePath: String?, level: Int) {
        try {
            val baseDir = basePath ?: appPrivateDir ?: return
            val configFile = File(baseDir, LOG_CONFIG_FILENAME)
            val configDir = configFile.parentFile

            if (configDir != null && !configDir.exists()) {
                configDir.mkdirs()
            }

            // The lock and the write have to share one descriptor: a second FileWriter would write
            // outside the lock, and truncating before taking the lock could empty the file for a
            // concurrent reader (which then falls back to the default level).
            FileChannel.open(
                configFile.toPath(),
                StandardOpenOption.CREATE,
                StandardOpenOption.WRITE
            ).use { channel ->
                val lock: FileLock = channel.lock()
                try {
                    channel.truncate(0)
                    channel.position(0)
                    channel.write(ByteBuffer.wrap(level.toString().toByteArray(Charsets.UTF_8)))
                    channel.force(true)
                } finally {
                    lock.release()
                }
            }
        } catch (e: Exception) {
            AndroidLog.e("LogConfigManager", "Failed to write log level to file: ", e)
        }
    }

    @JvmStatic
    fun readLogLevel(): Int {
        return readLogLevel(null)
    }

    @JvmStatic
    fun readLogLevel(basePath: String?): Int {
        try {
            val baseDir = basePath ?: appPrivateDir ?: return getDefaultLogLevel()
            val configFile = File(baseDir, LOG_CONFIG_FILENAME)

            if (configFile.exists()) {
                BufferedReader(FileReader(configFile)).use { reader ->
                    val line = reader.readLine()
                    if (line != null) {
                        try {
                            val level = line.trim().toInt()
                            return LogLevelManager.getEffectiveLogLevel(level)
                        } catch (_: NumberFormatException) {
                        }
                    }
                }
            }
        } catch (e: Exception) {
            AndroidLog.e("LogConfigManager", "Failed to read log level from file: ", e)
        }

        return getDefaultLogLevel()
    }

    private fun getDefaultLogLevel(): Int {
        return try {
            val level = PrefsBridge.getStringAsInt(LogLevelManager.PREF_KEY, LogLevelManager.getDefaultLogLevel())
            LogLevelManager.getEffectiveLogLevel(level)
        } catch (_: IllegalStateException) {
            LogLevelManager.getDefaultLogLevel()
        }
    }
}
