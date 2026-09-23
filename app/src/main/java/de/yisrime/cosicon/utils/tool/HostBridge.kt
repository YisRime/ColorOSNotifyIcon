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
package de.yisrime.cosicon.utils.tool

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import androidx.core.content.ContextCompat
import de.yisrime.cosicon.BuildConfig
import de.yisrime.cosicon.const.PackageName
import de.yisrime.cosicon.utils.tool.ModuleLog
import de.yisrime.cosicon.hook.HostEnv
import de.yisrime.cosicon.utils.factory.execShell
import de.yisrime.cosicon.wrapper.BuildConfigWrapper
import java.io.File

/**
 * 模块进程与宿主进程之间的命令通道
 *
 * 替代原 dataChannel：请求与回执各走一次显式广播，大块数据不经 binder 传输，
 * 改由宿主落盘、模块读取。
 */
object HostBridge {

    private const val TAG = "ColorOSNotifyIcon"

    /** 模块发往宿主的命令 */
    const val ACTION_HOST_COMMAND = "de.yisrime.cosicon.action.HOST_COMMAND"

    /** 宿主发回模块的回执 */
    const val ACTION_MODULE_REPLY = "de.yisrime.cosicon.action.MODULE_REPLY"

    /** 命令类别 */
    const val EXTRA_KIND = "kind"

    /** 请求序号，用于匹配回执 */
    const val EXTRA_REQUEST_ID = "request_id"

    /** 布尔结果 */
    const val EXTRA_RESULT = "result"

    /** 宿主加载的模块版本号 */
    const val EXTRA_VERSION_CODE = "version_code"

    /** 附带的仓库版本 */
    const val EXTRA_REPO_VERSION = "repo_version"

    /** 宿主落盘后的日志文件名 */
    const val EXTRA_LOG_NAME = "log_name"

    /** 仓库版本时间戳 */
    const val EXTRA_REPO_TIMESTAMP = "repo_timestamp"

    /** 刷新状态栏缓存 */
    const val KIND_REFRESH_CACHE = 1

    /** 刷新通知图标数据 */
    const val KIND_REFRESH_ICON_DATA = 2

    /** 查询宿主加载的模块版本 */
    const val KIND_QUERY_VERSION = 3

    /** 导出宿主进程内的调试日志 */
    const val KIND_DUMP_LOGS = 4

    /** 宿主日志目录相对外部存储的固定位置 */
    private const val HOST_LOG_DIRECTORY = "/storage/emulated/0/Android/data/"

    /** 允许拼接进 shell 的日志文件名字符集 */
    private val LOG_NAME_PATTERN = Regex("[A-Za-z0-9._-]+\\.log")

    /** 宿主侧的处理逻辑 - 仓库版本与结果回执 */
    private var iconDataHandler: ((Pair<String, Long>, (Boolean) -> Unit) -> Unit)? = null

    /** 宿主侧的缓存刷新逻辑 */
    private var cacheHandler: ((Boolean) -> Boolean)? = null

    /**
     * 宿主侧登记缓存刷新处理逻辑
     */
    fun hostRegisterCacheHandler(handler: (Boolean) -> Boolean) {
        cacheHandler = handler
    }

    /**
     * 宿主侧登记图标数据刷新处理逻辑
     */
    fun hostRegisterIconDataHandler(handler: (Pair<String, Long>, (Boolean) -> Unit) -> Unit) {
        iconDataHandler = handler
    }

    /**
     * 在宿主进程内挂载命令接收器
     */
    fun hostMountReceivers() {
        val context = HostEnv.applicationContext ?: return
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(receiverContext: Context, intent: Intent) {
                val kind = intent.getIntExtra(EXTRA_KIND, 0)
                val requestId = intent.getLongExtra(EXTRA_REQUEST_ID, 0L)
                when (kind) {
                    KIND_REFRESH_CACHE ->
                        reply(receiverContext, requestId, result = cacheHandler?.invoke(
                            intent.getBooleanExtra(EXTRA_RESULT, false)
                        ) ?: false)
                    KIND_REFRESH_ICON_DATA -> {
                        val version = (intent.getStringExtra(EXTRA_REPO_VERSION) ?: "") to
                            intent.getLongExtra(EXTRA_REPO_TIMESTAMP, 0L)
                        val handler = iconDataHandler
                        if (handler == null) reply(receiverContext, requestId, result = false)
                        else handler(version) { ok -> reply(receiverContext, requestId, result = ok) }
                    }
                    KIND_QUERY_VERSION ->
                        reply(receiverContext, requestId, result = true, versionCode = BuildConfig.VERSION_CODE)
                    KIND_DUMP_LOGS -> {
                        val name = writeHostLogs(context)
                        reply(receiverContext, requestId, result = name != null, logName = name)
                    }
                }
            }
        }
        runCatching {
            ContextCompat.registerReceiver(
                context, receiver, IntentFilter(ACTION_HOST_COMMAND), ContextCompat.RECEIVER_EXPORTED
            )
        }.onFailure { Log.e(TAG, "宿主命令接收器注册失败", it) }
    }

    private fun writeHostLogs(context: Context): String? = runCatching {
        val name = "host-${System.currentTimeMillis()}.log"
        val directory = File(context.getExternalFilesDir(null), "logs").apply { mkdirs() }
        File(directory, name).apply {
            writeText(ModuleLog.contents.trim())
            setReadable(true, false)
        }
        name
    }.getOrNull()

    private fun reply(
        context: Context,
        requestId: Long,
        result: Boolean,
        versionCode: Int = 0,
        logName: String? = null
    ) {
        val intent = Intent(ACTION_MODULE_REPLY).apply {
            `package` = BuildConfig.APPLICATION_ID
            putExtra(EXTRA_REQUEST_ID, requestId)
            putExtra(EXTRA_RESULT, result)
            putExtra(EXTRA_VERSION_CODE, versionCode)
            logName?.let { putExtra(EXTRA_LOG_NAME, it) }
        }
        runCatching { context.sendBroadcast(intent) }
    }

    /**
     * 模块侧发起一次请求并等待回执
     * @param context 实例
     * @param kind 命令类别
     * @param repoVersion 附带的仓库版本
     * @param timeoutMs 超时毫秒
     * @param onReply 回执处理 - 意图携带的 extras
     */
    fun request(
        context: Context,
        kind: Int,
        repoVersion: Pair<String, Long>? = null,
        booleanPayload: Boolean = false,
        timeoutMs: Long = 5000L,
        onReply: (Intent) -> Unit
    ) {
        val requestId = System.nanoTime()
        var delivered = false
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(receiverContext: Context, intent: Intent) {
                if (intent.getLongExtra(EXTRA_REQUEST_ID, 0L) != requestId) return
                if (delivered) return
                delivered = true
                runCatching { receiverContext.unregisterReceiver(this) }
                onReply(intent)
            }
        }
        runCatching {
            ContextCompat.registerReceiver(
                context, receiver, IntentFilter(ACTION_MODULE_REPLY), ContextCompat.RECEIVER_EXPORTED
            )
        }.onFailure { Log.e(TAG, "回执接收器注册失败", it); return }
        val command = Intent(ACTION_HOST_COMMAND).apply {
            `package` = PackageName.SYSTEMUI
            putExtra(EXTRA_KIND, kind)
            putExtra(EXTRA_REQUEST_ID, requestId)
            putExtra(EXTRA_RESULT, booleanPayload)
            repoVersion?.let {
                putExtra(EXTRA_REPO_VERSION, it.first)
                putExtra(EXTRA_REPO_TIMESTAMP, it.second)
            }
        }
        runCatching { context.sendBroadcast(command) }
            .onFailure { runCatching { context.unregisterReceiver(receiver) }; return }
        val handler = android.os.Handler(context.getMainLooper())
        handler.postDelayed({
            if (delivered) return@postDelayed
            delivered = true
            runCatching { context.unregisterReceiver(receiver) }
            onReply(Intent(ACTION_MODULE_REPLY).putExtra(EXTRA_RESULT, false))
        }, timeoutMs)
    }

    /**
     * 读取宿主落盘的日志内容
     *
     * 文件名来自广播，必须先校验字符集再拼进 shell，避免命令注入。
     * @param name 宿主回执的日志文件名
     */
    fun readHostLogs(name: String?): String? {
        if (name == null || LOG_NAME_PATTERN.matches(name).not()) return null
        val path = "$HOST_LOG_DIRECTORY${PackageName.SYSTEMUI}/files/logs/$name"
        return runCatching { execShell(cmd = "cat '$path'").takeIf { it.isNotBlank() } }.getOrNull()
    }
}
