package com.otpbox.domain

import java.security.SecureRandom

object PasswordGenerator {

    data class Config(
        val length: Int = 16,
        val useUpper: Boolean = true,
        val useLower: Boolean = true,
        val useDigits: Boolean = true,
        val useSymbols: Boolean = true,
        val excludeAmbiguous: Boolean = false
    )

    sealed interface Result {
        data class Success(val password: String) : Result
        data class Error(val message: String) : Result
    }

    private val UPPER = "ABCDEFGHJKLMNPQRSTUVWXYZ"
    private val LOWER = "abcdefghijkmnopqrstuvwxyz"
    private val DIGITS = "23456789"
    private val SYMBOLS = "!@#\$%^&*()-_=+[]{};:,.<>?"
    private val AMBIGUOUS = setOf('O', '0', 'l', '1', 'I', '|')

    private val random = SecureRandom()

    fun generate(config: Config = Config()): Result {
        val length = config.length.coerceIn(8, 64)
        var pool = ""
        val guaranteed = mutableListOf<Char>()

        fun add(set: String, enabled: Boolean) {
            if (!enabled) return
            val usable = if (config.excludeAmbiguous) set.filter { it !in AMBIGUOUS } else set
            if (usable.isEmpty()) return
            pool += usable
            guaranteed += usable[random.nextInt(usable.length)]
        }
        add(UPPER, config.useUpper)
        add(LOWER, config.useLower)
        add(DIGITS, config.useDigits)
        add(SYMBOLS, config.useSymbols)

        if (pool.isEmpty()) {
            return Result.Error("请至少启用一种字符类型")
        }
        if (guaranteed.size > length) {
            return Result.Error("密码长度不足以包含所选字符类型")
        }

        val sb = StringBuilder()
        guaranteed.forEach { sb.append(it) }
        while (sb.length < length) {
            sb.append(pool[random.nextInt(pool.length)])
        }
        return Result.Success(sb.toString().toCharArray().apply { shuffle() }.concatToString())
    }

    private fun CharArray.shuffle() {
        for (i in lastIndex downTo 1) {
            val j = random.nextInt(i + 1)
            val t = this[i]; this[i] = this[j]; this[j] = t
        }
    }
}
