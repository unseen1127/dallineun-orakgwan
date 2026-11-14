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

    private val sinoDigit = listOf("", "일", "이", "삼", "사", "오", "육", "칠", "팔", "구")

    /** 1~99를 한자어 수로: 9 -> 구, 23 -> 이십삼 */
    fun sino(n: Int): String {
        if (n !in 1..99) return n.toString()
        val t = n / 10; val o = n % 10
        val tens = when (t) { 0 -> ""; 1 -> "십"; else -> sinoDigit[t] + "십" }
        return tens + sinoDigit[o]
    }

    /** 화면 문구를 TTS가 자연스럽게 읽도록 바꾼다. OX -> 오엑스, 369 -> 삼육구, 3글자 -> 세 글자 */
    fun spoken(text: String): String = text
        .replace("OX", "오엑스")
        .replace("369", "삼육구")
        .replace("3, 6, 9", "삼, 육, 구")
        .replace(Regex("(?<![A-Za-z])O(?![A-Za-z])"), "오")
        .replace(Regex("(?<![A-Za-z])X(?![A-Za-z])"), "엑스")
        .replace(Regex("(?<!\\d)([1-4])글자")) { listOf("", "한", "두", "세", "네")[it.groupValues[1].toInt()] + " 글자" }
        .replace(Regex("(?<!\\d)([1-3])번")) { sinoDigit[it.groupValues[1].toInt()] + " 번" }

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

    /* ---------- 369 게임 ---------- */

    /** 숫자에 들어 있는 3·6·9의 개수 (박수 횟수) */
    fun claps(n: Int) = n.toString().count { it == '3' || it == '6' || it == '9' }

    private val digitKo = mapOf('일' to 1, '이' to 2, '삼' to 3, '사' to 4, '오' to 5, '육' to 6, '륙' to 6, '칠' to 7, '팔' to 8, '구' to 9)

    /** "23", "이십삼", "십일" 같은 답을 숫자로. 못 읽으면 null */
    fun parseNumber(raw: String): Int? {
        val t = raw.replace(" ", "").trimEnd('!', '.', '?', '요')
        Regex("\\d+").find(t)?.let { return it.value.toInt() }
        val s = t.filter { it in digitKo.keys || it == '십' }
        if (s.isEmpty()) return null
        val i = s.indexOf('십')
        if (i < 0) return if (s.length == 1) digitKo[s[0]] else null
        val tens = if (i == 0) 1 else digitKo[s[i - 1]] ?: return null
        val ones = if (i == s.lastIndex) 0 else digitKo[s[i + 1]] ?: return null
        return tens * 10 + ones
    }

    fun isClap(raw: String) = raw.contains("짝") || raw.contains("박수")

    /* ---------- 객관식 / OX ---------- */

    /** "2번", "이 번", "두 번째", 보기 텍스트 → 보기 인덱스 */
    fun parseChoice(raw: String, options: List<String>): Int? {
        val t = raw.replace(" ", "")
        options.forEachIndexed { i, o -> if (t.contains(o.replace(" ", ""))) return i }
        val nums = listOf(listOf("1번", "일번", "첫번째", "첫째", "하나"), listOf("2번", "이번", "두번째", "둘째", "둘"), listOf("3번", "삼번", "세번째", "셋째", "셋"))
        nums.forEachIndexed { i, keys -> if (i < options.size && keys.any { t.contains(it) }) return i }
        return null
    }

    /** O / X 답 인식. 음성 인식이 "오"를 숫자 5로 돌려주는 경우도 O로 본다 */
    fun parseOX(raw: String): Boolean? {
        val t = raw.replace(" ", "").uppercase()
        if (listOf("엑스", "X", "틀려", "틀리", "틀렸", "아니", "땡", "가위").any { t.contains(it) }) return false
        if (listOf("오", "O", "5", "맞아", "맞다", "맞습", "동그라미", "응").any { t.contains(it) }) return true
        return null
    }

    fun parseCount(raw: String): Int? {
        val t = raw.replace(" ", "")
        Regex("[2-6]").find(t)?.let { return it.value.toInt() }
        val words = listOf("다섯" to 5, "여섯" to 6, "넷" to 4, "네명" to 4, "네사람" to 4, "셋" to 3, "세명" to 3,
            "세사람" to 3, "둘" to 2, "두명" to 2, "두사람" to 2, "네" to 4, "세" to 3, "두" to 2)
        return words.firstOrNull { t.contains(it.first) }?.second
    }
}
