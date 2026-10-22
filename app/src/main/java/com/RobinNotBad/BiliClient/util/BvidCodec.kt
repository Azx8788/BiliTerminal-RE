package com.RobinNotBad.BiliClient.util

/**
 * AV 号与 BV 号的互转（纯算法，无 Android 依赖，可直接 JVM 单测）。
 *
 * 为什么需要它：播放器手里只有 av 号（[android.content.Intent] 的 `aid`），而
 * SponsorBlock 的接口只认 BV 号；为此给 [com.RobinNotBad.BiliClient.model.PlayerData]
 * 加一个 Parcelable 字段要改序列化和所有入口，代价远大于这三十行算法。
 *
 * 算法：把 `(2^51 | aid) ^ 23442827791579` 按 58 进制写进 12 位 BV 号的后 9 位，
 * 再把第 4/10 位、第 5/8 位（下标从 0 起）各交换一次。已知向量见单测。
 */
object BvidCodec {

    private const val TABLE = "FcwAPNKTMug3GV5Lj7EJnHpWsx4tb8haYeviqBz6rkCy12mUSDQX9RdoZf"
    private const val XOR_CODE = 23442827791579L
    private const val MASK_CODE = (1L shl 51) - 1L
    private const val MAX_AID = 1L shl 51

    private val REVERSE: Map<Char, Int> = TABLE.withIndex().associate { it.value to it.index }

    /** av 号转 BV 号。aid 需小于 2^51（B 站当前量级远远够用）。 */
    fun av2bv(aid: Long): String {
        val chars = "BV1000000000".toCharArray()
        var index = 11
        var tmp = (MAX_AID or aid) xor XOR_CODE
        while (tmp > 0) {
            chars[index] = TABLE[(tmp % 58L).toInt()]
            tmp /= 58L
            index--
        }
        swap(chars, 3, 9)
        swap(chars, 4, 7)
        return String(chars)
    }

    /** BV 号转 av 号；格式不合法时返回 null（播放页不需要为非法输入兜底）。 */
    fun bv2av(bvid: String): Long? {
        if (bvid.length != 12 || !bvid.startsWith("BV")) return null
        val chars = bvid.toCharArray()
        swap(chars, 3, 9)
        swap(chars, 4, 7)
        var tmp = 0L
        for (i in 3 until 12) {
            val value = REVERSE[chars[i]] ?: return null
            tmp = tmp * 58L + value
        }
        return (tmp and MASK_CODE) xor XOR_CODE
    }

    private fun swap(chars: CharArray, a: Int, b: Int) {
        val t = chars[a]
        chars[a] = chars[b]
        chars[b] = t
    }
}