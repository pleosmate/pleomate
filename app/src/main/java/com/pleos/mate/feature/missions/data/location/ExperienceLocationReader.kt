package com.pleos.mate.feature.missions.data.location

import ai.pleos.playground.location.fused.FusedLocation
import ai.pleos.playground.location.fused.listener.FusedLocationListener
import ai.pleos.playground.location.fused.model.LocationData
import ai.pleos.playground.navi.constants.NaviErrorCode
import ai.pleos.playground.navi.data.DestinationInfo
import ai.pleos.playground.navi.helper.NaviHelper
import ai.pleos.playground.navi.helper.listener.NaviHelperEventListener
import android.content.Context
import android.os.Handler
import android.os.Looper
import com.pleos.mate.feature.missions.domain.ExperienceRules

/** Explicit foreground query; no coordinates are persisted. */
internal class ExperienceLocationReader(context: Context) {
    private val context = context.applicationContext
    private val main = Handler(Looper.getMainLooper())
    private var fused: FusedLocation? = null
    private var navi: NaviHelper? = null
    private var locationListener: FusedLocationListener? = null
    private var naviListener: NaviHelperEventListener? = null
    private var active = false
    private var fix: LocationData? = null
    private var destination: DestinationInfo? = null
    private var needsDestination = false
    private var callback: (String?, String?) -> Unit = { _, _ -> }
    private val timeout = Runnable { finish(null, "새 위치 또는 목적지 정보를 확인하지 못했어요. 위치 공유와 내비게이션 연결을 확인해 주세요.") }

    fun read(needsDestination: Boolean, callback: (String?, String?) -> Unit) {
        this.needsDestination = needsDestination
        this.callback = callback
        active = true
        main.postDelayed(timeout, 15000)
        guarded {
            val location = FusedLocation(context)
            fused = location
            if (!location.isSupported()) {
                finish(null, "이 환경은 Pleos 위치 서비스를 지원하지 않아요.")
                return@guarded
            }
            val listener = object : FusedLocationListener {
                override fun onChanged(location: LocationData?) = dispatch {
                    if (location != null && ExperienceRules.validCoordinate(location.latitude, location.longitude) &&
                        ExperienceRules.fresh(location.timestampMillis, System.currentTimeMillis()) && location.confidence.isFinite() && location.confidence >= 0f) {
                        fix = location
                        resolve()
                    }
                }
                override fun onError(exception: Exception) = dispatch { finish(null, "위치를 읽을 수 없어요. 위치 권한과 차량의 위치 공유 설정을 확인해 주세요.") }
            }
            locationListener = listener
            location.initialize()
            location.registerFusedLocationCallback(listener)
            location.getLastKnownLocation(listener)
            if (needsDestination) {
                val helper = NaviHelper(context)
                navi = helper
                val events = object : NaviHelperEventListener {
                    override fun onDestinationInfo(destinationInfo: DestinationInfo) = dispatch {
                        if (destinationInfo.name.isBlank() || !ExperienceRules.validCoordinate(destinationInfo.latitude, destinationInfo.longitude) ||
                            (destinationInfo.latitude == 0.0 && destinationInfo.longitude == 0.0)) {
                            finish(null, "현재 설정된 목적지가 없어요. 내비게이션에서 목적지를 설정한 뒤 다시 확인해 주세요.")
                        } else { destination = destinationInfo; resolve() }
                    }
                    override fun onDestinationChanged() = dispatch { finish(null, "목적지가 변경됐어요. 다시 확인해 주세요.") }
                    override fun onError(errorCode: NaviErrorCode) = dispatch { finish(null, "목적지를 읽지 못했어요. 내비게이션 연결 상태를 확인해 주세요.") }
                }
                naviListener = events
                helper.initialize(onConnected = { dispatch { helper.addListener(events); helper.getDestinationInfo() } },
                    onFailed = { dispatch { finish(null, "내비게이션 서비스에 연결할 수 없어요.") } })
            }
        }
    }

    private fun resolve() {
        val current = fix ?: return
        if (!needsDestination) finish("현재 위치 확인 완료 · 장소 이름만 저장하며 좌표는 저장하지 않아요.", null)
        else {
            val target = destination ?: return
            if (!ExperienceRules.near(current.latitude, current.longitude, target.latitude, target.longitude))
                finish(null, "아직 목적지 근처가 아니에요. 평소 이동을 마친 뒤 목적지 150m 이내에서 주차하고 다시 확인해 주세요.")
            else finish(target.name.take(ExperienceRules.MAX_TEXT), null)
        }
    }

    fun cancel() {
        active = false
        main.removeCallbacks(timeout)
        try { locationListener?.let { fused?.unregisterFusedLocationCallback(it) } } catch (_: Exception) { }
        try { fused?.release() } catch (_: Exception) { }
        try { naviListener?.let { navi?.removeListener(it) } } catch (_: Exception) { }
        try { navi?.release() } catch (_: Exception) { }
        fused = null; navi = null; fix = null; destination = null
    }

    private fun guarded(block: () -> Unit) {
        if (!active) return
        try { block() } catch (_: Exception) { finish(null, "위치·내비게이션에 연결하지 못했어요. 권한과 지원 상태를 확인해 주세요.") }
    }
    private fun dispatch(block: () -> Unit) { main.post { guarded(block) } }
    private fun finish(value: String?, error: String?) {
        if (!active) return
        cancel()
        callback(value, error)
    }
}
