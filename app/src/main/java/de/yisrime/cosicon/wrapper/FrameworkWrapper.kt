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
package de.yisrime.cosicon.wrapper

import android.content.Context
import io.github.libxposed.service.XposedService
import de.yisrime.cosicon.hook.HostEnv

/**
 * 框架服务状态
 */
object FrameworkWrapper {

    /** 当前框架服务实例，未连接时为 null */
    @Volatile
    var service: XposedService? = null

    /** 框架服务是否已连接 */
    val isBound: Boolean get() = service != null

    /** 框架名称 */
    val frameworkName: String get() = service?.frameworkName ?: ""

    /** 框架 API 等级 */
    val frameworkApiVersion: Int get() = service?.apiVersion ?: 0

    /**
     * 是否运行在被注入的进程中
     *
     * 宿主类加载器只在注入进程内被登记，据此判定即可，无需探测框架类。
     */
    val isXposedEnvironment: Boolean get() = HostEnv.classLoader != null
}
