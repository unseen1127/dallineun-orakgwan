package kr.unseen.dallineun

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.TextUtils
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kr.unseen.dallineun.car.CarContext
import kr.unseen.dallineun.game.GameType
import kr.unseen.dallineun.game.MC
import kr.unseen.dallineun.game.UiState
import kr.unseen.dallineun.voice.AndroidVoice
import kr.unseen.dallineun.voice.DemoVoice
import kr.unseen.dallineun.voice.PleosVoice
import kr.unseen.dallineun.voice.VoiceIO

/**
 * 달리는 오락관 - 화면을 보지 않고 목소리로 함께 노는 차량용 게임 사회자.
 * Pleos Connect UI 가이드 기준: 다크 배경, 대비 4.5:1 이상, 큰 터치 영역, 주행 중 정보 최소화, 애니메이션 없음.
 */
class MainActivity : Activity() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var car: CarContext
    private lateinit var mc: MC
    private var realVoice: VoiceIO? = null
    private var demoMode = false
    private var last = UiState()
    private lateinit var root: FrameLayout
    private lateinit var font: Typeface

    // Palette (Pleos dark 계열 + 오락관 포인트 컬러)
    private val bg = Color.parseColor("#0E0F13")
    private val surface = Color.parseColor("#1A1C22")
    private val surface2 = Color.parseColor("#252832")
    private val textHi = Color.parseColor("#F5F6F8")
    private val textMid = Color.parseColor("#AEB3BE")
    private val accent = Color.parseColor("#FF6A3D")
    private val yellow = Color.parseColor("#FFD447")
    private val green = Color.parseColor("#3DDC97")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        font = resources.getFont(R.font.asta_sans)
        root = FrameLayout(this).apply { setBackgroundColor(bg) }
        setContentView(root)

        car = CarContext(this)
        car.onChange = { runOnUiThread { render(last) } }
        car.start()
        mc = MC(scope, car) { s -> runOnUiThread { last = s; render(s) } }

        demoMode = intent?.getBooleanExtra("demo", false) == true
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.ACCESS_FINE_LOCATION), 1)
        }
        scope.launch {
            realVoice = PleosVoice.create(this@MainActivity) ?: AndroidVoice.create(this@MainActivity)
            mc.voice = realVoice!!
            render(last)
        }
        // 데모 시뮬레이션 중 남은 시간 흐르게
        scope.launch { while (isActive) { delay(1000); car.simulateTick(1) } }
        render(last)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig); render(last)
    }

    override fun onDestroy() {
        scope.cancel(); realVoice?.release(); car.release(); super.onDestroy()
    }

    /* ---------------- 게임 시작 ---------------- */

    private fun startGame(type: GameType) {
        val v = realVoice ?: return
        if (demoMode) {
            val demo = DemoVoice(v) { }
            demo.load(DemoScripts.forGame(type))
            mc.voice = demo; mc.demo = true
            car.simulate(driving = true, remainSec = 80 * 60, destination = "광주 무등산",
                address = "충청남도 천안시 동남구", lat = 36.80, lng = 127.15)
            mc.start(type, DemoScripts.players(type))
        } else {
            mc.voice = v; mc.demo = false
            mc.start(type)
        }
    }

    /* ---------------- 렌더링 ---------------- */

    private fun dp(v: Float) = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, resources.displayMetrics).toInt()
    private fun round(color: Int, r: Float, stroke: Int? = null) = GradientDrawable().apply {
        setColor(color); cornerRadius = dp(r).toFloat(); stroke?.let { setStroke(dp(3f), it) }
    }

    private fun text(s: String, size: Float, color: Int = textHi, bold: Boolean = false, lines: Int = 2) =
        TextView(this).apply {
            text = s; setTextColor(color); setTextSize(TypedValue.COMPLEX_UNIT_SP, size)
            typeface = Typeface.create(font, if (bold) 700 else 400, false)
            maxLines = lines; ellipsize = TextUtils.TruncateAt.END
            includeFontPadding = false
        }

    private fun chip(s: String, fg: Int, bgc: Int) = text(s, 18f, fg, true, 1).apply {
        background = round(bgc, 22f); setPadding(dp(18f), dp(10f), dp(18f), dp(10f))
    }

    private fun lp(w: Int = ViewGroup.LayoutParams.MATCH_PARENT, h: Int = ViewGroup.LayoutParams.WRAP_CONTENT, weight: Float = 0f, top: Int = 0, end: Int = 0) =
        LinearLayout.LayoutParams(w, h, weight).apply { topMargin = dp(top.toFloat()); marginEnd = dp(end.toFloat()) }

    private fun col() = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
    private fun row() = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }

    private val landscape get() = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    private fun render(s: UiState) {
        root.removeAllViews()
        val page = col().apply { setPadding(dp(32f), dp(24f), dp(32f), dp(24f)) }
        root.addView(page, FrameLayout.LayoutParams(-1, -1))
        page.addView(topBar())
        when (s.screen) {
            UiState.Screen.HOME -> page.addView(home(), lp(h = 0, weight = 1f, top = 20))
            else -> page.addView(game(s), lp(h = 0, weight = 1f, top = 20))
        }
    }

    private fun topBar(): View {
        val st = car.state
        val bar = row()
        bar.addView(text("달리는 오락관", 30f, textHi, true, 1), lp(w = 0, weight = 1f))
        if (st.audioPaused || mc.paused) bar.addView(chip("일시정지", bg, yellow), lp(w = -2, end = 10))
        bar.addView(
            if (st.driving) chip("주행 중 · 음성 전용", bg, green) else chip("정차 중", textHi, surface2),
            lp(w = -2, end = 10)
        )
        st.remainSec?.let { sec ->
            val m = sec / 60
            bar.addView(chip("도착까지 " + (if (m >= 60) "${m / 60}시간 ${m % 60}분" else "${m}분"), textHi, surface2), lp(w = -2))
        }
        return bar
    }

    private fun home(): View {
        val box = col()
        box.addView(text("어떤 게임을 할까요?", 26f, textMid, false, 1))
        val cards = if (landscape) row() else col()
        GameType.values().forEachIndexed { i, g ->
            val card = col().apply {
                background = round(surface, 24f)
                setPadding(dp(28f), dp(28f), dp(28f), dp(28f))
                isClickable = true
                setOnClickListener { startGame(g) }
                contentDescription = g.title
            }
            card.addView(text("0${i + 1}", 20f, accent, true, 1))
            card.addView(text(g.title, 30f, textHi, true, 2), lp(top = 14))
            card.addView(text(g.desc, 20f, textMid, false, 2), lp(top = 10))
            val last = GameType.values().size - 1
            val l = if (landscape) lp(w = 0, h = -1, weight = 1f, end = if (i < last) 16 else 0) else lp(h = 0, weight = 1f, top = if (i > 0) 16 else 0)
            cards.addView(card, l)
        }
        box.addView(cards, lp(h = 0, weight = 1f, top = 16))
        val foot = row()
        val engine = (if (demoMode) "시연 모드 · " else "") + (realVoice?.engineName ?: "음성 엔진 준비 중")
        foot.addView(text("음성 엔진: $engine", 16f, textMid, false, 1), lp(w = 0, weight = 1f))
        val demoBtn = chip(if (demoMode) "시연 모드 켜짐" else "시연 모드", if (demoMode) bg else textHi, if (demoMode) yellow else surface2).apply {
            isClickable = true; minHeight = dp(64f); gravity = Gravity.CENTER
            setOnClickListener { demoMode = !demoMode; render(last) }
        }
        foot.addView(demoBtn, lp(w = -2))
        box.addView(foot, lp(top = 16))
        return box
    }

    private fun game(s: UiState): View {
        val driving = car.state.driving
        val main = col().apply { background = round(surface, 28f); setPadding(dp(32f), dp(28f), dp(32f), dp(28f)) }

        // 게임명 + 규칙 + 라운드
        val head = row()
        head.addView(text(s.game?.title ?: "", 24f, accent, true, 1), lp(w = -2, end = 14))
        if (s.rule.isNotBlank()) head.addView(chip(s.rule, bg, yellow), lp(w = -2, end = 10))
        if (s.round.isNotBlank()) head.addView(chip(s.round, textHi, surface2), lp(w = -2))
        main.addView(head)

        // 핵심 정보: 지금 단어 / 초성 / 우승자
        main.addView(text(s.boardLabel, 20f, textMid, false, 1), lp(top = 22))
        if (s.board.isNotBlank()) main.addView(text(s.board, if (s.board.length > 8) 56f else 80f, textHi, true, 1), lp(top = 6))
        // 객관식 보기: 큰 글씨 3줄
        s.options.forEachIndexed { i, o ->
            val row = text("${i + 1}   $o", 36f, textHi, true, 1).apply {
                background = round(surface2, 18f); setPadding(dp(24f), dp(14f), dp(24f), dp(14f))
            }
            main.addView(row, lp(top = if (i == 0) 12 else 10))
        }

        // 차례 + 타이머
        val turn = row()
        val cur = s.players.getOrNull(s.current)
        val who = when {
            s.screen == UiState.Screen.RESULT -> "시상식"
            cur != null -> "${cur.name} 차례"
            s.current >= 0 -> "이름 등록 중"
            else -> "MC 진행 중"
        }
        turn.addView(text(who, 34f, textHi, true, 1), lp(w = 0, weight = 1f))
        if (s.listening) {
            val t = text("${s.timerLeft}", 44f, if (s.timerLeft <= 3) accent else textHi, true, 1).apply {
                gravity = Gravity.CENTER
                background = round(surface2, 50f, if (s.timerLeft <= 3) accent else green)
            }
            turn.addView(t, LinearLayout.LayoutParams(dp(100f), dp(100f)))
        }
        main.addView(turn, lp(top = 18))

        // MC 자막 (주행 중에는 1줄로 줄임)
        if (s.mcLine.isNotBlank()) {
            main.addView(text("MC  ${s.mcLine}", 22f, textMid, false, if (driving) 1 else 2), lp(top = 18))
        }
        if (!driving && s.heard.isNotBlank()) main.addView(text(s.heard, 20f, green, false, 1), lp(top = 8))

        // 점수판
        val board = if (landscape) col() else row()
        s.players.forEachIndexed { i, p ->
            val active = i == s.current
            val pc = row().apply {
                background = round(if (active) surface2 else surface, 20f, if (active) accent else null)
                setPadding(dp(20f), dp(16f), dp(20f), dp(16f))
            }
            pc.addView(text(p.name + if (p.isDriver) "  · 운전" else "", 22f, textHi, active, 1), lp(w = 0, weight = 1f))
            pc.addView(text("${p.score}", 28f, if (active) accent else textHi, true, 1), lp(w = -2))
            board.addView(pc, if (landscape) lp(top = if (i > 0) 12 else 0) else lp(w = 0, weight = 1f, end = 10))
        }

        // 하단 조작: 큰 버튼 2개
        val ctl = row()
        val pause = chip(if (mc.paused) "계속" else "일시정지", textHi, surface2).apply {
            gravity = Gravity.CENTER; minHeight = dp(76f); isClickable = true
            setOnClickListener { mc.paused = !mc.paused; render(last) }
        }
        val stop = chip(if (s.screen == UiState.Screen.RESULT) "처음으로" else "그만하기", bg, accent).apply {
            gravity = Gravity.CENTER; minHeight = dp(76f); isClickable = true
            setOnClickListener { mc.paused = false; mc.stop() }
        }
        ctl.addView(pause, lp(w = 0, weight = 1f, end = 12))
        ctl.addView(stop, lp(w = 0, weight = 1f))

        return if (landscape) {
            row().apply {
                gravity = Gravity.TOP
                addView(main, lp(w = 0, h = -1, weight = 1.6f, end = 20))
                val side = col()
                side.addView(text("점수판", 20f, textMid, false, 1))
                side.addView(board, lp(h = 0, weight = 1f, top = 12))
                side.addView(ctl, lp(top = 12))
                addView(side, lp(w = 0, h = -1, weight = 1f))
            }
        } else {
            col().apply {
                addView(main, lp(h = 0, weight = 1f))
                addView(board, lp(top = 16))
                addView(ctl, lp(top = 16))
            }
        }
    }
}

/** 시연 모드 대본 (마이크 없는 환경에서 화면 녹화용) */
object DemoScripts {
    fun players(t: GameType) = when (t) {
        GameType.WORD_CHAIN -> listOf("아빠", "엄마", "지우", "민준")
        GameType.CHOSUNG -> listOf("아빠", "지우")
        GameType.GAME_369 -> listOf("아빠", "엄마", "지우", "민준")
        GameType.REGION -> listOf("아빠", "엄마", "지우")
    }

    fun forGame(t: GameType): List<String?> = when (t) {
        GameType.WORD_CHAIN -> listOf(
            "네 명이요", "아빠", "엄마", "지우", "민준", "그대로 할게요",
            "차표", "표범", "범인", "인형",          // 4연속 성공 -> 새 규칙(3글자)
            "형광등", "등산로", "노란색", "색깔",     // 두음법칙(로->노), 3글자 규칙 위반
            "다람쥐", "쥐포", "포도", "도토리",       // 새 단어 '바다'에서 재시작 -> 새 규칙('가'로 시작)
            "가방", "그만하자",
        )
        GameType.CHOSUNG -> listOf(
            "두 명", "아빠", "지우", "그대로",
            "김치찌개", "떡국", "떡볶이", "붕어빵", null, "호두과자",
        )
        GameType.GAME_369 -> listOf(
            "네 명이요", "아빠", "엄마", "지우", "민준", "그대로 할게요",
            "이", "(박수)", "사", "오", "(박수)", "칠", "팔", "구",   // 아빠: 9에서 박수를 못 쳐서 아웃
            "이", "삼",                                      // 민준: 3에서 숫자를 말해 아웃
            "이", "(박수)", "사", "오", "육",                // 지우: 6에서 아웃 -> 엄마 생존
        )
        GameType.REGION -> listOf(
            "세 명", "아빠", "엄마", "지우", "섞어 줘",
            "오!", "2번!", "엑스",                            // OX 정답, 객관식 정답, OX 오답
        )
    }
}
