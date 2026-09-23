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

import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedInterface.Chain
import io.github.libxposed.api.XposedInterface.HookBuilder
import java.lang.reflect.Executable

/**
 * 当前进程内的 libxposed 框架接口
 */
object HookEnv {

    /** 框架接口实例 */
    lateinit var api: XposedInterface
}

/**
 * 方法钩子回调上下文
 */
class HookCallback internal constructor(@PublishedApi internal val chain: Chain) {

    private var replaced = false
    private var replacement: Any? = null

    /** 当前调用参数 */
    fun args(): List<Any?> = chain.getArgs()

    /** 当前调用参数列表 */
    val args: List<Any?> get() = chain.getArgs()

    /**
     * 按下标取参数
     * @param index 下标
     */
    fun args(index: Int): Any? = chain.getArgs().getOrNull(index)

    /** 当前调用实例 */
    val thisObject: Any? get() = chain.getThisObject()

    /** 当前调用实例，与 thisObject 同义 */
    val instance: Any? get() = chain.getThisObject()

    /** 读取或改写方法返回值，改写后原方法不再执行 */
    var result: Any?
        get() = if (replaced) replacement else null
        set(value) {
            replaced = true
            replacement = value
        }

    /** 拦截并返回 null，原方法不再执行 */
    fun resultNull() {
        replaced = true
        replacement = null
    }

    /** 拦截并返回 false，原方法不再执行 */
    fun resultFalse() {
        replaced = true
        replacement = false
    }

    /** 拦截并返回 true，原方法不再执行 */
    fun resultTrue() {
        replaced = true
        replacement = true
    }

    /** 拦截并返回指定值，原方法不再执行 */
    @JvmName("setResultValue")
    fun setResult(value: Any?) {
        replaced = true
        replacement = value
    }

    /** 是否已拦截 */
    internal fun isReplaced() = replaced

    /** 取回拦截结果 */
    internal fun takeResult(): Any? = replacement

    /** 执行原方法并返回其结果 */
    fun callOriginal(): Any? = runCatching { chain.proceed() }.getOrNull()

    /** 以指定参数执行原方法 */
    fun callOriginal(args: Array<Any?>): Any? =
        runCatching { chain.proceed(args) }.getOrNull()

    /** 当前调用实例并按类型转换 */
    inline fun <reified T> instance(): T = chain.getThisObject() as T
}

/** 任意值本身 */
fun Any?.any(): Any? = this

/** 转换为 [Int] */
fun Any?.int(): Int = (this as? Number)?.toInt() ?: 0

/** 转换为 [Long] */
fun Any?.long(): Long = (this as? Number)?.toLong() ?: 0L

/** 转换为 [Boolean] */
fun Any?.boolean(): Boolean = this as? Boolean ?: false

/** 转换为 [String] */
fun Any?.string(): String? = this as? String

/** 按类型转换 */
inline fun <reified T> Any?.cast(): T? = this as? T

/**
 * 单个方法的钩子构建器
 */
class MethodHookHandle internal constructor(private val builder: HookBuilder) {

    /** 在原方法执行前介入，可通过 resultNull 等方式跳过原方法 */
    fun before(block: HookCallback.() -> Unit) = builder.intercept { chain ->
        HookCallback(chain).apply(block).let { if (it.isReplaced()) it.takeResult() else chain.proceed() }
    }

    /** 在原方法执行后介入 */
    fun after(block: HookCallback.() -> Unit) = builder.intercept { chain ->
        val callback = HookCallback(chain)
        val origin = chain.proceed()
        callback.setResult(origin)
        callback.block()
        if (callback.isReplaced()) callback.takeResult() else origin
    }

    /** 完全替换原方法，回调返回值即为方法结果 */
    fun replaceAny(block: HookCallback.() -> Any?) = builder.intercept { chain ->
        HookCallback(chain).block()
    }

    /** 拦截并丢弃原方法执行 */
    fun intercept() = builder.intercept { null }
}

/**
 * 对可执行成员挂载钩子
 */
fun Executable.hook(): MethodHookHandle = MethodHookHandle(HookEnv.api.hook(this))

/**
 * 一组解析结果的批量钩子
 */
class HookGroupHandle internal constructor(private val builders: List<HookBuilder>) {

    /** 在原成员执行前介入 */
    fun before(block: HookCallback.() -> Unit) = builders.forEach { it.before(block) }

    /** 在原成员执行后介入 */
    fun after(block: HookCallback.() -> Unit) = builders.forEach { it.after(block) }

    /** 完全替换原成员执行 */
    fun replaceAny(block: HookCallback.() -> Any?) = builders.forEach { it.replaceAny(block) }

    /** 拦截并丢弃原成员执行 */
    fun intercept() = builders.forEach { it.intercept() }

    private fun HookBuilder.before(block: HookCallback.() -> Unit) {
        intercept { chain ->
            HookCallback(chain).apply(block)
                .let { if (it.isReplaced()) it.takeResult() else chain.proceed() }
        }
    }

    private fun HookBuilder.after(block: HookCallback.() -> Unit) {
        intercept { chain ->
            val callback = HookCallback(chain)
            val origin = chain.proceed()
            callback.setResult(origin)
            callback.block()
            if (callback.isReplaced()) callback.takeResult() else origin
        }
    }

    private fun HookBuilder.replaceAny(block: HookCallback.() -> Any?) {
        intercept { chain -> HookCallback(chain).block() }
    }

    private fun HookBuilder.intercept() {
        intercept { null }
    }
}
