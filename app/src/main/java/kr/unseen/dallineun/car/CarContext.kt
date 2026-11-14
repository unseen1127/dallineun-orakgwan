package kr.unseen.dallineun.car

import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.util.Log
import ai.pleos.playground.location.fused.FusedLocation
import ai.pleos.playground.location.fused.listener.FusedLocationListener
import ai.pleos.playground.location.fused.model.LocationData
import ai.pleos.playground.navi.data.CurrentLocationInfo
import ai.pleos.playground.navi.data.DrivingInfo
import ai.pleos.playground.navi.helper.NaviHelper
import ai.pleos.playground.navi.helper.listener.NaviHelperEventListener

private const val TAG = "DallineunCar"

/**
 * 차량 맥락을 한곳에 모은다.
 * - 주행 규제(CarUxRestrictions): 주행 중이면 화면을 최소화하고 음성 전용으로 진행
 * - NaviHelper: 목적지까지 남은 시간, 현재 주소(지역 퀴즈)
 * - FusedLocation: 차량 위치(주소가 없을 때 좌표로 지역 판정)
 * - 오디오 포커스: 길 안내 음성이 나오면 게임 일시정지
 */
/** MC가 쓰는 차량 맥락. 테스트에서는 가짜 구현으로 대체한다 */
interface CarEnv {
    val state: CarContext.State
    fun requestAudioFocus(): Boolean
    fun abandonAudioFocus()
    fun refreshLocation()
}

class CarContext(private val context: Context) : CarEnv {

    data class State(
        val driving: Boolean = false,
        val remainSec: Int? = null,
        val destination: String? = null,
        val address: String? = null,
        val lat: Double? = null,
        val lng: Double? = null,
        val audioPaused: Boolean = false,
        val sources: Set<String> = emptySet(),
    )

    @Volatile override var state = State(); private set
    var onChange: ((State) -> Unit)? = null

    private var car: Any? = null
    private var navi: NaviHelper? = null
    private var fused: FusedLocation? = null
    private var focusRequest: AudioFocusRequest? = null
    private val audio = context.getSystemService(AudioManager::class.java)

    /** 데모·시연용 강제 값 (실차에서는 사용하지 않음) */
    var simulated = false

    private fun update(f: (State) -> State) {
        state = f(state); onChange?.invoke(state)
    }

    fun start() {
        startUxRestrictions()
        startNavi()
        startFusedLocation()
    }

    private fun startUxRestrictions() {
        if (!context.packageManager.hasSystemFeature(PackageManager.FEATURE_AUTOMOTIVE)) return
        try {
            val c = android.car.Car.createCar(context)
            car = c
            val mgr = c.getCarManager(android.car.Car.CAR_UX_RESTRICTION_SERVICE)
                as android.car.drivingstate.CarUxRestrictionsManager
            val apply: (android.car.drivingstate.CarUxRestrictions) -> Unit = { r ->
                if (!simulated) update { it.copy(driving = r.isRequiresDistractionOptimization, sources = it.sources + "CarUxRestrictions") }
            }
            apply(mgr.currentCarUxRestrictions)
            mgr.registerListener { r -> apply(r) }
        } catch (t: Throwable) {
            Log.w(TAG, "CarUxRestrictions unavailable", t)
        }
    }

    private val naviListener = object : NaviHelperEventListener {
        override fun onDrivingInfo(drivingInfo: DrivingInfo) {
            if (simulated) return
            val d = drivingInfo.destination ?: return
            update { it.copy(remainSec = d.duration, destination = d.name, sources = it.sources + "NaviHelper") }
        }
        override fun onCurrentLocationInfo(currentLocationInfo: CurrentLocationInfo) {
            if (simulated) return
            update {
                it.copy(address = currentLocationInfo.address, lat = currentLocationInfo.latitude,
                    lng = currentLocationInfo.longitude, sources = it.sources + "NaviHelper")
            }
        }
    }

    private fun startNavi() {
        try {
            val n = NaviHelper(context.applicationContext)
            navi = n
            n.initialize({
                n.addListener(naviListener)
                n.getCurrentLocationInfo()
            }, { e -> Log.w(TAG, "NaviHelper init failed", e) })
        } catch (t: Throwable) {
            Log.w(TAG, "NaviHelper unavailable", t)
        }
    }

    /** 지역 퀴즈 직전에 최신 주소를 다시 요청 */
    override fun refreshLocation() {
        runCatching { navi?.getCurrentLocationInfo() }
        runCatching { fused?.getLastKnownLocation(fusedListener) }
    }

    private val fusedListener = object : FusedLocationListener {
        override fun onChanged(location: LocationData?) {
            val data = location ?: return
            if (simulated) return
            update { it.copy(lat = data.latitude, lng = data.longitude, sources = it.sources + "FusedLocation") }
        }
        override fun onError(e: Exception) { Log.w(TAG, "FusedLocation error", e) }
    }

    private fun startFusedLocation() {
        try {
            val f = FusedLocation(context.applicationContext)
            f.initialize()
            if (f.isSupported()) {
                fused = f
                f.registerFusedLocationCallback(fusedListener)
            }
        } catch (t: Throwable) {
            Log.w(TAG, "FusedLocation unavailable", t)
        }
    }

    /* ---------- 오디오 포커스: 길 안내 등 중요한 음성이 오면 잠시 멈춤 ---------- */
    override fun requestAudioFocus(): Boolean {
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
            .build()
        val req = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(attrs)
            .setWillPauseWhenDucked(true)
            .setOnAudioFocusChangeListener { change ->
                when (change) {
                    AudioManager.AUDIOFOCUS_LOSS_TRANSIENT,
                    AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK,
                    AudioManager.AUDIOFOCUS_LOSS -> update { it.copy(audioPaused = true) }
                    AudioManager.AUDIOFOCUS_GAIN -> update { it.copy(audioPaused = false) }
                }
            }
            .build()
        focusRequest = req
        return audio.requestAudioFocus(req) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    }

    override fun abandonAudioFocus() { focusRequest?.let { audio.abandonAudioFocusRequest(it) } }

    /* ---------- 데모 시뮬레이션 ---------- */
    fun simulate(driving: Boolean, remainSec: Int?, destination: String?, address: String?, lat: Double?, lng: Double?) {
        simulated = true
        update { State(driving, remainSec, destination, address, lat, lng, it.audioPaused, setOf("데모 시뮬레이션")) }
    }

    fun simulateTick(seconds: Int) {
        if (!simulated) return
        update { s -> s.copy(remainSec = s.remainSec?.let { (it - seconds).coerceAtLeast(0) }) }
    }

    fun release() {
        abandonAudioFocus()
        runCatching { navi?.removeListener(naviListener); navi?.release() }
        runCatching { fused?.unregisterFusedLocationCallback(fusedListener); fused?.release() }
        runCatching { (car as? android.car.Car)?.disconnect() }
    }
}
