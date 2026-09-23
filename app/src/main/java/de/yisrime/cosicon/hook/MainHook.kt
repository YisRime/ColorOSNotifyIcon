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
package de.yisrime.cosicon.hook

import android.util.Log
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface.ModuleLoadedParam
import io.github.libxposed.api.XposedModuleInterface.PackageLoadedParam
import io.github.libxposed.api.XposedModuleInterface.PackageReadyParam
import io.github.libxposed.api.XposedModuleInterface.SystemServerStartingParam
import de.yisrime.cosicon.const.PackageName
import de.yisrime.cosicon.data.ConfigData
import de.yisrime.cosicon.data.ConfigStore
import de.yisrime.cosicon.hook.entity.FrameworkHooker
import de.yisrime.cosicon.hook.entity.SystemUIHooker
import de.yisrime.cosicon.utils.factory.isNotColorOS

/**
 * libxposed 模块入口
 */
class MainHook : XposedModule() {

    override fun onModuleLoaded(param: ModuleLoadedParam) {
        HookEnv.api = this
    }

    override fun onPackageLoaded(param: PackageLoadedParam) {
        HostEnv.classLoader = param.defaultClassLoader
        HostEnv.packageName = param.packageName
        runCatching { ConfigStore.attach(getRemotePreferences(ConfigStore.GROUP), false) }
            .onFailure { Log.w(TAG, "配置挂载失败", it) }
    }

    override fun onPackageReady(param: PackageReadyParam) {
        HostEnv.classLoader = param.classLoader
        HostEnv.packageName = param.packageName
        if (param.packageName != PackageName.SYSTEMUI) return
        HostEnv.applicationContext = HostEnv.resolveApplication()
        if (isNotColorOS) {
            Log.w(TAG, "Aborted Hook -> This System is not ColorOS")
            return
        }
        if (ConfigData.isEnableModule.not()) {
            Log.w(TAG, "Aborted Hook -> Hook Closed")
            return
        }
        runCatching { SystemUIHooker.prepare() }
            .onFailure { Log.e(TAG, "Hook 挂载失败: SystemUIHooker", it) }
    }

    override fun onSystemServerStarting(param: SystemServerStartingParam) {
        HostEnv.classLoader = param.classLoader
        if (isNotColorOS) return
        runCatching { FrameworkHooker.prepare() }
            .onFailure { Log.e(TAG, "Hook 挂载失败: FrameworkHooker", it) }
    }

    private companion object {

        const val TAG = "ColorOSNotifyIcon"
    }
}
