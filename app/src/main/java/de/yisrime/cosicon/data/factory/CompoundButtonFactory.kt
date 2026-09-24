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
 * This file is created by fankes on 2023/2/3.
 */
@file:Suppress("unused", "MemberVisibilityCanBePrivate")

package de.yisrime.cosicon.data.factory

import android.widget.CompoundButton
import de.yisrime.cosicon.data.ConfigData
import de.yisrime.cosicon.data.PrefKey

/**
 * 绑定到 [CompoundButton] 自动设置选中状态
 * @param data 键值数据模板
 * @param initiate 方法体
 */
fun CompoundButton.bind(data: PrefKey<Boolean>, initiate: CompoundButtonDataBinder.(CompoundButton) -> Unit = {}) {
    val binder = CompoundButtonDataBinder(button = this, write = { ConfigData.putBoolean(data, it) })
        .also { initiate(it, this) }
    isChecked = ConfigData.getBoolean(data).also { binder.initializeCallback?.invoke(it) }
    setOnCheckedChangeListener { button, isChecked ->
        if (button.isPressed) {
            binder.write(isChecked)
            binder.changedCallback?.invoke(isChecked)
        }
    }
}

/**
 * [CompoundButton] 数据绑定管理器实例
 * @param button 当前实例
 * @param write 写入当前状态的逻辑
 */
class CompoundButtonDataBinder internal constructor(private val button: CompoundButton, internal val write: (Boolean) -> Unit) {

    /** 状态初始化回调事件 */
    internal var initializeCallback: ((Boolean) -> Unit)? = null

    /** 状态改变回调事件 */
    internal var changedCallback: ((Boolean) -> Unit)? = null

    /**
     * 监听状态初始化
     * @param result 回调结果
     */
    fun onInitialize(result: (Boolean) -> Unit) {
        initializeCallback = result
    }

    /**
     * 监听状态改变
     * @param result 回调结果
     */
    fun onChanged(result: (Boolean) -> Unit) {
        changedCallback = result
    }

    /** 重新初始化 */
    fun reinitialize() {
        initializeCallback?.invoke(button.isChecked)
    }
}