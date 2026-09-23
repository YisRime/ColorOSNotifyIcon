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
 * This file is created by fankes on 2024/3/24.
 */
package de.yisrime.cosicon.param.factory

import android.content.Context
import de.yisrime.cosicon.data.ConfigStore
import de.yisrime.cosicon.data.PrefKey
import de.yisrime.cosicon.hook.HookerBase
import de.yisrime.cosicon.utils.factory.base64
import com.highcapable.anip.sdk.entity.NotificationIcon

private const val ENABLE_SUFFIX = "_enable"
private const val OVERLAY_SUFFIX = "_enable_all"

private fun NotificationIcon.preferenceName(suffix: String) = (label + packageName).base64 + suffix

fun HookerBase.isAppNotifyEnabledOf(icon: NotificationIcon) =
    ConfigStore.get(PrefKey(icon.preferenceName(ENABLE_SUFFIX), true))

fun Context.isAppNotifyEnabledOf(icon: NotificationIcon) =
    ConfigStore.get(PrefKey(icon.preferenceName(ENABLE_SUFFIX), true))

fun Context.putAppNotifyEnabledOf(icon: NotificationIcon, isEnabled: Boolean) =
    ConfigStore.put(PrefKey(icon.preferenceName(ENABLE_SUFFIX), true), isEnabled)

fun HookerBase.isAppNotifyOverlayOf(icon: NotificationIcon) =
    ConfigStore.get(PrefKey(icon.preferenceName(OVERLAY_SUFFIX), icon.overlay))

fun Context.isAppNotifyOverlayOf(icon: NotificationIcon) =
    ConfigStore.get(PrefKey(icon.preferenceName(OVERLAY_SUFFIX), icon.overlay))

fun Context.putAppNotifyOverlayOf(icon: NotificationIcon, isOverlay: Boolean) =
    ConfigStore.put(PrefKey(icon.preferenceName(OVERLAY_SUFFIX), icon.overlay), isOverlay)
