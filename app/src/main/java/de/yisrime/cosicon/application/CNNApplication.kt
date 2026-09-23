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
 * This file is created by fankes on 2022/1/24.
 */
@file:Suppress("unused")
package de.yisrime.cosicon.application
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import io.github.libxposed.service.XposedService
import io.github.libxposed.service.XposedServiceHelper
import de.yisrime.cosicon.data.ConfigStore
import de.yisrime.cosicon.wrapper.FrameworkWrapper
import android.app.Application
import de.yisrime.cosicon.wrapper.AppWrapper
class CNNApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        /** 跟随系统夜间模式 */
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        AppWrapper.attach(this)
        /** 连接框架服务并挂载可写的配置存储 */
        XposedServiceHelper.registerListener(object : XposedServiceHelper.OnServiceListener {
            override fun onServiceBind(service: XposedService) {
                runCatching { ConfigStore.attach(service.getRemotePreferences(ConfigStore.GROUP), true) }
                    .onFailure { Log.e(TAG, "框架服务绑定失败", it) }
                FrameworkWrapper.service = service
            }
            override fun onServiceDied(service: XposedService) {
                ConfigStore.detach()
                FrameworkWrapper.service = null
            }
        })
    }
    private companion object {
        const val TAG = "ColorOSNotifyIcon"
    }
}