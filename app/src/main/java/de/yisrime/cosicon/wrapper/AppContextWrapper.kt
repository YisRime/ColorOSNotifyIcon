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
import de.yisrime.cosicon.hook.HostEnv

/**
 * 进程级上下文
 *
 * 宿主进程内取宿主 Application，模块进程内取自身 Application。
 */
val appContext: Context
    get() = HostEnv.applicationContext ?: AppWrapper.context
        ?: throw IllegalStateException("Context not available yet")

/**
 * 模块进程上下文持有者
 */
object AppWrapper {

    @Volatile
    var context: Context? = null
        private set

    /**
     * 由 Application 调用登记自身
     */
    fun attach(context: Context) {
        this.context = context.applicationContext
    }
}
