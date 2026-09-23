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
 * This file is created by fankes on 2022/3/26.
 */
package de.yisrime.cosicon.service

import android.service.quicksettings.TileService
import de.yisrime.cosicon.ui.activity.ConfigureActivity
import de.yisrime.cosicon.utils.factory.navigate

class QuickStartTileService : TileService() {

    override fun onClick() {
        super.onClick()
        /** 启动通知图标优化列表窗口 */
        navigate<ConfigureActivity>()
    }
}