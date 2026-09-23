/*
 * ColorOSNotifyIcon - Optimize notification icons for ColorOS and adapt to native notification icon specifications.
 * Copyright (C) 2026 YisRime
 *
 * This software is free opensource software: you can redistribute it
 * and/or modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either
 * version 3 of the License, or any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this software.  If not, see <https://www.gnu.org/licenses/>.
 */
package de.yisrime.cosicon.utils.tool

import android.util.Log
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 模块日志
 *
 * 取代原框架封装的日志器：写入 logcat 的同时保留内存环形缓冲，
 * 供宿主落盘后由模块进程读取导出。
 */
object ModuleLog {

    private const val TAG = "ColorOSNotifyIcon"

    /** 环形缓冲上限 */
    private const val MAX_RECORDS = 1000

    private val records = ArrayDeque<String>()
    private val timeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())

    /** 已记录的日志全文 */
    val contents: String get() = synchronized(records) { records.joinToString("\n") }

    /** 清空缓冲 */
    fun clear() {
        synchronized(records) { records.clear() }
    }

    /** 记录调试级日志 */
    fun debug(msg: String) = record("D", msg, null)

    /** 记录提示级日志 */
    fun info(msg: String) = record("I", msg, null)

    /** 记录警告级日志 */
    fun warn(msg: String) = record("W", msg, null)

    /** 记录错误级日志 */
    fun error(msg: String, throwable: Throwable? = null) = record("E", msg, throwable)

    private fun record(level: String, msg: String, throwable: Throwable?) {
        val line = "${timeFormat.format(Date())}/$level/$TAG: $msg"
        synchronized(records) {
            records.addLast(line)
            while (records.size > MAX_RECORDS) records.removeFirst()
        }
        when (level) {
            "E" -> Log.e(TAG, msg, throwable)
            "W" -> Log.w(TAG, msg, throwable)
            "I" -> Log.i(TAG, msg, throwable)
            else -> Log.d(TAG, msg, throwable)
        }
    }
}
