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
 * This file is created by fankes on 2022/2/26.
 */
@file:Suppress("SetTextI18n")

package de.yisrime.cosicon.ui.activity

import androidx.core.view.isVisible
import de.yisrime.cosicon.R
import de.yisrime.cosicon.const.ModuleVersion
import de.yisrime.cosicon.data.ConfigData
import de.yisrime.cosicon.data.factory.bind
import de.yisrime.cosicon.databinding.ActivityMainBinding
import de.yisrime.cosicon.ui.activity.base.BaseActivity
import de.yisrime.cosicon.utils.factory.androidVersionCodeName
import de.yisrime.cosicon.utils.factory.colorOSFullVersion
import de.yisrime.cosicon.utils.factory.colorOSNumberVersion
import de.yisrime.cosicon.utils.factory.hideOrShowLauncherIcon
import de.yisrime.cosicon.utils.factory.isLauncherIconShowing
import de.yisrime.cosicon.utils.factory.isNotColorOS
import de.yisrime.cosicon.utils.factory.isNotNoificationEnabled
import de.yisrime.cosicon.utils.factory.navigate
import de.yisrime.cosicon.utils.factory.openBrowser
import de.yisrime.cosicon.utils.factory.openNotifySetting
import de.yisrime.cosicon.utils.factory.showDialog
import de.yisrime.cosicon.utils.factory.showTimePicker
import de.yisrime.cosicon.utils.tool.I18nWarnTool
import de.yisrime.cosicon.utils.tool.IconRuleManagerTool
import de.yisrime.cosicon.utils.tool.SystemUITool
import de.yisrime.cosicon.wrapper.FrameworkWrapper

class MainActivity : BaseActivity<ActivityMainBinding>() {

    companion object {

        /** 窗口是否启动 */
        internal var isActivityLive = false

        /** 模块是否可用 */
        internal var isModuleRegular = false

        /** 模块是否有效 */
        internal var isModuleValied = false
    }

    override fun onCreate() {
        /** 设置可用性 */
        isActivityLive = true
        when {
            /** 判断是否为 ColorOS 系统 */
            isNotColorOS ->
                showDialog {
                    title = "不是 ColorOS 系统"
                    msg = "此模块专为 ColorOS 系统打造，当前无法识别你的系统为 ColorOS，所以模块无法工作。"
                    confirmButton(text = "查看支持的模块") {
                        openBrowser(url = IconRuleManagerTool.ANIP_REPOSITORY_URL)
                        finish()
                    }
                    cancelButton(text = "退出") { finish() }
                    noCancelable()
                }
            /** 判断是否 Hook */
            FrameworkWrapper.isBound -> {
                if (IconRuleManagerTool.hasCachedResources(this).not() && ConfigData.isEnableNotifyIconFix)
                    showDialog {
                        title = "配置通知图标优化名单"
                        msg = "模块需要更新 “通知图标优化名单”，它现在是空的，这看起来是你第一次使用模块，请首先进行配置才可以使用相关功能。\n" +
                            "你可以随时在本页面下方找到 “配置通知图标优化名单” 手动前往。"
                        confirmButton(text = "前往") { navigate<ConfigureActivity>() }
                        cancelButton()
                        noCancelable()
                    }
                if (isNotNoificationEnabled && ConfigData.isEnableNotifyIconFix)
                    showDialog {
                        title = "模块的通知权限已关闭"
                        msg = "请开启通知权限，以确保你能收到通知图标优化名单的更新。"
                        confirmButton { openNotifySetting() }
                        cancelButton()
                        noCancelable()
                    }
            }
            else -> showNotActivatedDialog()
        }
        I18nWarnTool.checkingOrShowing(context = this)
        binding.mainTextVersion.text = "模块版本：${ModuleVersion.NAME}"
        /** 设置 CI 自动构建标识 */
        if (ModuleVersion.isCiMode)
            binding.mainTextReleaseVersion.apply {
                text = "CI ${ModuleVersion.GITHUB_COMMIT_ID}"
                isVisible = true
                setOnClickListener {
                    showDialog {
                        title = "CI 自动构建说明"
                        msg = """
                          你正在使用的是 CI 自动构建版本，Commit ID 为 ${ModuleVersion.GITHUB_COMMIT_ID}。
                          
                          它是由代码提交后自动触发并构建、自动编译发布的，并未经任何稳定性测试，使用风险自负。
                        """.trimIndent()
                        confirmButton(text = "我知道了")
                        noCancelable()
                    }
                }
            }
        binding.mainTextColorOsVersion.text = "系统版本：[$androidVersionCodeName] $colorOSFullVersion"
        /** 媒体通知自动展开仅支持 12.1 - 旧版本适配过于复杂已放弃 */
        if (colorOSNumberVersion != "V12.1") {
            binding.notifyMediaPanelAutoExpSwitch.isVisible = false
            binding.notifyMediaPanelAutoExpText.isVisible = false
        }
        /** 通知面板背景透明度功能仅支持 ColorOS 12、12.1、13、13.1 */
        binding.notifyPanelConfigItem.isVisible = colorOSNumberVersion.let { it == "V12" || it == "V12.1" || it == "V13" || it == "V13.1" }
        binding.notifyIconAutoSyncText.text = ConfigData.notifyIconFixAutoTime
        binding.moduleEnableSwitch.bind(ConfigData.ENABLE_MODULE) {
            onInitialize {
                binding.moduleEnableLogItem.isVisible = it
                binding.expAllDebugLogButton.isVisible = it && ConfigData.isEnableModuleLog
                binding.notifyIconConfigItem.isVisible = it
                binding.devNotifyConfigItem.isVisible = it
                binding.notifyStyleConfigItem.isVisible = it
            }
            onChanged {
                reinitialize()
                refreshModuleStatus()
                SystemUITool.showNeedRestartSnake(context = this@MainActivity)
            }
        }
        binding.moduleEnableLogSwitch.bind(ConfigData.ENABLE_MODULE_LOG) {
            onInitialize { binding.expAllDebugLogButton.isVisible = it && ConfigData.isEnableModule }
            onChanged {
                reinitialize()
                SystemUITool.refreshSystemUI(context = this@MainActivity, isRefreshCacheOnly = true)
            }
        }
        binding.devNotifyConfigSwitch.bind(ConfigData.ENABLE_REMOVE_DEV_NOTIFY) {
            onChanged { SystemUITool.refreshSystemUI(context = this@MainActivity, isRefreshCacheOnly = true) }
        }
        binding.crcpNotifyConfigSwitch.bind(ConfigData.ENABLE_REMOVE_CHANGE_COMPLETE_NOTIFY) {
            onChanged { SystemUITool.refreshSystemUI(context = this@MainActivity, isRefreshCacheOnly = true) }
        }
        binding.dndNotifyConfigSwitch.bind(ConfigData.ENABLE_REMOVE_DND_ALERT_NOTIFY) {
            onChanged { SystemUITool.refreshSystemUI(context = this@MainActivity, isRefreshCacheOnly = true) }
        }
        binding.colorIconCompatSwitch.bind(ConfigData.ENABLE_COLOR_ICON_COMPAT) {
            onChanged { SystemUITool.refreshSystemUI(context = this@MainActivity) }
        }
        binding.md3StyleConfigSwitch.bind(ConfigData.ENABLE_MD3_NOTIFY_ICON_STYLE) {
            onInitialize { binding.notifyIconCustomCornerItem.isVisible = it && ConfigData.isEnableNotifyIconForceAppIcon.not() }
            onChanged {
                reinitialize()
                SystemUITool.refreshSystemUI(context = this@MainActivity)
            }
        }
        binding.notifyIconForceSystemColorSwitch.bind(ConfigData.ENABLE_NOTIFY_ICON_FORCE_SYSTEM_COLOR) {
            onChanged { SystemUITool.refreshSystemUI(context = this@MainActivity) }
        }
        binding.notifyIconForceAppIconSwitch.bind(ConfigData.ENABLE_NOTIFY_ICON_FORCE_APP_ICON) {
            onInitialize {
                binding.notifyIconForceSystemColorItem.isVisible = it.not()
                binding.notifyIconCustomCornerItem.isVisible = it.not() && ConfigData.isEnableMd3NotifyIconStyle
            }
            onChanged {
                reinitialize()
                SystemUITool.refreshSystemUI(context = this@MainActivity)
            }
        }
        binding.notifyPanelConfigSwitch.bind(ConfigData.ENABLE_NOTIFY_PANEL_ALPHA) {
            onInitialize {
                binding.notifyPanelConfigTextPanel.isVisible = it
                binding.notifyPanelConfigWarnPanel.isVisible = it
                binding.notifyPanelConfigSeekbar.isVisible = it
            }
            onChanged {
                reinitialize()
                SystemUITool.refreshSystemUI(context = this@MainActivity)
            }
        }
        binding.notifyMediaPanelAutoExpSwitch.bind(ConfigData.ENABLE_NOTIFY_MEDIA_PANEL_AUTO_EXP) {
            onChanged { SystemUITool.refreshSystemUI(context = this@MainActivity, isRefreshCacheOnly = true) }
        }
        binding.notifyIconFixSwitch.bind(ConfigData.ENABLE_NOTIFY_ICON_FIX) {
            onInitialize {
                binding.notifyIconFixButton.isVisible = it
                binding.notifyIconFixPlaceholderItem.isVisible = it
                binding.notifyIconFixNotifyItem.isVisible = it
                binding.notifyIconAutoSyncItem.isVisible = it
            }
            onChanged {
                reinitialize()
                SystemUITool.refreshSystemUI(context = this@MainActivity)
            }
        }
        binding.notifyIconFixPlaceholderSwitch.bind(ConfigData.ENABLE_NOTIFY_ICON_FIX_PLACEHOLDER) {
            onChanged { SystemUITool.refreshSystemUI(context = this@MainActivity) }
        }
        binding.notifyIconFixNotifySwitch.bind(ConfigData.ENABLE_NOTIFY_ICON_FIX_NOTIFY) {
            onChanged { SystemUITool.refreshSystemUI(context = this@MainActivity, isRefreshCacheOnly = true) }
        }
        binding.notifyIconAutoSyncSwitch.bind(ConfigData.ENABLE_NOTIFY_ICON_FIX_AUTO) {
            onInitialize { binding.notifyIconAutoSyncChildItem.isVisible = it }
            onChanged {
                reinitialize()
                SystemUITool.refreshSystemUI(context = this@MainActivity, isRefreshCacheOnly = true)
            }
        }
        binding.notifyPanelConfigSeekbar.bind(ConfigData.NOTIFY_PANEL_ALPHA_LEVEL, binding.notifyPanelConfigText, suffix = "%") {
            SystemUITool.refreshSystemUI(context = this)
        }
        binding.notifyIconCustomCornerSeekbar.bind(ConfigData.NOTIFY_ICON_CORNER_SIZE, binding.notifyIconCustomCornerText, suffix = " dp") {
            SystemUITool.refreshSystemUI(context = this)
        }
        /** 导出全部日志按钮点击事件 */
        binding.expAllDebugLogButton.setOnClickListener { SystemUITool.obtainAndExportDebugLogs(context = this) }
        /** 通知图标优化名单按钮点击事件 */
        binding.notifyIconFixButton.setOnClickListener { navigate<ConfigureActivity>() }
        /** 自动更新在线规则修改时间按钮点击事件 */
        binding.notifyIconAutoSyncButton.setOnClickListener {
            showTimePicker(ConfigData.notifyIconFixAutoTime) {
                ConfigData.notifyIconFixAutoTime = it
                binding.notifyIconAutoSyncText.text = it
                SystemUITool.refreshSystemUI(this, isRefreshCacheOnly = true)
            }
        }
        /** 重启按钮点击事件 */
        binding.titleRestartIcon.setOnClickListener { SystemUITool.restartSystemUI(context = this) }
        /** 设置桌面图标显示隐藏 */
        binding.hideIconInLauncherSwitch.isChecked = isLauncherIconShowing.not()
        binding.hideIconInLauncherSwitch.setOnCheckedChangeListener { btn, b ->
            if (btn.isPressed.not()) return@setOnCheckedChangeListener
            hideOrShowLauncherIcon(b)
        }
        /** 注册导出调试日志启动器 */
        SystemUITool.registerExportDebugLogsLauncher(activity = this)
    }

    /** 模块未激活提示 */
    private fun showNotActivatedDialog() {
        showDialog {
            title = "模块没有激活"
            msg = "检测到模块没有激活，模块需要 Xposed 环境依赖，" +
                "同时需要系统拥有 Root 权限，" +
                "请自行查看本页面使用帮助与说明第二条。\n" +
                "由于需要修改系统应用达到效果，模块不支持太极阴、应用转生。"
            confirmButton(text = "我知道了")
            noCancelable()
        }
    }

    /** 刷新模块状态 */
    private fun refreshModuleStatus() {
        binding.mainLinStatus.setBackgroundResource(
            when {
                FrameworkWrapper.isBound &&
                    (isModuleRegular.not() || isModuleValied.not() || ConfigData.isEnableModule.not()) -> R.drawable.bg_yellow_round
                FrameworkWrapper.isBound -> R.drawable.bg_green_round
                else -> R.drawable.bg_dark_round
            }
        )
        binding.mainImgStatus.setImageResource(
            when {
                FrameworkWrapper.isBound && ConfigData.isEnableModule -> R.drawable.ic_success
                else -> R.drawable.ic_warn
            }
        )
        binding.mainTextStatus.text = when {
            FrameworkWrapper.isBound && ConfigData.isEnableModule.not() -> "模块已停用"
            FrameworkWrapper.isBound && isModuleRegular.not() -> "模块已激活，请重启系统界面"
            FrameworkWrapper.isBound && isModuleValied.not() -> "模块已更新，请重启系统界面"
            FrameworkWrapper.isBound -> "模块已激活"
            else -> "模块未激活"
        }
        val frameworkName = FrameworkWrapper.frameworkName
        binding.mainTextApiWay.isVisible = frameworkName.isNotEmpty()
        binding.mainTextApiWay.text = "Activated by $frameworkName API ${FrameworkWrapper.frameworkApiVersion}"
    }

    override fun onResume() {
        super.onResume()
        /** 刷新模块状态 */
        refreshModuleStatus()
        /** 检查模块激活状态 */
        SystemUITool.checkingActivated(context = this) { isValied ->
            isModuleRegular = true
            isModuleValied = isValied
            refreshModuleStatus()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        isActivityLive = false
    }
}