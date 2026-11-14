package kr.unseen.dallineun

import kr.unseen.dallineun.game.Content
import kr.unseen.dallineun.game.Hangul
import org.junit.Assert.*
import org.junit.Test

class HangulTest {
    @Test fun dueum() {
        assertTrue('노' in Hangul.allowedStarts('로'))   // 등산로 -> 노란색
        assertTrue('이' in Hangul.allowedStarts('리'))
        assertTrue('여' in Hangul.allowedStarts('녀'))
        assertEquals(setOf('차'), Hangul.allowedStarts('차'))
    }
    @Test fun extract() {
        assertEquals("차표", Hangul.extractWord("차표요!"))
        assertEquals("형광등", Hangul.extractWord("형광등"))
        assertEquals("아빠", Hangul.extractWord("아빠"))
    }
    @Test fun count() {
        assertEquals(4, Hangul.parseCount("네 명이요"))
        assertEquals(3, Hangul.parseCount("세 명"))
        assertEquals(2, Hangul.parseCount("두 명"))
        assertEquals(5, Hangul.parseCount("5명"))
    }
    @Test fun chosung() {
        assertEquals("ㄱ ㅊ ㅉ ㄱ", Hangul.chosung("김치찌개"))
        assertEquals("기역, 치읓, 쌍지읒, 기역", Hangul.chosungSpoken("김치찌개"))
        assertEquals("으로", Hangul.ro('색')); assertEquals("로", Hangul.ro('차')); assertEquals("로", Hangul.ro('발'))
    }
    @Test fun game369() {
        assertEquals(1, Hangul.claps(3)); assertEquals(2, Hangul.claps(33)); assertEquals(0, Hangul.claps(10)); assertEquals(1, Hangul.claps(29))
        assertEquals(23, Hangul.parseNumber("이십삼")); assertEquals(11, Hangul.parseNumber("십일")); assertEquals(5, Hangul.parseNumber("오"))
        assertEquals(14, Hangul.parseNumber("14")); assertEquals(2, Hangul.parseNumber("이요")); assertTrue(Hangul.isClap("짝!"))
    }
    @Test fun choiceAndOx() {
        val opts = listOf("찹쌀떡", "호두과자", "붕어빵")
        assertEquals(1, Hangul.parseChoice("2번!", opts)); assertEquals(1, Hangul.parseChoice("호두과자요", opts)); assertEquals(2, Hangul.parseChoice("세 번째", opts))
        assertEquals(true, Hangul.parseOX("오!")); assertEquals(false, Hangul.parseOX("엑스")); assertEquals(false, Hangul.parseOX("아니오")); assertEquals(true, Hangul.parseOX("맞아"))
    }
    @Test fun region() {
        assertEquals("천안", Content.findRegion("충청남도 천안시 동남구", null, null)?.name)
        assertTrue(Content.regions.all { r -> r.qs.all { q -> q !is Content.Quiz.Choice || q.options.size == 3 } })
        assertEquals("천안", Content.findRegion(null, 36.80, 127.15)?.name)
        assertNull(Content.findRegion(null, 33.0, 124.0))
    }
}
