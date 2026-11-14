package kr.unseen.dallineun.game

object Hangul {
    private const val BASE = 0xAC00
    private val CHO = listOf("ㄱ","ㄲ","ㄴ","ㄷ","ㄸ","ㄹ","ㅁ","ㅂ","ㅃ","ㅅ","ㅆ","ㅇ","ㅈ","ㅉ","ㅊ","ㅋ","ㅌ","ㅍ","ㅎ")
    private val CHO_NAME = mapOf(
        "ㄱ" to "기역", "ㄲ" to "쌍기역", "ㄴ" to "니은", "ㄷ" to "디귿", "ㄸ" to "쌍디귿", "ㄹ" to "리을",
        "ㅁ" to "미음", "ㅂ" to "비읍", "ㅃ" to "쌍비읍", "ㅅ" to "시옷", "ㅆ" to "쌍시옷", "ㅇ" to "이응",
        "ㅈ" to "지읒", "ㅉ" to "쌍지읒", "ㅊ" to "치읓", "ㅋ" to "키읔", "ㅌ" to "티읕", "ㅍ" to "피읖", "ㅎ" to "히읗",
    )

    fun isSyllable(c: Char) = c.code in 0xAC00..0xD7A3

    /** '로/으로' 조사: 받침 없거나 ㄹ받침이면 '로' */
    fun ro(c: Char): String {
        if (!isSyllable(c)) return "로"
        val jong = (c.code - BASE) % 28
        return if (jong == 0 || jong == 8) "로" else "으로"
    }

    private fun parts(c: Char): Triple<Int, Int, Int> {
        val code = c.code - BASE
        return Triple(code / (21 * 28), (code % (21 * 28)) / 28, code % 28)
    }
    private fun compose(cho: Int, jung: Int, jong: Int) = (BASE + (cho * 21 + jung) * 28 + jong).toChar()

    fun chosung(word: String) = word.filter(::isSyllable).map { CHO[parts(it).first] }.joinToString(" ")
    fun chosungSpoken(word: String) = word.filter(::isSyllable).map { CHO_NAME[CHO[parts(it).first]]!! }.joinToString(", ")

    /** 끝 글자로 시작할 수 있는 글자들 (두음법칙 포함) */
    fun allowedStarts(last: Char): Set<Char> {
        if (!isSyllable(last)) return setOf(last)
        val (cho, jung, jong) = parts(last)
        val set = mutableSetOf(last)
        val iotized = setOf(2, 6, 7, 12, 17, 20) // ㅑ ㅕ ㅖ ㅛ ㅠ ㅣ
        when (cho) {
            5 -> set += compose(if (jung in iotized) 11 else 2, jung, jong) // ㄹ -> ㅇ / ㄴ
            2 -> if (jung in iotized) set += compose(11, jung, jong)          // ㄴ -> ㅇ
        }
        return set
    }

    private val endings = listOf("입니다", "이요", "예요", "에요", "요")

    /** STT 결과에서 단어 하나를 뽑는다. "차표요!" -> "차표" */
    fun extractWord(raw: String): String {
        val first = raw.trim().split(Regex("\\s+")).firstOrNull().orEmpty().filter(::isSyllable)
        for (e in endings) if (first.length - e.length >= 2 && first.endsWith(e)) return first.dropLast(e.length)
        return first
    }

    fun normalize(raw: String) = raw.filter { isSyllable(it) || it.isLetterOrDigit() }

    fun parseCount(raw: String): Int? {
        val t = raw.replace(" ", "")
        Regex("[2-6]").find(t)?.let { return it.value.toInt() }
        val words = listOf("다섯" to 5, "여섯" to 6, "넷" to 4, "네명" to 4, "네사람" to 4, "셋" to 3, "세명" to 3,
            "세사람" to 3, "둘" to 2, "두명" to 2, "두사람" to 2, "네" to 4, "세" to 3, "두" to 2)
        return words.firstOrNull { t.contains(it.first) }?.second
    }
}
