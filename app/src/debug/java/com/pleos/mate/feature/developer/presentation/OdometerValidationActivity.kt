package com.pleos.mate.feature.developer.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pleos.mate.feature.journey.data.vehicle.OdometerObserver

/** Read-only live SDK probe. Never writes journey or growth records. */
class OdometerValidationActivity : ComponentActivity() {
    private var events by mutableStateOf(emptyList<String>())
    private var running by mutableStateOf(false)
    private lateinit var observer: OdometerObserver

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        observer = OdometerObserver(this,
            onSample = { log("방식=${it.source} / 거리=${it.meters} m / 주행=${it.minutes?.let { value -> "$value min" } ?: "확인 불가"}\nwall=${it.wallMillis}, elapsed=${it.elapsedMillis}") },
            onProblem = { running = false; log(it) },
            onDiagnostic = ::log)
        setContent {
            MaterialTheme {
                Surface {
                    Column(Modifier.verticalScroll(rememberScrollState()).padding(32.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text("Odometer SDK 검증", style = MaterialTheme.typography.headlineMedium)
                        Text("현재 트립 거리·시간을 읽고, 미지원이면 누적거리를 확인합니다. 기록과 경험치를 저장하지 않습니다. 차량을 주차한 상태에서 가상 콘솔 값과 비교하세요.")
                        Text("SDK 2.3.2: 트립 m / 주행 시간 min, 누적거리 km는 m로 변환. 콘솔 매핑·트립 초기화 시점은 실제 수신 값으로 확인해야 합니다.")
                        Button(onClick = { running = true; events = emptyList(); observer.start() }, enabled = !running,
                            modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp)) { Text("조회·구독 시작") }
                        OutlinedButton(onClick = { observer.stop(); running = false }, enabled = running,
                            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) { Text("수신 중지") }
                        OutlinedButton(onClick = { finish() }, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) { Text("홈으로 돌아가기") }
                        events.forEach { Text(it) }
                    }
                }
            }
        }
    }

    private fun log(text: String) { events = (listOf(text) + events).take(30) }
    override fun onPause() { observer.stop(); running = false; super.onPause() }
    override fun onDestroy() { observer.stop(); super.onDestroy() }
}
