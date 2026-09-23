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
package de.yisrime.cosicon.data

import android.content.SharedPreferences

/**
 * 配置键描述
 * @param key 存储键名
 * @param default 缺省值，同时决定读取时使用的类型
 */
class PrefKey<T>(val key: String, val default: T)

/**
 * 跨进程配置存储
 *
 * 模块进程以可写方式挂载，宿主进程以只读方式挂载，
 * 数据由框架经 RemotePreferences 转发，不落地为可读文件。
 */
object ConfigStore {

    /** 配置组名 */
    const val GROUP = "de.yisrime.cosicon"

    @Volatile
    private var prefs: SharedPreferences? = null

    @Volatile
    private var writable = false

    /** 当前挂载的存储实例，未挂载时为 null */
    val source: SharedPreferences? get() = prefs

    /** 是否已挂载 */
    val isAttached: Boolean get() = prefs != null

    /** 配置是否可读写访问 */
    val isPreferencesAvailable: Boolean get() = prefs != null

    /**
     * 挂载配置存储
     * @param source 配置来源
     * @param writable 是否允许写入
     */
    fun attach(source: SharedPreferences, writable: Boolean) {
        prefs = source
        this.writable = writable
    }

    /** 卸载配置存储 */
    fun detach() {
        prefs = null
        writable = false
    }

    /**
     * 读取配置
     * @param key 键描述
     * @return 存储值，未挂载或类型不符时返回缺省值
     */
    @Suppress("UNCHECKED_CAST")
    fun <T> get(key: PrefKey<T>): T {
        val source = prefs ?: return key.default
        val stored = when (val default = key.default) {
            is String -> source.getString(key.key, default)
            is Int -> source.getInt(key.key, default)
            is Long -> source.getLong(key.key, default)
            is Boolean -> source.getBoolean(key.key, default)
            is Float -> source.getFloat(key.key, default)
            is Set<*> -> source.getStringSet(key.key, default as Set<String>)
            else -> null
        }
        return (stored ?: key.default) as T
    }

    /**
     * 写入配置，只读挂载下静默忽略
     * @param key 键描述
     * @param value 写入值
     */
    fun <T> put(key: PrefKey<T>, value: T) {
        val source = prefs ?: return
        if (!writable) return
        val editor = source.edit()
        when (value) {
            is String -> editor.putString(key.key, value)
            is Int -> editor.putInt(key.key, value)
            is Long -> editor.putLong(key.key, value)
            is Boolean -> editor.putBoolean(key.key, value)
            is Float -> editor.putFloat(key.key, value)
            is Set<*> -> @Suppress("UNCHECKED_CAST") editor.putStringSet(key.key, value as Set<String>)
            else -> return
        }
        editor.apply()
    }

    /**
     * 注册配置变更监听
     * @param listener 监听器
     */
    fun registerChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        prefs?.registerOnSharedPreferenceChangeListener(listener)
    }

    /**
     * 注销配置变更监听
     * @param listener 监听器
     */
    fun unregisterChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        prefs?.unregisterOnSharedPreferenceChangeListener(listener)
    }
}
