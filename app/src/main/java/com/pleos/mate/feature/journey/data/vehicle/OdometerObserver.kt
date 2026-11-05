package com.pleos.mate.feature.journey.data.vehicle

import ai.pleos.playground.vehicle.Vehicle
import ai.pleos.playground.vehicle.api.model.capability.AccessType
import ai.pleos.playground.vehicle.listener.DistanceDrivenListener
import ai.pleos.playground.vehicle.listener.DrivingTimeListener
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import com.pleos.mate.feature.journey.domain.JourneyObservation
import com.pleos.mate.feature.journey.domain.OdometerSample
import com.pleos.mate.feature.journey.domain.OdometerSource
import kotlin.math.abs

/** Main-thread owner. Listener notifications trigger paired reads; a heartbeat detects silent stalls. */
class OdometerObserver(
    context: Context,
    private val onSample: (OdometerSample) -> Unit,
    private val onProblem: (String) -> Unit,
    private val onDiagnostic: (String) -> Unit = {}
) : JourneyObservation {
    private val context = context.applicationContext
    private val main = Handler(Looper.getMainLooper())
    private var vehicle: Vehicle? = null
    private var distanceListener: DistanceDrivenListener? = null
    private var timeListener: DrivingTimeListener? = null
    private var generation = 0
    private var totalDistance = false
    private var enabled = false
    private var reading = false
    private var readVersion = 0
    private var lastRead = -10_000L
    private var deadline: Runnable? = null
    private var heartbeat: Runnable? = null

    override fun start() {
        stop()
        val token = generation
        totalDistance = false
        try {
            val sdk = Vehicle(context)
            vehicle = sdk
            sdk.initialize()
            deadline = Runnable { fail(token, "이동 정보 연결을 확인하지 못했어요.") }.also { main.postDelayed(it, 15_000) }
            sdk.getOdometer().checkTripCurrentDrivingDistanceCapabilities(
                onComplete = { distance -> main.post {
                    if (valid(token)) {
                        onDiagnostic("distance access=${distance.accessType}")
                        if (!readable(distance.accessType)) checkTotalDistance(token, sdk)
                        else try { sdk.getOdometer().checkTripCurrentDrivingTimeCapabilities(
                            onComplete = { time -> main.post {
                                if (valid(token)) {
                                    onDiagnostic("time access=${time.accessType}")
                                    if (!readable(time.accessType)) checkTotalDistance(token, sdk)
                                    else subscribe(token, sdk)
                                }
                            } },
                            onFailed = { error -> main.post { if (valid(token)) {
                                if (unsupported(error)) checkTotalDistance(token, sdk) else fail(token, "주행 시간 연결을 확인하지 못했어요.")
                            } } }
                        ) } catch (_: Exception) { fail(token, "주행 시간 지원을 확인하지 못했어요.") }
                    }
                } },
                onFailed = { error -> main.post { if (valid(token)) { onDiagnostic("distance capability error=${error.javaClass.simpleName}: ${error.message}"); if (unsupported(error)) checkTotalDistance(token, sdk) else fail(token, "이동 거리 연결을 확인하지 못했어요.") } } }
            )
        } catch (_: Exception) { fail(token, "이동 정보를 읽을 수 없어요. 차량 연결과 권한을 확인해 주세요.") }
    }

    private fun unsupported(error: Exception) = error.message?.contains("ERROR_NOT_SUPPORTED_PROPERTY") == true

    private fun checkTotalDistance(token: Int, sdk: Vehicle) {
        if (!valid(token)) return
        onDiagnostic("현재 트립 미지원 → 누적거리 확인 (km → m), 주행 시간은 확인 불가")
        try {
            sdk.getOdometer().checkCurrentDistanceDrivenCapabilities(
                onComplete = { config -> main.post {
                    if (valid(token)) {
                        if (!readable(config.accessType)) fail(token, "현재 트립과 누적거리 조회가 지원되지 않아요.")
                        else {
                            totalDistance = true
                            enabled = true
                            requestSample()
                        }
                    }
                } },
                onFailed = { main.post { fail(token, "누적거리 연결도 확인하지 못했어요. 지원 여부와 권한을 확인해 주세요.") } }
            )
        } catch (_: Exception) { fail(token, "누적거리 지원을 확인하지 못했어요.") }
    }

    private fun readable(access: AccessType) = access == AccessType.READ || access == AccessType.READ_WRITE
    private fun valid(token: Int) = token == generation && vehicle != null

    private fun subscribe(token: Int, sdk: Vehicle) {
        try {
            enabled = true
            distanceListener = object : DistanceDrivenListener {
                override fun onDistanceDrivenUpdated(value: Float?) { main.post { signal(token, "distance update=$value m") } }
                override fun onFailed(e: Exception) { main.post { fail(token, "이동 거리 수신이 끊겼어요.") } }
            }.also { sdk.getOdometer().registerTripCurrentDrivingDistance(it) }
            timeListener = object : DrivingTimeListener {
                override fun onDrivingTimeUpdated(value: Int?) { main.post { signal(token, "time update=$value min") } }
                override fun onFailed(e: Exception) { main.post { fail(token, "주행 시간 수신이 끊겼어요.") } }
            }.also { sdk.getOdometer().registerTripCurrentDrivingTime(it) }
            requestSample()
        } catch (_: Exception) { fail(token, "이동 정보 수신을 시작하지 못했어요.") }
    }

    private fun signal(token: Int, text: String) {
        if (!valid(token)) return
        onDiagnostic(text)
        if (!reading && SystemClock.elapsedRealtime() - lastRead >= 1_000) requestSample()
    }

    override fun requestSample() {
        val sdk = vehicle ?: return
        if (!enabled || reading) return
        val token = generation
        reading = true
        val request = ++readVersion
        fun currentRead() = valid(token) && reading && request == readVersion
        lastRead = SystemClock.elapsedRealtime()
        deadline?.let(main::removeCallbacks)
        heartbeat?.let(main::removeCallbacks)
        deadline = Runnable { if (currentRead()) fail(token, "이동 정보 확인에 공백이 생겼어요. 마지막 확인 구간만 남길게요.") }.also { main.postDelayed(it, 5_000) }
        val readStarted = lastRead
        var meters: Double? = null
        var minutes: Int? = null
        var distanceAt = 0L
        var timeAt = 0L
        fun finishPair() {
            if (!currentRead() || distanceAt == 0L || (!totalDistance && timeAt == 0L)) return
            val distance = meters
            val duration = minutes
            if (distance == null || !distance.isFinite() || distance < 0 || (!totalDistance && (duration == null || duration < 0 || abs(distanceAt - timeAt) > 1_000)) || SystemClock.elapsedRealtime() - readStarted > 5_000) {
                fail(token, "같은 구간의 유효한 이동 값을 확인하지 못했어요.")
                return
            }
            reading = false
            deadline?.let(main::removeCallbacks)
            onDiagnostic("sample distance=$distance m, time=$duration min")
            onSample(OdometerSample(distance, duration, System.currentTimeMillis(), SystemClock.elapsedRealtime(), if (totalDistance) OdometerSource.TOTAL else OdometerSource.TRIP))
            if (valid(token)) heartbeat = Runnable { requestSample() }.also { main.postDelayed(it, 5_000) }
        }
        try {
            if (totalDistance) {
                sdk.getOdometer().getCurrentDistanceDriven(
                    onComplete = { value -> main.post { if (currentRead()) {
                        meters = value?.toDouble()?.times(1000.0)
                        distanceAt = SystemClock.elapsedRealtime()
                        finishPair()
                    } } },
                    onFailed = { main.post { if (currentRead()) fail(token, "누적거리를 확인하지 못했어요.") } }
                )
                return
            }
            sdk.getOdometer().getTripCurrentDrivingDistance(
                onComplete = { value -> main.post { if (currentRead()) { meters = value?.toDouble(); distanceAt = SystemClock.elapsedRealtime(); finishPair() } } },
                onFailed = { main.post { if (currentRead()) fail(token, "이동 거리를 확인하지 못했어요.") } }
            )
            sdk.getOdometer().getTripCurrentDrivingTime(
                onComplete = { value -> main.post { if (currentRead()) { minutes = value; timeAt = SystemClock.elapsedRealtime(); finishPair() } } },
                onFailed = { main.post { if (currentRead()) fail(token, "주행 시간을 확인하지 못했어요.") } }
            )
        } catch (_: Exception) { fail(token, "이동 정보를 다시 확인하지 못했어요.") }
    }

    private fun fail(token: Int, reason: String) {
        if (token != generation) return
        onDiagnostic("FAILED: $reason")
        stop()
        onProblem(reason)
    }

    override fun stop() {
        generation++
        enabled = false
        reading = false
        deadline?.let(main::removeCallbacks)
        heartbeat?.let(main::removeCallbacks)
        deadline = null
        heartbeat = null
        try { distanceListener?.let { vehicle?.getOdometer()?.unregisterTripCurrentDrivingDistance(it) } } catch (_: Exception) { }
        try { timeListener?.let { vehicle?.getOdometer()?.unregisterTripCurrentDrivingTime(it) } } catch (_: Exception) { }
        distanceListener = null
        timeListener = null
        try { vehicle?.release() } catch (_: Exception) { }
        vehicle = null
    }
}
