/*
 * ColorOSNotifyIcon - Optimize notification icons for ColorOS and adapt to native notification icon specifications.
 * Copyright (C) 20174 Fankes Studio(qzmmcn@163.com)
 * Copyright (C) 2026 YisRime
 *
 * https://github.com/fankes/ColorOSNotifyIcon
 *
 * This software is non-free but opensource software: you can redistribute it
 * and/or modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either
 * version 3 of the License, or any later version.
 * <p>
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * and eula along with this software.  If not, see
 * <https://www.gnu.org/licenses/>
 *
 * This file is created by fankes on 2023/2/2.
 */
@file:Suppress("MemberVisibilityCanBePrivate")
package de.yisrime.cosicon.data
import android.content.Context
import de.yisrime.cosicon.const.IconRuleSourceSyncType
import de.yisrime.cosicon.utils.factory.isUpperOfAndroidS
import com.highcapable.anip.sdk.config.RemoteSource
import de.yisrime.cosicon.utils.tool.ModuleLog
/**
 * 全局配置存储控制类
 */
object ConfigData {
    /** 启用模块 */
    val ENABLE_MODULE = PrefKey("_enable_module", true)
    /** 启用模块日志 */
    val ENABLE_MODULE_LOG = PrefKey("_enable_module_log", false)
    /** 启用通知图标兼容模式 */
    val ENABLE_COLOR_ICON_COMPAT = PrefKey("_color_icon_compat", false)
    /** 移除开发者选项警告通知 */
    val ENABLE_REMOVE_DEV_NOTIFY = PrefKey("_remove_dev_notify", true)
    /** 移除充电完成通知 */
    val ENABLE_REMOVE_CHANGE_COMPLETE_NOTIFY = PrefKey("_remove_charge_complete_notify", false)
    /** 移除免打扰通知 */
    val ENABLE_REMOVE_DND_ALERT_NOTIFY = PrefKey("_remove_dndalert_notify", false)
    /** 启用 Material 3 通知图标风格 */
    val ENABLE_MD3_NOTIFY_ICON_STYLE = PrefKey("_notify_icon_md3_style", isUpperOfAndroidS)
    /** 通知栏中的通知图标圆角程度 */
    val NOTIFY_ICON_CORNER_SIZE = PrefKey("_notify_icon_corner", 15)
    /** 强制通知栏中的通知图标使用系统着色 */
    val ENABLE_NOTIFY_ICON_FORCE_SYSTEM_COLOR = PrefKey("_notify_icon_force_system_color", false)
    /** 强制通知栏中的通知图标为 APP 图标 */
    val ENABLE_NOTIFY_ICON_FORCE_APP_ICON = PrefKey("_notify_icon_force_app_icon", false)
    /** 启用媒体通知播放时自动展开 */
    val ENABLE_NOTIFY_MEDIA_PANEL_AUTO_EXP = PrefKey("_enable_notify_media_panel_auto_exp", false)
    /** 启用自定义通知面板背景透明度 */
    val ENABLE_NOTIFY_PANEL_ALPHA = PrefKey("_enable_notify_panel_alpha_pst", false)
    /** 自定义通知面板背景透明度 */
    val NOTIFY_PANEL_ALPHA_LEVEL = PrefKey("_notify_panel_alpha_pst", 75)
    /** 启用通知图标优化 */
    val ENABLE_NOTIFY_ICON_FIX = PrefKey("_notify_icon_fix", true)
    /** 使用占位符修补未适配的通知图标 */
    val ENABLE_NOTIFY_ICON_FIX_PLACEHOLDER = PrefKey("_notify_icon_fix_placeholder", false)
    /** 提醒未适配通知图标的新安装应用 */
    val ENABLE_NOTIFY_ICON_FIX_NOTIFY = PrefKey("_notify_icon_fix_notify", true)
    /** 启用通知图标优化名单自动更新 */
    val ENABLE_NOTIFY_ICON_FIX_AUTO = PrefKey("_enable_notify_icon_fix_auto", true)
    /** 通知图标优化名单自动更新时间 */
    val NOTIFY_ICON_FIX_AUTO_TIME = PrefKey("_notify_icon_fix_auto_time", "07:00")
    /** 通知图标优化名单同步方式 */
    val ICON_RULE_SOURCE_SYNC_TYPE = PrefKey("_rule_source_sync_way", IconRuleSourceSyncType.GITHUB_PROXY_1)
    /** ANIP GitHub 存储库 */
    val ICON_RULE_REPOSITORY = PrefKey("_rule_repository", RemoteSource.GITHUB_OFFICIAL_REPO_SLUG)
    /**
     * 读取 [String] 数据
     * @param data 键值数据模板
     * @return [String]
     */
    private fun getString(data: PrefKey<String>) = ConfigStore.get(data)
    /**
     * 存入 [String] 数据
     * @param data 键值数据模板
     * @param value 键值内容
     */
    private fun putString(data: PrefKey<String>, value: String) {
        ConfigStore.put(data, value)
    }
    /**
     * 读取 [Int] 数据
     * @param data 键值数据模板
     * @return [Int]
     */
    internal fun getInt(data: PrefKey<Int>) = ConfigStore.get(data)
    /**
     * 存入 [Int] 数据
     * @param data 键值数据模板
     * @param value 键值内容
     */
    internal fun putInt(data: PrefKey<Int>, value: Int) {
        ConfigStore.put(data, value)
    }
    /**
     * 读取 [Boolean] 数据
     * @param data 键值数据模板
     * @return [Boolean]
     */
    internal fun getBoolean(data: PrefKey<Boolean>) = ConfigStore.get(data)
    /**
     * 存入 [Boolean] 数据
     * @param data 键值数据模板
     * @param value 键值内容
     */
    internal fun putBoolean(data: PrefKey<Boolean>, value: Boolean) {
        ConfigStore.put(data, value)
    }
    /**
     * 是否启用模块
     * @return [Boolean]
     */
    var isEnableModule
        get() = getBoolean(ENABLE_MODULE)
        set(value) {
            putBoolean(ENABLE_MODULE, value)
        }
    /**
     * 是否启用模块日志
     * @return [Boolean]
     */
    var isEnableModuleLog
        get() = getBoolean(ENABLE_MODULE_LOG)
        set(value) {
            putBoolean(ENABLE_MODULE_LOG, value)
        }
    /**
     * 是否启用通知图标兼容模式
     * @return [Boolean]
     */
    var isEnableColorIconCompat
        get() = getBoolean(ENABLE_COLOR_ICON_COMPAT)
        set(value) {
            putBoolean(ENABLE_COLOR_ICON_COMPAT, value)
        }
    /**
     * 是否移除开发者选项警告通知
     * @return [Boolean]
     */
    var isEnableRemoveDevNotify
        get() = getBoolean(ENABLE_REMOVE_DEV_NOTIFY)
        set(value) {
            putBoolean(ENABLE_REMOVE_DEV_NOTIFY, value)
        }
    /**
     * 是否移除充电完成通知
     * @return [Boolean]
     */
    var isEnableRemoveChangeCompleteNotify
        get() = getBoolean(ENABLE_REMOVE_CHANGE_COMPLETE_NOTIFY)
        set(value) {
            putBoolean(ENABLE_REMOVE_CHANGE_COMPLETE_NOTIFY, value)
        }
    /**
     * 是否移除免打扰通知
     * @return [Boolean]
     */
    var isEnableRemoveDndAlertNotify
        get() = getBoolean(ENABLE_REMOVE_DND_ALERT_NOTIFY)
        set(value) {
            putBoolean(ENABLE_REMOVE_DND_ALERT_NOTIFY, value)
        }
    /**
     * 是否启用 material 3 通知图标风格
     * @return [Boolean]
     */
    var isEnableMd3NotifyIconStyle
        get() = getBoolean(ENABLE_MD3_NOTIFY_ICON_STYLE)
        set(value) {
            putBoolean(ENABLE_MD3_NOTIFY_ICON_STYLE, value)
        }
    /**
     * 通知栏中的通知图标圆角程度
     * @return [Int]
     */
    var notifyIconCornerSize
        get() = getInt(NOTIFY_ICON_CORNER_SIZE)
        set(value) {
            putInt(NOTIFY_ICON_CORNER_SIZE, value)
        }
    /**
     * 是否强制通知栏中的通知图标使用系统着色
     * @return [Boolean]
     */
    var isEnableNotifyIconForceSystemColor
        get() = getBoolean(ENABLE_NOTIFY_ICON_FORCE_SYSTEM_COLOR)
        set(value) {
            putBoolean(ENABLE_NOTIFY_ICON_FORCE_SYSTEM_COLOR, value)
        }
    /**
     * 是否强制通知栏中的通知图标为 APP 图标
     * @return [Boolean]
     */
    var isEnableNotifyIconForceAppIcon
        get() = getBoolean(ENABLE_NOTIFY_ICON_FORCE_APP_ICON)
        set(value) {
            putBoolean(ENABLE_NOTIFY_ICON_FORCE_APP_ICON, value)
        }
    /**
     * 是否启用媒体通知播放时自动展开
     * @return [Boolean]
     */
    var isEnableNotifyMediaPanelAutoExp
        get() = getBoolean(ENABLE_NOTIFY_MEDIA_PANEL_AUTO_EXP)
        set(value) {
            putBoolean(ENABLE_NOTIFY_MEDIA_PANEL_AUTO_EXP, value)
        }
    /**
     * 是否启用自定义通知面板背景透明度
     * @return [Boolean]
     */
    var isEnableNotifyPanelAlpha
        get() = getBoolean(ENABLE_NOTIFY_PANEL_ALPHA)
        set(value) {
            putBoolean(ENABLE_NOTIFY_PANEL_ALPHA, value)
        }
    /**
     * 自定义通知面板背景透明度
     * @return [Int]
     */
    var notifyPanelAlphaLevel
        get() = getInt(NOTIFY_PANEL_ALPHA_LEVEL)
        set(value) {
            putInt(NOTIFY_PANEL_ALPHA_LEVEL, value)
        }
    /**
     * 是否启用通知图标优化
     * @return [Boolean]
     */
    var isEnableNotifyIconFix
        get() = getBoolean(ENABLE_NOTIFY_ICON_FIX)
        set(value) {
            putBoolean(ENABLE_NOTIFY_ICON_FIX, value)
        }
    /**
     * 是否使用占位符修补未适配的通知图标
     * @return [Boolean]
     */
    var isEnableNotifyIconFixPlaceholder
        get() = getBoolean(ENABLE_NOTIFY_ICON_FIX_PLACEHOLDER)
        set(value) {
            putBoolean(ENABLE_NOTIFY_ICON_FIX_PLACEHOLDER, value)
        }
    /**
     * 是否提醒未适配通知图标的新安装应用
     * @return [Boolean]
     */
    var isEnableNotifyIconFixNotify
        get() = getBoolean(ENABLE_NOTIFY_ICON_FIX_NOTIFY)
        set(value) {
            putBoolean(ENABLE_NOTIFY_ICON_FIX_NOTIFY, value)
        }
    /**
     * 是否启用通知图标优化名单自动更新
     * @return [Boolean]
     */
    var isEnableNotifyIconFixAuto
        get() = getBoolean(ENABLE_NOTIFY_ICON_FIX_AUTO)
        set(value) {
            putBoolean(ENABLE_NOTIFY_ICON_FIX_AUTO, value)
        }
    /**
     * 通知图标优化名单自动更新时间
     * @return [String]
     */
    var notifyIconFixAutoTime
        get() = getString(NOTIFY_ICON_FIX_AUTO_TIME)
        set(value) {
            putString(NOTIFY_ICON_FIX_AUTO_TIME, value)
        }
    /**
     * 通知图标优化名单同步方式
     * @return [Int]
     */
    var iconRuleSourceSyncType
        get() = getInt(ICON_RULE_SOURCE_SYNC_TYPE)
        set(value) {
            putInt(ICON_RULE_SOURCE_SYNC_TYPE, value)
        }
    /**
     * ANIP GitHub 存储库
     * @return [String]
     */
    var iconRuleRepository
        get() = getString(ICON_RULE_REPOSITORY).ifBlank { RemoteSource.GITHUB_OFFICIAL_REPO_SLUG }
        set(value) {
            putString(ICON_RULE_REPOSITORY, value)
        }
}