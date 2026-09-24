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
 * This file is created by fankes on 2022/1/30.
 */
@file:Suppress("SetTextI18n", "InflateParams")

package de.yisrime.cosicon.ui.activity

import androidx.core.view.isVisible
import de.yisrime.cosicon.R
import de.yisrime.cosicon.data.ConfigData
import de.yisrime.cosicon.data.ConfigStore
import de.yisrime.cosicon.databinding.ActivityConfigBinding
import de.yisrime.cosicon.databinding.AdapterConfigBinding
import de.yisrime.cosicon.databinding.DiaIconFilterBinding
import de.yisrime.cosicon.param.factory.isAppNotifyEnabledOf
import de.yisrime.cosicon.param.factory.isAppNotifyOverlayOf
import de.yisrime.cosicon.param.factory.putAppNotifyEnabledOf
import de.yisrime.cosicon.param.factory.putAppNotifyOverlayOf
import de.yisrime.cosicon.ui.activity.base.BaseActivity
import de.yisrime.cosicon.utils.factory.addOnBackPressedEvent
import de.yisrime.cosicon.utils.factory.bindAdapter
import de.yisrime.cosicon.utils.factory.callOnBackPressed
import de.yisrime.cosicon.utils.factory.colorOf
import de.yisrime.cosicon.utils.factory.copyToClipboard
import de.yisrime.cosicon.utils.factory.navigate
import de.yisrime.cosicon.utils.factory.openBrowser
import de.yisrime.cosicon.utils.factory.showDialog
import de.yisrime.cosicon.utils.factory.snake
import de.yisrime.cosicon.utils.factory.toast
import de.yisrime.cosicon.utils.tool.IconRuleManagerTool
import de.yisrime.cosicon.utils.tool.SystemUITool
import com.highcapable.anip.sdk.entity.NotificationIcon
import com.highcapable.betterandroid.ui.extension.component.launch
import de.yisrime.cosicon.wrapper.FrameworkWrapper

class ConfigureActivity : BaseActivity<ActivityConfigBinding>() {

    /** 当前筛选条件 */
    private var filterText = ""

    /** 回调适配器改变 */
    private var onChanged: (() -> Unit)? = null

    /** 回调滚动事件改变 */
    private var onScrollEvent: ((Boolean) -> Unit)? = null

    /** 全部的通知图标优化数据 */
    private var iconAllDatas = emptyList<NotificationIcon>()

    override fun onCreate() {
        /** 检查激活和启用状态 */
        if (ConfigStore.isPreferencesAvailable && (FrameworkWrapper.isBound.not() || ConfigData.isEnableModule.not())) {
            showDialog {
                title = "模块不可用"
                msg = "模块没有激活或已被停用，你无法使用这里的功能，请先激活或启用模块。"
                confirmButton(text = "我知道了") { finish() }
                noCancelable()
            }
            return
        }
        /** 返回按钮点击事件 */
        binding.titleBackIcon.setOnClickListener { callOnBackPressed() }
        /** 刷新适配器结果相关 */
        refreshAdapterResult()
        /** 设置上下按钮点击事件 */
        binding.configTitleUp.setOnClickListener {
            snake(msg = "滚动到顶部")
            onScrollEvent?.invoke(false)
        }
        binding.configTitleDown.setOnClickListener {
            snake(msg = "滚动到底部")
            onScrollEvent?.invoke(true)
        }
        /** 设置过滤按钮点击事件 */
        binding.configTitleFilter.setOnClickListener {
            showDialog<DiaIconFilterBinding> {
                title = "按条件过滤"
                binding.iconFiltersEdit.apply {
                    requestFocus()
                    invalidate()
                    if (filterText.isNotBlank()) {
                        setText(filterText)
                        setSelection(filterText.length)
                    }
                }
                confirmButton {
                    if (binding.iconFiltersEdit.text.toString().isNotBlank()) {
                        filterText = binding.iconFiltersEdit.text.toString().trim()
                        refreshAdapterResult()
                    } else {
                        toast(msg = "条件不能为空")
                        it.performClick()
                    }
                }
                cancelButton()
                if (filterText.isNotBlank())
                    neutralButton(text = "清除条件") {
                        filterText = ""
                        refreshAdapterResult()
                    }
            }
        }
        /** 设置同步列表按钮点击事件 */
        binding.configTitleSync.setOnClickListener { onStartRefresh() }
        /** 设置列表元素和 Adapter */
        binding.configListView.apply {
            bindAdapter {
                onBindDatas { iconDatas }
                onBindViews<AdapterConfigBinding> { binding, position ->
                    iconDatas[position].also { icon ->
                        binding.adpAppIcon.setImageBitmap(IconRuleManagerTool.snapshot?.getBitmap(icon))
                        (icon.color ?: resources.colorOf(R.color.colorTextGray)).also { color ->
                            binding.adpAppIcon.setColorFilter(color)
                            binding.adpAppName.setTextColor(color)
                        }
                        binding.adpAppName.text = icon.label
                        binding.adpAppPkgName.text = icon.packageName
                        binding.adpCbrName.text = "贡献者：" + icon.contributors.joinToString()
                        isAppNotifyEnabledOf(icon).also { e ->
                            binding.adpAppOpenSwitch.isChecked = e
                            binding.adpAppAllSwitch.isEnabled = e
                        }
                        binding.adpAppOpenSwitch.setOnCheckedChangeListener { btn, b ->
                            if (btn.isPressed.not()) return@setOnCheckedChangeListener
                            putAppNotifyEnabledOf(icon, b)
                            binding.adpAppAllSwitch.isEnabled = b
                            SystemUITool.refreshSystemUI(context = this@ConfigureActivity)
                        }
                        binding.adpAppAllSwitch.isChecked = isAppNotifyOverlayOf(icon)
                        binding.adpAppAllSwitch.setOnCheckedChangeListener { btn, b ->
                            if (btn.isPressed.not()) return@setOnCheckedChangeListener
                            putAppNotifyOverlayOf(icon, b)
                            SystemUITool.refreshSystemUI(context = this@ConfigureActivity)
                        }
                    }
                }
            }.apply { onChanged = { notifyDataSetChanged() } }
            onScrollEvent = { post { setSelection(if (it) iconDatas.lastIndex else 0) } }
        }
        /** 设置点击事件 */
        binding.configCbrButton.setOnClickListener {
            showDialog {
                title = "为 ANIP 做出贡献"
                msg = "ANIP 全名为 Android 通知图标适配计划，由社区共同维护。\n" +
                    "你可以参与贡献新图标，也可以为未收录的应用请求适配。"
                confirmButton(text = "参与贡献") { openBrowser(IconRuleManagerTool.RULES_CONTRIBUTING_URL) }
                cancelButton(text = "请求适配") { openBrowser(IconRuleManagerTool.RULES_FEEDBACK_URL) }
                neutralButton(text = "暂时不用")
            }
        }
        /** 装载数据 */
        mockLocalData()
        /** 更新通知对应的仓库和版本，仅在此次通知点击中使用 */
        val requestedSource = intent?.getStringExtra(IconRuleManagerTool.EXTRA_ICON_RULE_SOURCE).orEmpty()
        val requestedVersion = intent?.getLongExtra(IconRuleManagerTool.EXTRA_ICON_RULE_VERSION, 0L) ?: 0L
        /** 更新数据 */
        when {
            intent?.getBooleanExtra("isNewAppSupport", false) == true ->
                showDialog {
                    val appName = intent?.getStringExtra("appName") ?: ""
                    val pkgName = intent?.getStringExtra("pkgName") ?: ""
                    title = "新安装应用通知图标适配"
                    msg = "你已安装 $appName($pkgName)\n\n" +
                        "此应用未在通知优化名单中发现适配数据，若此应用发送的通知为彩色图标，" +
                        "可随时点击本页面下方的 “为 ANIP 做出贡献” 按钮提交贡献或请求适配。\n\n" +
                        "若你已知晓此应用会遵守原生通知图标规范，可忽略此提示。\n\n" +
                        "你可以现在立即同步适配列表，以获取最新的适配数据。"
                    confirmButton(text = "同步列表") { onStartRefresh() }
                    cancelButton(text = "复制名称+包名") { copyToClipboard(content = "$appName($pkgName)") }
                    neutralButton(text = "取消")
                    noCancelable()
                }
            intent?.getBooleanExtra("isDirectUpdate", false) == true -> onStartRefresh(isByHand = false)
            requestedSource.isNotBlank() && requestedVersion > 0L -> launch {
                if (IconRuleManagerTool.isCachedVersionBehind(this@ConfigureActivity, requestedSource to requestedVersion))
                    onStartRefresh(isByHand = false)
            }
            /** 已有数据时直接进列表，同步入口仍可由页面下方的同步按钮使用 */
            intent?.getBooleanExtra("isShowUpdDialog", true) == true && iconAllDatas.isEmpty() -> onStartRefresh()
        }
        /** 清除数据 */
        intent?.apply {
            removeExtra("isNewAppSupport")
            removeExtra("isDirectUpdate")
            removeExtra("isShowUpdDialog")
            removeExtra(IconRuleManagerTool.EXTRA_ICON_RULE_SOURCE)
            removeExtra(IconRuleManagerTool.EXTRA_ICON_RULE_VERSION)
        }
        /** 设置返回监听事件 */
        addOnBackPressedEvent {
            if (MainActivity.isActivityLive.not()) {
                releaseEventAndBack()
                navigate<MainActivity>()
            } else releaseEventAndBack()
        }
    }

    /**
     * 开始同步
     * @param isByHand 是否手动同步 - 默认是
     */
    private fun onStartRefresh(isByHand: Boolean = true) {
        if (isByHand)
            IconRuleManagerTool.syncByHand(context = this) {
                filterText = ""
                mockLocalData()
            }
        else
            IconRuleManagerTool.sync(context = this) {
                filterText = ""
                mockLocalData()
            }
    }

    /** 装载或刷新本地数据 */
    private fun mockLocalData() {
        iconAllDatas = IconRuleManagerTool.snapshot?.icons.orEmpty()
        refreshAdapterResult()
        if (iconAllDatas.isEmpty()) launch {
            if (IconRuleManagerTool.reload(this@ConfigureActivity)) {
                iconAllDatas = IconRuleManagerTool.snapshot?.icons.orEmpty()
                refreshAdapterResult()
            }
        }
    }

    /** 刷新适配器结果相关 */
    private fun refreshAdapterResult() {
        onChanged?.invoke()
        binding.configTitleCountText.text =
            if (filterText.isBlank()) "已收录 ${iconDatas.size} 个 APP 的通知图标"
            else "“$filterText” 匹配到 ${iconDatas.size} 个结果"
        binding.configListNoDataView.apply {
            text = if (iconAllDatas.isEmpty()) "噫，竟然什么都没有~\n请点击右上角同步按钮获取云端数据" else "噫，竟然什么都没找到~"
            isVisible = iconDatas.isEmpty()
        }
    }

    /**
     * 当前结果下的图标数组
     * @return [Array]
     */
    private val iconDatas
        get() = if (filterText.isBlank()) iconAllDatas
        else iconAllDatas.filter {
            it.label.lowercase().contains(filterText.lowercase()) ||
                it.packageName.lowercase().contains(filterText.lowercase()) ||
                it.contributors.any { contributor -> contributor.lowercase().contains(filterText.lowercase()) }
        }
}