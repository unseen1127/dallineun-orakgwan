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
    @Test fun spokenText() {
        assertEquals("오엑스 퀴즈! 맞으면 오, 틀리면 엑스!", Hangul.spoken("OX 퀴즈! 맞으면 O, 틀리면 X!"))
        assertEquals("삼육구 게임! 삼, 육, 구가 들어가면", Hangul.spoken("369 게임! 3, 6, 9가 들어가면"))
        assertEquals("이제부터 세 글자 단어만!", Hangul.spoken("이제부터 3글자 단어만!"))
        assertEquals("일 번 찹쌀떡, 이 번 호두과자", Hangul.spoken("1번 찹쌀떡, 2번 호두과자"))
        assertEquals("21번", Hangul.spoken("21번")); assertEquals("네 명! 세 명", Hangul.spoken("4명! 3명")); assertEquals("스무", Hangul.native(20))
        assertEquals("구", Hangul.sino(9)); assertEquals("이십삼", Hangul.sino(23)); assertEquals("십", Hangul.sino(10))
    }
    @Test fun clap() {
        assertTrue(kr.unseen.dallineun.voice.ClapDetector.isClap(listOf(1.0, 1.2, 0.9, 1.1, 9.5, 1.0, 1.1)))
        assertFalse(kr.unseen.dallineun.voice.ClapDetector.isClap(listOf(1.0, 1.2, 0.9, 1.1, 1.3, 1.0)))
        assertFalse(kr.unseen.dallineun.voice.ClapDetector.isClap(listOf(1.0, 9.0, 9.2, 9.5, 9.1, 9.3, 8.9, 1.0))) // 길게 말한 소리는 박수 아님
    }
    @Test fun soloWords() {
        // 시연 대본: 자동차 -> (차표) -> 표정 -> (정류장) -> 장갑 -> (갑옷) -> '옷' 단어 없음
        assertEquals("표정", Content.mcWords.first { it.first() in Hangul.allowedStarts('표') })
        assertEquals("장갑", Content.mcWords.first { it.first() in Hangul.allowedStarts('장') })
        assertTrue(Content.mcWords.none { it.first() in Hangul.allowedStarts('옷') })
        assertEquals(Content.mcWords.size, Content.mcWords.toSet().size)
    }
    @Test fun twentyQuestions() {
        val giraffe = Content.twenty.first()
        fun ans(q: String) = Content.twentyKeys.firstOrNull { it.first.containsMatchIn(q.replace(" ", "")) }?.second?.let { it in giraffe.facts }
        assertEquals(true, ans("동물이야?")); assertEquals(false, ans("바다에 살아?")); assertEquals(true, ans("다리가 네 개야?"))
        assertEquals(true, ans("사람보다 커?")); assertEquals(true, ans("목이 길어?")); assertEquals(false, ans("먹을 수 있어?"))
        assertTrue(Content.twenty.any { t -> t.aliases.any { "기린!".contains(it) } })
    }
    @Test fun region() {
        assertEquals("천안", Content.findRegion("충청남도 천안시 동남구", null, null)?.name)
        assertTrue(Content.regions.all { r -> r.qs.all { q -> q !is Content.Quiz.Choice || q.options.size == 3 } })
        assertEquals("천안", Content.findRegion(null, 36.80, 127.15)?.name)
        assertNull(Content.findRegion(null, 33.0, 124.0))
    }
}
