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

import android.content.BroadcastReceiver
import android.util.Log
import de.yisrime.cosicon.BuildConfig
import de.yisrime.cosicon.utils.tool.ModuleLog
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Handler
import android.os.Looper
import com.highcapable.kavaref.extension.VariousClass
import com.highcapable.kavaref.resolver.ConstructorResolver
import com.highcapable.kavaref.resolver.MethodResolver

/** 统一日志标识 */
private const val LOG_TAG = "ColorOSNotifyIcon"

/** 等待宿主 Application 发布的轮询间隔 */
private const val APPLICATION_RETRY_INTERVAL = 200L

/** 等待宿主 Application 发布的最大轮询次数 */
private const val APPLICATION_RETRY_LIMIT = 50

/**
 * 宿主类加载器，由 MainHook 在包加载时写入
 */
object HostEnv {

    /** 当前宿主进程的类加载器 */
    @Volatile
    var classLoader: ClassLoader? = null

    /** 当前宿主包名 */
    @Volatile
    var packageName: String = ""

    /** 宿主 Application 实例，包就绪后可用 */
    @Volatile
    var applicationContext: Context? = null

    /**
     * 反射取得宿主 Application
     */
    fun resolveApplication(): Context? = runCatching {
        Class.forName("android.app.ActivityThread")
            .getMethod("currentApplication")
            .invoke(null) as? Context
    }.getOrNull()

    /**
     * 取得宿主 Application 后执行
     *
     * 包就绪回调可能早于 ActivityThread 发布 Application，此时直接取会拿到 null，
     * 依赖上下文的挂载会整体静默失效；取不到时改投主循环后续轮次重试。
     * @param block 处理逻辑 - 参数为宿主 Application
     */
    fun awaitApplication(block: (Context) -> Unit) {
        resolveApplication()?.let {
            applicationContext = it
            block(it)
            return
        }
        val handler = Handler(Looper.getMainLooper())
        var attempt = 0
        val probe = object : Runnable {
            override fun run() {
                val application = resolveApplication()
                when {
                    application != null -> {
                        applicationContext = application
                        block(application)
                    }

                    ++attempt < APPLICATION_RETRY_LIMIT ->
                        handler.postDelayed(this, APPLICATION_RETRY_INTERVAL)

                    else -> ModuleLog.error("Aborted Hook -> Application Context Unavailable")
                }
            }
        }
        handler.postDelayed(probe, APPLICATION_RETRY_INTERVAL)
    }

}

/**
 * 宿主生命周期内的接收器注册作用域
 */
class ReceiverScope internal constructor(private val sink: MutableList<BroadcastReceiver>) {

    /**
     * 注册单一行为的接收器
     * @param action 广播行为
     * @param handler 处理逻辑 - 接收者上下文与意图
     */
    fun registerReceiver(action: String, handler: (Context, Intent) -> Unit) =
        registerReceiver(IntentFilter(action), handler)

    /**
     * 注册带过滤条件的接收器
     * @param filter 意图过滤器
     * @param handler 处理逻辑 - 接收者上下文与意图
     */
    fun registerReceiver(filter: IntentFilter, handler: (Context, Intent) -> Unit) {
        val context = HostEnv.applicationContext ?: run {
            ModuleLog.warn("Aborted Receiver -> Application Context Unavailable: $filter")
            return
        }
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(receiverContext: Context, intent: Intent) = handler(receiverContext, intent)
        }
        runCatching { context.registerReceiver(receiver, filter) }
            .onSuccess { sink.add(receiver) }
            .onFailure { ModuleLog.error("Aborted Receiver -> Register Failed: $filter", it) }
    }

    /**
     * 宿主 Application 就绪时执行
     *
     * 依赖 awaitApplication 已保证上下文存在；缺失时不再静默跳过而是留痕。
     * @param block 处理逻辑 - 接收者为主机上下文
     */
    fun onCreate(block: Context.() -> Unit) {
        val context = HostEnv.applicationContext ?: run {
            ModuleLog.warn("Aborted onCreate -> Application Context Unavailable")
            return
        }
        context.block()
    }
}

/**
 * 全部钩子实现类的基类
 */
abstract class HookerBase {

    /** 是否已完成挂载 */
    var isInit = false
        private set

    /** 已注册的接收器 */
    private val receivers = mutableListOf<BroadcastReceiver>()

    /** 执行挂载 */
    open fun prepare() {
        if (isInit) return
        onHook()
        isInit = true
    }

    /** 卸载钩子持有的接收器 */
    open fun detach() {
        val context = HostEnv.applicationContext
        receivers.forEach { runCatching { context?.unregisterReceiver(it) } }
        receivers.clear()
        isInit = false
    }

    /** 子类实现具体挂载逻辑 */
    abstract fun onHook()

    /**
     * 在宿主生命周期内注册监听
     * @param block 注册逻辑
     */
    protected fun onAppLifecycle(block: ReceiverScope.() -> Unit) {
        ReceiverScope(receivers).block()
    }

    /**
     * 延迟加载宿主类，缺失时在首次访问抛出异常
     *
     * 返回 [Class] 的泛型参数固定为 [Any]，使后续 resolve() 得到不变类型参数，
     * 从而允许解析结果调用 of(instance)。
     * @param targets 候选类名或 VariousClass，命中其一即可
     */
    protected fun lazyClass(vararg targets: Any) = lazy {
        findClass(*targets) ?: throw ClassNotFoundException("Class not found: ${targets.joinToString()}")
    }

    /**
     * 延迟加载宿主类，缺失时返回 null
     * @param targets 候选类名或 VariousClass，命中其一即可
     */
    protected fun lazyClassOrNull(vararg targets: Any) = lazy { findClass(*targets) }

    @Suppress("UNCHECKED_CAST")
    private fun findClass(vararg targets: Any): Class<Any>? = targets.firstNotNullOfOrNull { target ->
        when (target) {
            is String -> HostEnv.classLoader?.let { loader ->
                runCatching { Class.forName(target, false, loader) as Class<Any> }.getOrNull()
            }
            is VariousClass -> runCatching {
                target.loadOrNull(HostEnv.classLoader, false) as Class<Any>?
            }.getOrNull()
            else -> null
        }
    }
}

/**
 * 对 Kavaref 解析结果挂载钩子
 */
fun MethodResolver<*>.hook(): MethodHookHandle = self.hook()

/**
 * 对 Kavaref 在 optional 下解析出的全部方法挂载钩子
 */
@JvmName("hookAllMethods")
fun List<out MethodResolver<*>>.hookAll(): HookGroupHandle =
    HookGroupHandle(map { HookEnv.api.hook(it.self) })

/**
 * 对 Kavaref 在 optional 下解析出的全部构造器挂载钩子
 */
@JvmName("hookAllConstructors")
fun List<out ConstructorResolver<*>>.hookAll(): HookGroupHandle =
    HookGroupHandle(map { HookEnv.api.hook(it.self) })

/**
 * 通过类加载器解析类名，缺失时返回 null
 *
 * 宿主类加载器只在注入进程内存在；模块进程内它为空，此时必须退回本进程自身的
 * 类加载器，否则框架启动类（如 com.color.os.ColorBuild）会被判为不存在。
 * @return 解析结果
 */
@Suppress("UNCHECKED_CAST")
fun String.toClassOrNull(): Class<Any>? {
    HostEnv.classLoader?.let { loader ->
        runCatching { Class.forName(this, false, loader) as Class<Any>? }.getOrNull()?.let { return it }
    }
    return runCatching { Class.forName(this) as Class<Any>? }.getOrNull()
}

/**
 * 通过宿主类加载器解析类名
 * @return 解析结果
 * @throws ClassNotFoundException 类不存在
 */
fun String.toClass(): Class<Any> =
    toClassOrNull() ?: throw ClassNotFoundException("Class not found: $this")

/**
 * 判断类是否存在于宿主进程
 */
fun String.hasClass(): Boolean = toClassOrNull() != null

/**
 * 把模块 APK 的资源路径注入到宿主上下文中
 *
 * 使宿主侧可以直接用模块的 R 常量取 drawable。
 */
fun Context.injectModuleAppResources() {
    if (moduleResourcesInjected) return
    runCatching {
        val path = packageManager.getApplicationInfo(BuildConfig.APPLICATION_ID, 0).sourceDir
        val assetManager = resources.javaClass.getMethod("getAssets").invoke(resources)
        assetManager.javaClass.getMethod("addAssetPath", String::class.java)
            .invoke(assetManager, path)
        moduleResourcesInjected = true
    }.onFailure { Log.w("ColorOSNotifyIcon", "模块资源注入失败", it) }
}

private var moduleResourcesInjected = false

/**
 * 反射调用宿主对象方法，替代 XposedHelpers.callMethod
 */
fun Any?.callHostMethod(name: String, vararg args: Any?): Any? {
    if (this == null) return null
    val types = args.map { it?.javaClass }.toList()
    return runCatching {
        val method = javaClass.methods.firstOrNull { candidate ->
            candidate.name == name && candidate.parameterTypes.size == args.size &&
                candidate.parameterTypes.zip(types).all { (parameter, type) ->
                    type == null || parameter.isAssignableFrom(type) || parameter.isPrimitive
                }
        } ?: return@runCatching null
        method.isAccessible = true
        method.invoke(this, *args)
    }.getOrNull()
}

/**
 * 读取宿主对象字段，替代 XposedHelpers.getObjectField
 */
fun Any?.readHostField(name: String): Any? {
    if (this == null) return null
    return runCatching {
        var type: Class<*>? = javaClass
        while (type != null) {
            val field = type.declaredFields.firstOrNull { it.name == name }
            if (field != null) {
                field.isAccessible = true
                return@runCatching field.get(this)
            }
            type = type.superclass
        }
        null
    }.getOrNull()
}
