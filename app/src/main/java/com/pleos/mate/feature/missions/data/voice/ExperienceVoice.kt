package com.pleos.mate.feature.missions.data.voice

import ai.pleos.playground.stt.SpeechToText
import ai.pleos.playground.stt.constant.ErrorType as SttError
import ai.pleos.playground.stt.constant.Mode as SttMode
import ai.pleos.playground.stt.listener.OnInitializeListener as SttInitialize
import ai.pleos.playground.stt.listener.ResultListener
import ai.pleos.playground.tts.TextToSpeech
import ai.pleos.playground.tts.constant.ErrorType as TtsError
import ai.pleos.playground.tts.constant.Mode as TtsMode
import ai.pleos.playground.tts.listener.EventListener
import ai.pleos.playground.tts.listener.OnInitializeListener as TtsInitialize
import android.content.Context
import android.os.Handler
import android.os.Looper
import com.pleos.mate.feature.missions.domain.ExperienceRules
import java.util.Locale

internal class ExperienceVoice(context: Context) {
    private val context = context.applicationContext
    private val main = Handler(Looper.getMainLooper())
    private var stt: SpeechToText? = null
    private var tts: TextToSpeech? = null
    private var sttListener: ResultListener? = null
    private var ttsListener: EventListener? = null
    private var active = false
    private var listening = false
    private var sttStopping = false
    private var ttsStopping = false
    private var result: (String?, String?) -> Unit = { _, _ -> }
    private var status: (String) -> Unit = {}
    private val timeout = Runnable { finish(null, "음성 응답 시간이 지났어요. 다시 시도하거나 한마디를 직접 입력해 주세요.") }

    fun start(greeting: Boolean, onStatus: (String) -> Unit, onResult: (String?, String?) -> Unit) {
        active = true
        result = onResult
        status = onStatus
        main.postDelayed(timeout, 45000)
        if (greeting) speakGreeting() else recognize()
    }

    private fun speakGreeting() = guarded {
        status("동료의 인사를 준비하고 있어요.")
        val speaker = TextToSpeech(context, TtsMode.ON_DEVICE)
        tts = speaker
        val events = object : EventListener {
            override fun onStart() = dispatch { status("동료의 인사를 듣고, 이어서 인사해 주세요.") }
            override fun onReady() {}
            override fun onDone() = dispatch {
                if (tts !== speaker) return@dispatch
                releaseSpeaker(speaker)
                recognize()
            }
            override fun onError(errorType: TtsError) = dispatch { if (tts === speaker) finish(null, voiceError("TTS", errorType.name)) }
            override fun onStop() {
                main.post {
                    if (!active) releaseSpeaker(speaker)
                    else if (tts === speaker) finish(null, "인사 재생이 중단됐어요. 다시 시도해 주세요.")
                }
            }
            override fun onUpdatedRms(rms: Double) {}
        }
        ttsListener = events
        speaker.initialize(object : TtsInitialize {
            override fun onSuccess() = dispatch {
                speaker.addEventListener(events)
                speaker.setLocale(Locale.KOREA)
                speaker.speak("안녕! 나는 너의 이동 동료야. 앞으로 함께 추억을 만들어 보자. 나에게 인사해 줄래?")
            }
            override fun onFailure(errorType: TtsError) = dispatch { finish(null, voiceError("TTS", errorType.name)) }
        })
    }

    private fun recognize() = guarded {
        if (listening) return@guarded
        listening = true
        status("음성 입력을 준비하고 있어요.")
        val recognizer = SpeechToText(context, SttMode.ON_DEVICE)
        stt = recognizer
        val events = object : ResultListener {
            override fun onUpdated(stt: String, completed: Boolean) = dispatch {
                if (completed && stt.isNotBlank()) finish(stt.trim().take(ExperienceRules.MAX_TEXT), null)
            }
            override fun onUpdatedEpdData(on: Long, off: Long) {}
            override fun onStartedRecognition() = dispatch { status("듣고 있어요. 오늘의 한마디를 말해 주세요.") }
            override fun onEndedRecognition() {
                main.post {
                    if (!active) releaseRecognizer(recognizer)
                    else main.postDelayed({ guarded { finish(null, "인식된 말이 없어요. 다시 말하거나 한마디를 직접 입력해 주세요.") } }, 500)
                }
            }
            override fun onError(errorType: SttError) = dispatch { finish(null, voiceError("STT", errorType.name)) }
            override fun onReady() = dispatch { status("마이크가 켜졌어요. 말해 주세요.") }
            override fun onUpdatedRms(rms: Double) {}
        }
        sttListener = events
        recognizer.initialize(object : SttInitialize {
            override fun onSuccess() = dispatch {
                recognizer.addListener(events)
                recognizer.setLocale(Locale.KOREA)
                recognizer.request()
            }
            override fun onFailure(errorType: SttError) = dispatch { finish(null, voiceError("STT", errorType.name)) }
        })
    }

    fun cancel() {
        active = false
        main.removeCallbacks(timeout)
        stt?.let { recognizer ->
            if (!sttStopping) {
                sttStopping = true
                try { recognizer.stop() } catch (_: Exception) { releaseRecognizer(recognizer) }
                // stop() is asynchronous. Disconnecting now can discard the microphone-close request.
                main.postDelayed({ releaseRecognizer(recognizer) }, 5000)
            }
        }
        tts?.let { speaker ->
            if (!ttsStopping) {
                ttsStopping = true
                try { speaker.stop() } catch (_: Exception) { releaseSpeaker(speaker) }
                main.postDelayed({ releaseSpeaker(speaker) }, 5000)
            }
        }
    }

    private fun releaseRecognizer(recognizer: SpeechToText) {
        if (stt !== recognizer) return
        stt = null
        try { sttListener?.let(recognizer::removeListener) } catch (_: Exception) { }
        try { recognizer.release() } catch (_: Exception) { }
    }

    private fun releaseSpeaker(speaker: TextToSpeech) {
        if (tts !== speaker) return
        tts = null
        try { ttsListener?.let(speaker::removeEventListener) } catch (_: Exception) { }
        try { speaker.release() } catch (_: Exception) { }
    }

    private fun voiceError(stage: String, code: String): String {
        android.util.Log.w("CompanionVoice", "$stage: $code")
        return when (code) {
            "PERMISSION_DENIED" -> "음성 서비스 접근 권한이 없어요. 차량의 앱 권한 설정을 확인해 주세요."
            "UNAVAILABLE_LANGUAGE" -> "한국어 음성 엔진을 사용할 수 없어요. 차량의 음성 언어 설정을 확인해 주세요."
            "ON_DEVICE_ENGINE_ERROR", "NOT_SUPPORTED_API" -> "이 환경에서 기기 내 음성 엔진을 사용할 수 없어요. 차량 음성 서비스 설정을 확인해 주세요."
            "AUDIO_ERROR", "MIC_ERROR" -> "차량의 소리 출력 또는 마이크를 사용할 수 없어요. 오디오 설정을 확인해 주세요."
            else -> "음성 처리를 완료하지 못했어요. 다시 시도하거나 한마디를 직접 입력해 주세요."
        }
    }

    private fun guarded(block: () -> Unit) {
        if (!active) return
        try { block() } catch (_: Exception) { finish(null, "음성 서비스에 연결하지 못했어요. 지원 환경과 권한을 확인해 주세요.") }
    }
    private fun dispatch(block: () -> Unit) { main.post { guarded(block) } }
    private fun finish(text: String?, error: String?) {
        if (!active) return
        cancel()
        result(text, error)
    }
}
