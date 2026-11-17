package com.hyperalarm.libhook.utils.hookapi

/**
 * 宿主类句柄，用到时才解析。
 *
 * `Class.forName` 放在 lazy 里，宿主没有这个类（非 MIUI 环境）时只让当次调用失败，
 * 不会在类初始化阶段把整个模块拖崩；调用方统一用 runCatching 兜住。
 */
object LazyClass {
    @JvmStatic
    val clazzMiuiBuild: Class<*>
        by lazy { Class.forName("miui.os.Build") }
}
