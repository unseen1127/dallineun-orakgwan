package kr.unseen.dallineun.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import java.util.UUID
import android.speech.tts.TextToSpeech as AndroidTts
import android.speech.tts.UtteranceProgressListener

private const val TAG = "DallineunVoice"

/** MC가 말하고(TTS) 듣는(STT) 채널. 게임 엔진은 이 인터페이스만 사용한다. */
interface VoiceIO {
    val engineName: String
    suspend fun speak(text: String)
    /** 최대 timeoutMs 동안 한 번의 발화를 듣고 최종 텍스트를 돌려준다. 못 들으면 null. */
    suspend fun listen(timeoutMs: Long): String?
    /** 직전 listen()에서 말소리 없이 순간 음량 급상승(박수)이 있었는지. 실차 마이크 기준 임계값 튜닝 필요 */
    val clapDetected: Boolean get() = false
    fun stopAll()
    fun release()
}

/* ------------------------------------------------------------------ */
/* 1) Pleos Connect SDK (TextToSpeech / SpeechToText, ON_DEVICE 모드)      */
/* ------------------------------------------------------------------ */
class PleosVoice private constructor(
    private val tts: ai.pleos.playground.tts.TextToSpeech,
    private val stt: ai.pleos.playground.stt.SpeechToText,
) : VoiceIO {
    override val engineName = "Pleos Connect TTS/STT"

    private var speakDone: CompletableDeferred<Unit>? = null
    private var listenDone: CompletableDeferred<String?>? = null
    private var lastPartial: String? = null
    private val rmsLog = ArrayList<Double>()
    override val clapDetected: Boolean get() = ClapDetector.isClap(rmsLog)

    private val ttsListener = object : ai.pleos.playground.tts.listener.EventListener {
        override fun onReady() = Unit
        override fun onStart() = Unit
        override fun onUpdatedRms(rms: Double) = Unit
        override fun onDone() { speakDone?.complete(Unit) }
        override fun onStop() { speakDone?.complete(Unit) }
        override fun onError(errorType: ai.pleos.playground.tts.constant.ErrorType) {
            Log.w(TAG, "TTS error $errorType"); speakDone?.complete(Unit)
        }
    }

    private val sttListener = object : ai.pleos.playground.stt.listener.ResultListener {
        override fun onUpdated(text: String, isFinal: Boolean) {
            lastPartial = text
            if (isFinal) listenDone?.complete(text)
        }
        override fun onUpdatedEpdData(start: Long, end: Long) = Unit
        override fun onStartedRecognition() = Unit
        override fun onEndedRecognition() { listenDone?.complete(lastPartial) }
        override fun onError(errorType: ai.pleos.playground.stt.constant.ErrorType) {
            Log.w(TAG, "STT error $errorType"); listenDone?.complete(null)
        }
        override fun onReady() = Unit
        override fun onUpdatedRms(rms: Double) { rmsLog += rms }
    }

    init {
        tts.addEventListener(ttsListener)
        stt.addListener(sttListener)
    }

    override suspend fun speak(text: String) {
        val d = CompletableDeferred<Unit>(); speakDone = d
        tts.speak(text, true)
        // 안전장치: 콜백이 오지 않아도 진행이 멈추지 않게 글자 수 기반 상한 시간
        withTimeoutOrNull(4000L + text.length * 220L) { d.await() }
    }

    override suspend fun listen(timeoutMs: Long): String? {
        val deadline = System.currentTimeMillis() + timeoutMs
        rmsLog.clear()
        while (true) {
            val remain = deadline - System.currentTimeMillis()
            if (remain < 400) return lastPartial?.takeIf { it.isNotBlank() }
            val d = CompletableDeferred<String?>(); listenDone = d; lastPartial = null
            stt.request()
            val r = withTimeoutOrNull(remain) { d.await() }
            if (r == null && System.currentTimeMillis() >= deadline - 50) { stt.stop(); return lastPartial?.takeIf { it.isNotBlank() } }
            if (!r.isNullOrBlank()) return r
            // 말이 없어 인식이 일찍 끝나면 남은 시간 동안 다시 듣는다
            delay(250)
        }
    }

    override fun stopAll() { runCatching { tts.stop() }; runCatching { stt.stop() } }

    override fun release() {
        runCatching { tts.removeEventListener(ttsListener); tts.release() }
        runCatching { stt.removeListener(sttListener); stt.release() }
    }

    companion object {
        /** Pleos 음성 서비스가 있는 차량/에뮬레이터에서만 성공한다. 실패하면 null. */
        suspend fun create(context: Context): PleosVoice? = runCatching {
            val app = context.applicationContext
            val tts = ai.pleos.playground.tts.TextToSpeech(app, ai.pleos.playground.tts.constant.Mode.ON_DEVICE)
            val stt = ai.pleos.playground.stt.SpeechToText(app, ai.pleos.playground.stt.constant.Mode.ON_DEVICE)
            val ttsOk = CompletableDeferred<Boolean>()
            val sttOk = CompletableDeferred<Boolean>()
            tts.initialize(object : ai.pleos.playground.tts.listener.OnInitializeListener {
                override fun onSuccess() { ttsOk.complete(true) }
                override fun onFailure(errorType: ai.pleos.playground.tts.constant.ErrorType) { ttsOk.complete(false) }
            })
            stt.initialize(object : ai.pleos.playground.stt.listener.OnInitializeListener {
                override fun onSuccess() { sttOk.complete(true) }
                override fun onFailure(errorType: ai.pleos.playground.stt.constant.ErrorType) { sttOk.complete(false) }
            })
            val ok = withTimeoutOrNull(3000) { ttsOk.await() && sttOk.await() } ?: false
            if (!ok) { runCatching { tts.release() }; runCatching { stt.release() }; return@runCatching null }
            tts.setLocale(Locale.KOREA); stt.setLocale(Locale.KOREA)
            PleosVoice(tts, stt)
        }.onFailure { Log.w(TAG, "Pleos voice unavailable", it) }.getOrNull()
    }
}

/* ------------------------------------------------------------------ */
/* 2) 표준 Android TTS / SpeechRecognizer (Pleos 서비스가 없는 기기용 대체)   */
/* ------------------------------------------------------------------ */
class AndroidVoice private constructor(
    private val context: Context,
    private val tts: AndroidTts,
    /** 차량(Pleos)에서는 외부 음성 인식 서비스를 쓰지 않는다. 음성 인식은 SpeechToText SDK만 사용 */
    private val allowStt: Boolean,
) : VoiceIO {
    override val engineName = if (allowStt) "Android TTS/STT (fallback)" else "Android TTS (STT는 Pleos SDK 전용)"
    private var recognizer: SpeechRecognizer? = null
    private val rmsLog = ArrayList<Double>()
    override val clapDetected: Boolean get() = ClapDetector.isClap(rmsLog)

    override suspend fun speak(text: String) {
        val id = UUID.randomUUID().toString()
        val d = CompletableDeferred<Unit>()
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) = Unit
            override fun onDone(utteranceId: String?) { if (utteranceId == id) d.complete(Unit) }
            @Deprecated("deprecated") override fun onError(utteranceId: String?) { d.complete(Unit) }
        })
        tts.speak(text, AndroidTts.QUEUE_FLUSH, null, id)
        withTimeoutOrNull(4000L + text.length * 220L) { d.await() }
    }

    override suspend fun listen(timeoutMs: Long): String? {
        if (!allowStt || !SpeechRecognizer.isRecognitionAvailable(context)) { delay(timeoutMs); return null }
        val deadline = System.currentTimeMillis() + timeoutMs
        rmsLog.clear()
        while (true) {
            val remain = deadline - System.currentTimeMillis()
            if (remain < 600) { delay(remain.coerceAtLeast(0)); return null }
            val r = listenOnce(remain)
            if (!r.isNullOrBlank()) return r
            if (System.currentTimeMillis() >= deadline - 50) return null
            // 휴대폰 음성 인식은 몇 초 침묵이면 스스로 끝나므로, 남은 시간 동안 다시 듣는다
            delay(250)
        }
    }

    private suspend fun listenOnce(remainMs: Long): String? {
        val d = CompletableDeferred<String?>()
        val rec = recognizer ?: SpeechRecognizer.createSpeechRecognizer(context).also { recognizer = it }
        rec.setRecognitionListener(object : RecognitionListener {
            override fun onResults(results: Bundle?) {
                d.complete(results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull())
            }
            override fun onError(error: Int) { d.complete(null) }
            override fun onReadyForSpeech(params: Bundle?) = Unit
            override fun onBeginningOfSpeech() = Unit
            override fun onRmsChanged(rmsdB: Float) { rmsLog += rmsdB.toDouble() }
            override fun onBufferReceived(buffer: ByteArray?) = Unit
            override fun onEndOfSpeech() = Unit
            override fun onPartialResults(partialResults: Bundle?) = Unit
            override fun onEvent(eventType: Int, params: Bundle?) = Unit
        })
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ko-KR")
        }
        rec.startListening(intent)
        val r = withTimeoutOrNull(remainMs) { d.await() }
        if (r == null) rec.cancel()
        return r
    }

    override fun stopAll() { tts.stop(); recognizer?.cancel() }
    override fun release() { tts.shutdown(); recognizer?.destroy() }

    companion object {
        suspend fun create(context: Context, allowStt: Boolean = true): AndroidVoice {
            val d = CompletableDeferred<Int>()
            val tts = AndroidTts(context.applicationContext) { d.complete(it) }
            withTimeoutOrNull(3000) { d.await() }
            tts.language = Locale.KOREA
            tts.setSpeechRate(1.05f)
            return AndroidVoice(context.applicationContext, tts, allowStt)
        }
    }
}

/* ------------------------------------------------------------------ */
/* 3) 데모 모드: 실제 TTS로 진행하고, 탑승자 답변은 대본으로 시뮬레이션         */
/*    (마이크 없는 에뮬레이터 시연·심사용 화면 녹화)                        */
/* ------------------------------------------------------------------ */
class DemoVoice(
    private val base: VoiceIO,
    private val onSimulatedAnswer: (String) -> Unit,
) : VoiceIO {
    override val engineName = base.engineName + " · 데모 대본"
    private val script = ArrayDeque<String?>()
    private var lastWasClap = false
    override val clapDetected: Boolean get() = lastWasClap

    fun load(answers: List<String?>) { script.clear(); script.addAll(answers) }

    override suspend fun speak(text: String) = base.speak(text)

    override suspend fun listen(timeoutMs: Long): String? {
        lastWasClap = false
        if (script.isEmpty()) { delay(1200); return null }
        val a = script.removeFirst()
        if (a == "(박수)") { delay(1200); onSimulatedAnswer("(박수)"); lastWasClap = true; delay(300); return null }
        if (a == null) { delay(timeoutMs); return null } // 시간 초과 연출
        delay(1500)
        onSimulatedAnswer(a)
        delay(500)
        return a
    }

    override fun stopAll() = base.stopAll()
    override fun release() = base.release()
}

/** 음량 기록에서 박수 판정: 기준 음량 대비 짧고 큰 급상승이 있으면 박수로 본다 */
object ClapDetector {
    /** 기준 대비 상승 폭 임계값 (dB 계열 값 기준). 실차 마이크 환경에서 튜닝 필요 */
    var jumpThreshold = 6.0

    fun isClap(rms: List<Double>): Boolean {
        if (rms.size < 4) return false
        val sorted = rms.sorted()
        val baseline = sorted[sorted.size / 2]
        val peak = sorted.last()
        val loudCount = rms.count { it > baseline + jumpThreshold }
        return peak - baseline >= jumpThreshold && loudCount in 1..4
    }
}
