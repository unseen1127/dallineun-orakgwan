package kr.unseen.dallineun.game

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kr.unseen.dallineun.car.CarContext
import kr.unseen.dallineun.voice.VoiceIO

enum class GameType(val title: String, val desc: String) {
    WORD_CHAIN("끝말잇기", "MC가 순서·시간·판정, 중간에 새 규칙"),
    CHOSUNG("초성 퀴즈", "초성과 힌트를 듣고 정답 맞히기"),
    REGION("지나는 동네 퀴즈", "지나는 지역 문제를 객관식·OX로"),
    SOLO("혼자 드라이브", "혼자 운전할 때, MC와 스무고개·아재개그 퀴즈"),
}

data class Player(val name: String, val isDriver: Boolean, var score: Int = 0)

data class UiState(
    val screen: Screen = Screen.HOME,
    val game: GameType? = null,
    val mcLine: String = "",
    val heard: String = "",
    val players: List<Player> = emptyList(),
    val current: Int = -1,
    val timerLeft: Int = 0,
    val timerTotal: Int = 0,
    val rule: String = "",
    val board: String = "",
    val boardLabel: String = "",
    val listening: Boolean = false,
    val round: String = "",
    val options: List<String> = emptyList(),
    val okIdx: Int? = null,
) { enum class Screen { HOME, GAME, RESULT } }

/**
 * 사회자(MC) 엔진. 화면 없이 음성만으로 인원 등록, 순서, 타이머, 판정, 규칙 변주, 시상까지 진행한다.
 */
class MC(
    private val scope: CoroutineScope,
    private val car: CarContext,
    private val render: (UiState) -> Unit,
) {
    lateinit var voice: VoiceIO
    /** 시연 모드: 시작 단어·규칙·순서를 고정해 대본과 맞춘다 */
    var demo = false
    private var ui = UiState()
    private var job: Job? = null
    @Volatile var paused = false

    private fun set(f: (UiState) -> UiState) { ui = f(ui); render(ui) }

    val isRunning get() = job?.isActive == true

    fun stop() {
        job?.cancel(); voice.stopAll()
        set { UiState() }
    }

    fun start(game: GameType, demoPlayers: List<String>? = null) {
        job?.cancel()
        job = scope.launch {
            car.requestAudioFocus()
            set { UiState(screen = UiState.Screen.GAME, game = game) }
            val players = if (game == GameType.SOLO) mutableListOf(Player(demoPlayers?.firstOrNull() ?: "드라이버", isDriver = true))
                          else setupPlayers(game, demoPlayers)
            when (game) {
                GameType.WORD_CHAIN -> wordChain(players)
                GameType.CHOSUNG -> chosung(players)
                GameType.SOLO -> { solo(players[0]); car.abandonAudioFocus(); return@launch }
                GameType.REGION -> region(players)
            }
            awards(players)
            car.abandonAudioFocus()
        }
    }

    /* ---------------- 공통 ---------------- */

    private suspend fun say(text: String) {
        waitIfPaused()
        set { it.copy(mcLine = text, listening = false) }
        voice.speak(Hangul.spoken(text))
    }

    private suspend fun waitIfPaused() {
        while (paused || car.state.audioPaused) delay(300)
    }

    /** 운전자 차례이고 주행 중이면 생각할 시간을 더 준다 */
    private fun turnSeconds(p: Player?, base: Int) =
        if (p?.isDriver == true && car.state.driving) base + 4 else base

    private suspend fun hear(player: Player?, seconds: Int): String? {
        waitIfPaused()
        set { it.copy(listening = true, timerLeft = seconds, timerTotal = seconds) }
        val ticker = scope.launch {
            var left = seconds
            while (isActive && left > 0) { delay(1000); left--; set { it.copy(timerLeft = left) } }
        }
        val r = voice.listen(seconds * 1000L)
        ticker.cancel()
        val who = player?.name ?: "탑승자"
        set { it.copy(listening = false, heard = when { r != null -> "$who: $r"; voice.clapDetected -> "$who: 짝! (박수)"; else -> "$who: (시간 초과)" }) }
        return r
    }

    private fun isStop(t: String?) = t != null && listOf("그만", "종료", "끝내").any { t.contains(it) }

    private fun remainMin() = car.state.remainSec?.let { it / 60 }

    private suspend fun setupPlayers(game: GameType, demoNames: List<String>?): MutableList<Player> {
        say("반가워요! 달리는 오락관 MC입니다. 오늘의 게임은 ${game.title}!")
        remainMin()?.let { m ->
            if (m > 0) say("목적지까지 약 ${if (m >= 60) "${m / 60}시간 ${m % 60}분" else "${m}분"} 남았네요. 도착 전에 시상식까지 해 볼게요.")
        }
        var count: Int? = null
        repeat(2) {
            if (count != null) return@repeat
            say("몇 명이 참여하나요?")
            count = hear(null, 6)?.let { Hangul.parseCount(it) }
        }
        val n = (count ?: 2).coerceIn(2, 6)
        say("${n}명! 운전하시는 분부터 이름을 말해 주세요.")
        val players = mutableListOf<Player>()
        for (i in 0 until n) {
            val label = if (i == 0) "운전자" else "${i + 1}번째"
            set { it.copy(players = players.toList(), current = i, rule = "이름 등록 · $label") }
            if (i > 0) say("다음 분!")
            val raw = hear(null, 5)
            val name = raw?.let { Hangul.extractWord(it) }?.takeIf { it.isNotBlank() }
                ?: demoNames?.getOrNull(i) ?: "${i + 1}번 플레이어"
            players += Player(name, isDriver = i == 0)
            set { it.copy(players = players.toList()) }
        }
        say("${players.joinToString(", ") { it.name + " 님" }}, 반가워요! ${players[0].name} 님은 운전 중이니 시간을 조금 더 드릴게요.")
        say("순서는 말씀하신 대로 할까요, 제가 섞을까요?")
        val ans = hear(null, 5)
        if (ans != null && listOf("섞", "랜덤", "아무", "네가", "니가", "알아서").any { ans.contains(it) }) {
            if (demo) players.reverse() else players.shuffle()
            set { it.copy(players = players.toList()) }
            say("섞었어요! 순서는 ${players.joinToString(", ") { it.name }} 순입니다.")
        } else {
            say("좋아요. ${players.joinToString(", ") { it.name }} 순서로 갈게요.")
        }
        return players
    }

    /* ---------------- 끝말잇기 ---------------- */

    private sealed class Rule(val label: String, val announce: String) {
        object None : Rule("기본 규칙", "")
        class Length(val n: Int) : Rule("${n}글자 단어만", "이제부터 ${n}글자 단어만!")
        class StartWith(val c: Char) : Rule("'$c'${Hangul.ro(c)} 시작", "분위기 바꿔서, 다음 사람은 '$c'${Hangul.ro(c)} 시작하는 단어!")
        class Speed : Rule("스피드 라운드 4초", "스피드 라운드! 지금부터 4초 안에 답해 주세요.")
    }

    private suspend fun wordChain(players: MutableList<Player>) {
        val used = mutableSetOf<String>()
        var word = if (demo) Content.startWords.first() else Content.startWords.random()
        var rule: Rule = Rule.None
        var successInRow = 0
        var misses = 0
        val maxMisses = 3
        var idx = 0
        var praise = ""
        used += word
        set { it.copy(board = word, boardLabel = "지금 단어", rule = rule.label, round = "탈락 0/$maxMisses") }
        say("끝말잇기 시작! 첫 단어는 '$word'.")
        while (misses < maxMisses) {
            val p = players[idx % players.size]
            set { it.copy(current = players.indexOf(p)) }
            val starts = when (val r = rule) {
                is Rule.StartWith -> setOf(r.c)
                else -> Hangul.allowedStarts(word.last())
            }
            val startTxt = starts.joinToString(" 또는 ") { "'$it'" }
            val startRo = startTxt + Hangul.ro(starts.last())
            say("$praise${p.name} 님, $startRo 시작!".trim()); praise = ""
            val base = if (rule is Rule.Speed) 4 else 8
            val heard = hear(p, turnSeconds(p, base))
            if (isStop(heard)) { say("게임을 마칠게요."); return }
            val w = heard?.let { Hangul.extractWord(it) }.orEmpty()
            val fail: String? = when {
                heard == null || w.isEmpty() -> "시간 초과!"
                w.length < 2 -> "두 글자 이상이어야 해요."
                w.first() !in starts -> "$startRo 시작해야 해요."
                w in used -> "'$w'는 이미 나온 단어예요."
                rule is Rule.Length && w.length != (rule as Rule.Length).n -> "${(rule as Rule.Length).n}글자 규칙이에요!"
                else -> null
            }
            if (fail == null) {
                p.score += 1; used += w; word = w; successInRow++
                set { it.copy(board = w, players = players.toList()) }
                if (rule is Rule.StartWith) rule = Rule.None
                praise = listOf("좋아요! ", "인정! ", "오, 좋은데요? ", "통과! ")[(used.size) % 4]
                // 규칙 변주: 4번 연속 성공하면 새 규칙 제안
                if (successInRow >= 4) {
                    successInRow = 0
                    rule = pickRule(word)
                    set { it.copy(rule = rule.label) }
                    if (rule is Rule.StartWith) set { it.copy(board = "${(rule as Rule.StartWith).c}…") }
                    say("${praise}잠깐, 새 규칙! ${rule.announce}"); praise = ""
                }
                idx++
            } else {
                misses++
                set { it.copy(round = "탈락 $misses/$maxMisses") }
                say("아쉬워요! $fail ${p.name} 님 이번 판 탈락.")
                if (misses >= maxMisses) break
                if (finalRoundNeeded()) { say("곧 도착이라 여기까지! 마지막 판이었습니다."); break }
                val pool = Content.startWords.filter { it !in used }
                word = (if (demo) pool.firstOrNull() else pool.randomOrNull()) ?: "기차"
                used += word; rule = Rule.None; successInRow = 0
                set { it.copy(board = word, rule = rule.label) }
                say("다시 시작! 새 단어는 '$word'.")
                idx++
            }
        }
    }

    private var demoRuleIdx = 0
    private fun pickRule(current: String): Rule {
        if (demo) return listOf<Rule>(Rule.Length(3), Rule.StartWith('가'), Rule.Speed())[demoRuleIdx++ % 3]
        val options = mutableListOf<Rule>(Rule.Length(3), Rule.Speed(), Rule.StartWith(listOf('가', '사', '바', '자', '하').random()))
        if (current.length == 2) options += Rule.Length(2)
        return options.random()
    }

    private fun finalRoundNeeded() = car.state.remainSec?.let { it in 1..600 } == true

    /* ---------------- 초성 퀴즈 ---------------- */

    private suspend fun chosung(players: MutableList<Player>) {
        val qs = (if (demo) Content.chosungQuiz else Content.chosungQuiz.shuffled()).take(players.size * 2)
        say("초성 퀴즈! 제가 초성과 분야를 알려 드리면 차례대로 맞혀 주세요. 맞히면 2점, 힌트 듣고 맞히면 1점!")
        for ((i, q) in qs.withIndex()) {
            val p = players[i % players.size]
            set { it.copy(current = players.indexOf(p), board = Hangul.chosung(q.answer), boardLabel = "분야 · ${q.category}", rule = "${i + 1} / ${qs.size} 문제", round = "") }
            say("${p.name} 님 문제! 분야는 ${q.category}. 초성은 ${Hangul.chosungSpoken(q.answer)}.")
            var a = hear(p, turnSeconds(p, 10))
            if (isStop(a)) { say("게임을 마칠게요."); return }
            if (correct(a, listOf(q.answer))) {
                p.score += 2; set { it.copy(players = players.toList(), board = q.answer) }
                say("정답! ${q.answer}. 2점!")
            } else {
                say("힌트 갑니다. ${q.hint}.")
                a = hear(p, turnSeconds(p, 8))
                if (correct(a, listOf(q.answer))) { p.score += 1; say("정답! ${q.answer}. 1점!") }
                else say("아쉬워요. 정답은 ${q.answer}였어요.")
                set { it.copy(players = players.toList(), board = q.answer) }
            }
            if (finalRoundNeeded()) { say("곧 도착이에요. 여기까지!"); return }
        }
    }

    /* ---------------- 혼자 드라이브: MC가 상대 ---------------- */

    private suspend fun solo(me: Player) {
        val mcSide = Player("MC", isDriver = false)
        val both = listOf(me, mcSide)
        set { it.copy(players = both, current = -1, rule = "1:1 대결", board = "?", boardLabel = "MC가 생각한 것", round = "스무고개") }
        say("혼자 드라이브 모드! 오늘은 제가 문제를 낼게요. 스무고개!")
        var round = 0
        while (true) {
            twentyRound(me, mcSide, both, round++)
            // 한 판 더? 다른 게임?
            set { it.copy(current = 0, options = emptyList()) }
            say("한 판 더 할까요, 아니면 다른 게임을 해 볼까요?")
            val ans = hear(me, turnSeconds(me, 8)).orEmpty().replace(" ", "")
            if (isStop(ans)) { say("오늘은 여기까지! 안전 운전하세요."); return }
            if (listOf("한판", "또", "다시", "한번더").any { ans.contains(it) } && !ans.contains("다른")) continue
            break
        }
        // 다른 게임 안내: 화면에 목록을 보여 주고 음성으로 읽어 준다
        val menu = listOf("초성 퀴즈", "지나는 동네 퀴즈", "아재개그 퀴즈")
        set { it.copy(board = "", boardLabel = "혼자서 할 수 있는 게임", round = "다른 게임", options = menu, current = 0) }
        say("혼자서도 할 수 있는 게임은 1번 초성 퀴즈, 2번 지나는 동네 퀴즈, 3번 아재개그 퀴즈가 있어요. 뭘로 할까요?")
        val pick = hear(me, turnSeconds(me, 8)).orEmpty()
        val idx = Hangul.parseChoice(pick, menu) ?: when {
            pick.contains("아재") || pick.contains("개그") -> 2
            pick.contains("초성") -> 0
            pick.contains("동네") || pick.contains("지역") -> 1
            else -> 2
        }
        set { it.copy(okIdx = idx) }
        say("좋아요! ${menu[idx]} 시작합니다!")
        set { it.copy(okIdx = null, options = emptyList()) }
        when (idx) {
            0 -> chosung(mutableListOf(me))
            1 -> region(mutableListOf(me))
            else -> ajae(me)
        }
        set { it.copy(screen = UiState.Screen.RESULT, current = -1, board = "${me.score}점", boardLabel = "오늘의 기록", options = emptyList(), round = "") }
        say("오늘 기록 ${me.score}점! 졸리면 다음 휴게소에서 꼭 쉬어 가세요.")
    }

    /** 스무고개 한 판: MC가 정답을 정하고 운전자가 예/아니요 질문으로 맞힌다 */
    private suspend fun twentyRound(me: Player, mcSide: Player, both: List<Player>, round: Int) {
        val item = if (demo) Content.twenty[round % Content.twenty.size] else Content.twenty.random()
        set { it.copy(round = "스무고개", board = "?", boardLabel = "MC가 생각한 것", options = emptyList()) }
        say(if (round == 0) "제가 생각한 것을 예, 아니요로 답할 수 있는 질문으로 맞혀 보세요. 정답을 알면 이름을 말하면 돼요. 시작!" else "새 문제! 생각했어요. 질문하세요!")
        var asked = 0
        var hintIdx = 0
        while (asked < 20) {
            set { it.copy(current = 0, board = "?", boardLabel = "질문 ${asked + 1} / 20") }
            val heard = hear(me, turnSeconds(me, 10))
            if (isStop(heard)) return
            if (heard == null) { say("질문해 주세요! 예를 들면, 동물이야?"); continue }
            val q = heard.replace(" ", "")
            val guessed = Content.twenty.firstOrNull { t -> t.aliases.any { q.contains(it) } }
            asked++
            if (guessed != null) {
                if (guessed.name == item.name) {
                    me.score += maxOf(1, 21 - asked)
                    set { it.copy(board = item.name, boardLabel = "정답", players = both) }
                    say("정답! ${item.name}! ${Hangul.sino(asked)} 번 만에 맞히셨어요!")
                    return
                } else { say("아니요, ${guessed.name}은 아니에요!"); continue }
            }
            if (q.contains("힌트")) { say("힌트! ${item.hints.getOrNull(hintIdx++) ?: "힌트를 다 드렸어요"}."); continue }
            val key = Content.twentyKeys.firstOrNull { it.first.containsMatchIn(q) }?.second
            if (key == null) { asked--; say("음, 예 아니요로 답하기 어려운 질문이에요. 다른 질문!"); continue }
            val yes = key in item.facts
            say(if (yes) listOf("네!", "네, 맞아요!", "딩동! 네!")[asked % 3] else listOf("아니요!", "아니에요!", "땡, 아니요!")[asked % 3])
            if (asked == 10) say("벌써 질문 열 개! 힌트 하나 드릴게요. ${item.hints.first()}.")
            if (finalRoundNeeded()) { say("곧 도착이라 정답 공개! ${item.name}였어요."); return }
        }
        mcSide.score += 1
        say("스무 고개 끝! 정답은 ${item.name}였어요.")
    }

    /** 아재개그 퀴즈 */
    private suspend fun ajae(me: Player) {
        val list = (if (demo) Content.ajae else Content.ajae.shuffled()).take(3)
        for ((i, q) in list.withIndex()) {
            set { it.copy(game = it.game, round = "아재개그 퀴즈", board = q.q, boardLabel = "${i + 1} / ${list.size} 문제", current = 0) }
            say(if (i == 0) "첫 문제! ${q.q}" else "다음 문제! ${q.q}")
            val a = hear(me, turnSeconds(me, 10))
            if (isStop(a)) return
            if (correct(a, q.answers)) { me.score += 2; say("딩동댕! ${q.reveal}") } else say("땡! ${q.reveal}")
        }
    }

    /* ---------------- 지나는 동네 퀴즈 (객관식 · OX) ---------------- */

    private suspend fun region(players: MutableList<Player>) {
        car.refreshLocation(); delay(600)
        val s = car.state
        val reg = Content.findRegion(s.address, s.lat, s.lng)
        val pool = if (reg != null) {
            say("지금 ${reg.name} 근처를 지나고 있어요! ${reg.name} 퀴즈, 객관식과 OX로 나갑니다.")
            reg.qs + (if (demo) Content.nationwide else Content.nationwide.shuffled())
        } else {
            say("위치를 확인하지 못해서 전국 퀴즈로 진행할게요.")
            if (demo) Content.nationwide else Content.nationwide.shuffled()
        }
        val list = pool.take(maxOf(players.size, 3))
        for ((i, q) in list.withIndex()) {
            val p = players[i % players.size]
            val head = "${i + 1} / ${list.size} 문제"
            askQuiz(p, q, reg?.name, head)
        }
    }

    /** 객관식·OX 한 문제 진행. 맞히면 true */
    private suspend fun askQuiz(p: Player, q: Content.Quiz, regionName: String?, head: String = ""): Boolean {
        val players = ui.players
        when (q) {
            is Content.Quiz.OX -> {
                set { it.copy(current = players.indexOf(p), board = "O / X", boardLabel = "${regionName ?: "전국"} · OX 퀴즈", rule = head, options = emptyList()) }
                say("${p.name} 님, OX 퀴즈! ${q.q} 맞으면 O, 틀리면 X!")
                val a = hear(p, turnSeconds(p, 6))
                val ok = a?.let { Hangul.parseOX(it) } == q.answer
                if (ok) p.score += 1
                set { it.copy(board = if (q.answer) "O" else "X", players = ui.players.toList()) }
                say((if (ok) "딩동댕! " else "땡! ") + q.explain)
                return ok
            }
            is Content.Quiz.Choice -> {
                val order = if (demo) q.options.indices.toList().let { listOf(it[1], it[0], it[2]) } else q.options.indices.shuffled()
                val opts = order.map { q.options[it] }
                val answerIdx = order.indexOf(q.answer)
                set { it.copy(current = players.indexOf(p), board = "", boardLabel = "${regionName ?: "전국"} · 객관식", rule = head, options = opts) }
                say("${p.name} 님, 객관식! ${q.q} 1번 ${opts[0]}, 2번 ${opts[1]}, 3번 ${opts[2]}!")
                val a = hear(p, turnSeconds(p, 7))
                val ok = a?.let { Hangul.parseChoice(it, opts) } == answerIdx
                if (ok) p.score += 2
                set { it.copy(players = ui.players.toList(), board = "${answerIdx + 1}번 ${opts[answerIdx]}", options = emptyList()) }
                say((if (ok) "딩동댕! " else "땡! ") + q.explain)
                return ok
            }
        }
    }

    private fun correct(a: String?, answers: List<String>): Boolean {
        val n = a?.let { Hangul.normalize(it) } ?: return false
        return answers.any { n.contains(Hangul.normalize(it)) }
    }

    /* ---------------- 시상식 ---------------- */

    private suspend fun awards(players: List<Player>) {
        val sorted = players.sortedByDescending { it.score }
        set { it.copy(screen = UiState.Screen.RESULT, players = sorted, current = -1, board = sorted.first().name, boardLabel = "오늘의 우승", rule = "", round = "") }
        val top = sorted.first()
        val ties = sorted.filter { it.score == top.score }
        if (ties.size > 1) say("시상식! 공동 우승은 ${ties.joinToString(", ") { it.name }}, ${top.score}점!")
        else say("시상식! 오늘의 우승은 ${top.name} 님, ${top.score}점! 축하합니다!")
        say("다른 게임을 하려면 화면이나 음성으로 불러 주세요. 안전 운전하세요!")
    }
}
