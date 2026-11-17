package com.hyperalarm.libhook.utils.api

import java.security.MessageDigest

/**
 * 设备指纹（用于诊断信息里的 DeviceToken）。
 *
 * 只在本地计算：把 model / serial / androidId / cpuId 加项目固定盐做 SHA-256，不依赖也不上传到任何
 * 第三方构件。相同设备在本模块内得到稳定值，换设备即变化，且无法反推原始字段。
 */
object DeviceToken {
    private const val SALT = "hyperalarm.device-token/v1"

    @JvmStatic
    fun of(vararg parts: String?): String {
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(SALT.toByteArray(Charsets.UTF_8))
        for (part in parts) {
            // 每个字段前加分隔符，避免 ("ab","c") 与 ("a","bc") 算出同一结果。
            digest.update(0)
            digest.update((part ?: "").toByteArray(Charsets.UTF_8))
        }
        val hash = digest.digest()
        val sb = StringBuilder(hash.size * 2)
        for (b in hash) {
            sb.append(Character.forDigit((b.toInt() shr 4) and 0xF, 16))
            sb.append(Character.forDigit(b.toInt() and 0xF, 16))
        }
        return sb.toString()
    }
}
