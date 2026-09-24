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
package de.yisrime.cosicon.ui.activity.base

import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.Bundle
import android.view.LayoutInflater
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.WindowCompat
import androidx.core.view.isVisible
import androidx.viewbinding.ViewBinding
import de.yisrime.cosicon.R
import de.yisrime.cosicon.utils.factory.isNotSystemInDarkMode
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.kavaref.extension.genericSuperclassTypeArguments
import com.highcapable.kavaref.extension.toClassOrNull
import de.yisrime.cosicon.data.ConfigStore

abstract class BaseActivity<VB : ViewBinding> : AppCompatActivity() {

    /** 获取绑定布局对象 */
    lateinit var binding: VB

    /** 内容是否已经渲染 */
    private var contentRendered = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val bindingClass = javaClass.genericSuperclassTypeArguments().firstOrNull()?.toClassOrNull()
        binding = bindingClass?.resolve()?.optional()?.firstMethodOrNull {
            name = "inflate"
            parameters(LayoutInflater::class)
        }?.invoke<VB>(layoutInflater) ?: error("binding failed")
        if (Build.VERSION.SDK_INT >= 35) binding.root.fitsSystemWindows = true
        setContentView(binding.root)
        /** 隐藏系统的标题栏 */
        supportActionBar?.hide()
        /** 初始化沉浸状态栏 */
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = isNotSystemInDarkMode
            isAppearanceLightNavigationBars = isNotSystemInDarkMode
        }
        @Suppress("DEPRECATION")
        ResourcesCompat.getColor(resources, R.color.colorThemeBackground, null).also {
            window?.statusBarColor = it
            window?.navigationBarColor = it
            window?.navigationBarDividerColor = it
        }
        /**
         * 框架服务经 Provider 异步送达，未挂载时先压住内容，等挂载完成再渲染，
         * 避免首帧显示缺省值后再刷新一次；超时说明服务不会再来，照常渲染
         */
        if (ConfigStore.isPreferencesAvailable) {
            renderContent()
        } else {
            binding.root.isVisible = false
            ConfigStore.afterAttached { runOnUiThread { renderContent() } }
            Handler(Looper.getMainLooper()).postDelayed({ if (isDestroyed.not()) renderContent() }, 2500)
        }
    }

    /** 渲染页面内容，只生效一次 */
    private fun renderContent() {
        if (contentRendered) return
        contentRendered = true
        binding.root.isVisible = true
        onCreate()
    }

    /** 回调 [onCreate] 方法 */
    abstract fun onCreate()
}