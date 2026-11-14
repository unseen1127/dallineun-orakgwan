package kr.unseen.dallineun

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kr.unseen.dallineun.car.CarContext
import kr.unseen.dallineun.car.CarEnv
import kr.unseen.dallineun.game.GameType
import kr.unseen.dallineun.game.MC
import kr.unseen.dallineun.game.UiState
import kr.unseen.dallineun.voice.DemoVoice
import kr.unseen.dallineun.voice.VoiceIO
import org.junit.Assert.*
import org.junit.Test
import java.io.File

/**
 * 실제 MC 엔진 코드를 시연 대본으로 처음부터 끝까지 돌려 보는 시뮬레이션.
 * 음성·차량은 가짜로 대체하고, 진행 멘트와 화면 상태를 기록해 검증한다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class GameFlowSimulationTest {

    private class FakeVoice(val log: MutableList<String>) : VoiceIO {
        override val engineName = "fake"
        override suspend fun speak(text: String) { log += "MC  : $text" }
        override suspend fun listen(timeoutMs: Long): String? = null
        override fun stopAll() = Unit
        override fun release() = Unit
    }

    private class FakeCar(override val state: CarContext.State) : CarEnv {
        override fun requestAudioFocus() = true
        override fun abandonAudioFocus() = Unit
        override fun refreshLocation() = Unit
    }

    private fun run(name: String, script: List<String?>, names: List<String>, address: String, lat: Double, lng: Double): Pair<List<String>, List<UiState>> {
        val log = mutableListOf<String>()
        val states = mutableListOf<UiState>()
        runTest {
            val car = FakeCar(CarContext.State(driving = true, remainSec = 80 * 60, address = address, lat = lat, lng = lng))
            val mc = MC(this, car, { states += it })
            val demo = DemoVoice(FakeVoice(log)) { a -> log += "답변: $a" }
            demo.load(script)
            mc.voice = demo; mc.demo = true
            mc.startSession(names)
            advanceUntilIdle()
        }
        File("build/sim-$name.txt").writeText(log.joinToString("\n"))
        return log to states
    }

    @Test fun familyWordChain() {
        val (log, states) = run("FAMILY_WORD_CHAIN", DemoScripts.family(GameType.WORD_CHAIN), DemoScripts.players(GameType.WORD_CHAIN), "충청남도 천안시 동남구", 36.80, 127.15)
        assertTrue(log.first { it.startsWith("MC") }.contains("반가워요"))
        assertTrue(log.any { it.contains("몇 명이 게임하나요") })
        assertTrue(log.any { it.contains("네 명이군요! 함께 할 수 있는 게임은 일 번 끝말잇기, 이 번 초성 퀴즈, 삼 번 지나는 동네 퀴즈") })
        assertTrue(log.any { it.contains("세 글자 규칙") }); assertTrue(log.any { it.contains("'로' 또는 '노'") })
        assertTrue(log.any { it.contains("우승은 아빠 님, 4점") })
        assertTrue(log.any { it.contains("한 판 더 할까요") }); assertTrue(log.last().contains("오늘 게임은 여기까지"))
        assertTrue(states.any { it.options.contains("1명 (혼자)") })
    }

    @Test fun familyChosung() {
        val (log, _) = run("FAMILY_CHOSUNG", DemoScripts.family(GameType.CHOSUNG), DemoScripts.players(GameType.CHOSUNG), "충청남도 천안시 동남구", 36.80, 127.15)
        assertTrue(log.any { it.contains("두 명이군요") }); assertTrue(log.any { it.contains("힌트") })
        assertTrue(log.any { it.contains("우승은 아빠 님, 4점") }); assertTrue(log.last().contains("오늘 게임은 여기까지"))
    }

    @Test fun familyRegion() {
        val (log, _) = run("FAMILY_REGION", DemoScripts.family(GameType.REGION), DemoScripts.players(GameType.REGION), "충청남도 천안시 동남구", 36.80, 127.15)
        assertTrue(log.any { it.contains("세 명이군요") }); assertTrue(log.any { it.contains("천안 근처") }); assertTrue(log.any { it.contains("한 분씩 이름") })
        assertTrue(log.any { it.contains("오엑스 퀴즈") }); assertTrue(log.none { it.contains("OX") })
        assertTrue(log.any { it.contains("우승은 엄마 님, 이 점") || it.contains("우승은 엄마 님, 2점") })
    }

    @Test fun soloSession() {
        val (log, states) = run("SOLO", DemoScripts.solo, listOf("아빠"), "대전광역시 유성구", 36.36, 127.36)
        assertTrue(log.any { it.contains("혼자서 운전하시는군요! 지루하지 않으시도록 저랑 같이 게임을 해 봐요. 혼자서 할 수 있는 게임은 일 번 스무고개, 이 번 초성 퀴즈, 삼 번 지나는 동네 퀴즈, 사 번 아재개그 퀴즈") })
        assertTrue(log.any { it.contains("정답! 기린! 네 번 만에") })
        assertTrue(log.any { it.contains("아재개그 퀴즈 시작") }); assertTrue(log.any { it.contains("카톡") })
        assertEquals("21점", states.last().board)
        assertTrue(log.last().contains("오늘 기록 21점"))
    }
}
