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

    private fun run(type: GameType, address: String, lat: Double, lng: Double): Pair<List<String>, List<UiState>> {
        val log = mutableListOf<String>()
        val states = mutableListOf<UiState>()
        runTest {
            val car = FakeCar(CarContext.State(driving = true, remainSec = 80 * 60, address = address, lat = lat, lng = lng))
            val mc = MC(this, car, { states += it })
            val demo = DemoVoice(FakeVoice(log)) { a -> log += "답변: $a" }
            demo.load(DemoScripts.forGame(type))
            mc.voice = demo; mc.demo = true
            mc.start(type, DemoScripts.players(type))
            advanceUntilIdle()
        }
        File("build/sim-$type.txt").writeText(log.joinToString("\n"))
        return log to states
    }

    @Test fun wordChain() {
        val (log, states) = run(GameType.WORD_CHAIN, "충청남도 천안시 동남구", 36.80, 127.15)
        val end = states.last()
        assertEquals(UiState.Screen.RESULT, end.screen)
        assertEquals("아빠", end.players.first().name); assertEquals(4, end.players.first().score)
        assertTrue(log.any { it.contains("새 규칙") }); assertTrue(log.any { it.contains("세 글자 규칙") }); assertTrue(log.any { it.contains("네 명!") })
        assertTrue(log.any { it.contains("'로' 또는 '노'") })
    }

    @Test fun chosung() {
        val (log, states) = run(GameType.CHOSUNG, "충청남도 천안시 동남구", 36.80, 127.15)
        val end = states.last()
        assertEquals(UiState.Screen.RESULT, end.screen)
        assertEquals(listOf("아빠" to 4, "지우" to 2), end.players.map { it.name to it.score })
        assertTrue(log.any { it.contains("힌트") })
    }

    @Test fun region() {
        val (log, states) = run(GameType.REGION, "충청남도 천안시 동남구", 36.80, 127.15)
        val end = states.last()
        assertEquals(UiState.Screen.RESULT, end.screen)
        assertEquals("엄마", end.players.first().name); assertEquals(2, end.players.first().score)
        assertTrue(log.any { it.contains("천안 근처") }); assertTrue(log.any { it.contains("오엑스 퀴즈") }); assertTrue(log.none { it.contains("OX") }); assertTrue(log.any { it.contains("객관식") })
    }

    @Test fun soloTwentyThenAjae() {
        val (log, states) = run(GameType.SOLO, "대전광역시 유성구", 36.36, 127.36)
        val end = states.last()
        assertEquals(UiState.Screen.RESULT, end.screen)
        assertTrue(log.any { it.contains("정답! 기린") })
        assertTrue(log.any { it.contains("삼 번 아재개그 퀴즈") }); assertTrue(log.any { it.contains("네 번 만에") })
        assertTrue(log.any { it.contains("카톡") })
        assertEquals("21점", end.board)
    }
}
