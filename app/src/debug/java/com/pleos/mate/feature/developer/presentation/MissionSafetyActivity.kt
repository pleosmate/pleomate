package com.pleos.mate.feature.developer.presentation

import android.os.Bundle
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.lifecycle.Lifecycle
import com.pleos.mate.feature.companion.presentation.session.CompanionSession
import com.pleos.mate.data.local.CompanionPreferences
import com.pleos.mate.feature.growth.data.local.GrowthStore
import com.pleos.mate.feature.growth.data.local.GrowthRepository
import java.io.File

/** Live production mission logic with a separate temporary reward record for each trial. */
class MissionSafetyActivity : ComponentActivity() {
    private var trial by mutableStateOf<CompanionSession?>(null)
    private var trialId by mutableStateOf(0L)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        newTrial()
        setContent {
            val session = trial ?: return@setContent
            MaterialTheme {
                Surface(Modifier.fillMaxSize()) {
                    Column(Modifier.verticalScroll(rememberScrollState()).padding(32.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                        Text("미션 중단·복귀 검증", style = MaterialTheme.typography.headlineMedium)
                        Text("실제 Window·P·UX 상태와 제품 판정 코드를 사용합니다. 경험치와 기억은 이 검증의 임시 폴더에만 저장합니다.")
                        Text("먼저 가상 콘솔에서 P, Front Left 0%로 설정하세요. 미션 시작 후 P를 해제하거나 에뮬레이터 Home으로 나갔다 돌아오면 IDLE / 0 XP여야 합니다. 이후 창문을 열어도 자동 완료되면 안 됩니다.")
                        Text(session.parkingMessage)
                        Text("검증 번호: $trialId\n주차·UX 허용: ${session.parkedAndAllowed}\n단계: ${session.mission}\n현재 raw: ${session.rawPosition ?: "미수신"}\n검증 기록: Lv.${session.progress.level} / ${session.progress.totalXp} XP")
                        Text(session.message)
                        val busy = session.mission in setOf(CompanionSession.Mission.CHECKING, CompanionSession.Mission.WAITING, CompanionSession.Mission.VERIFYING, CompanionSession.Mission.SAVING)
                        Button(onClick = session::startMission,
                            enabled = session.loaded && session.parkedAndAllowed && !busy && session.progress.windCompletedAt == null,
                            modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp)) { Text("검증 미션 시작") }
                        OutlinedButton(onClick = { session.cancelMission("사용자가 취소한 검증입니다.") }, enabled = busy && session.mission != CompanionSession.Mission.SAVING,
                            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) { Text("미션 취소") }
                        OutlinedButton(onClick = ::newTrial, enabled = session.mission != CompanionSession.Mission.SAVING,
                            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) { Text("새 임시 기록으로 검증") }
                        OutlinedButton(onClick = { finish() }, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) { Text("제품 홈으로 돌아가기") }
                    }
                }
            }
        }
    }

    private fun newTrial() {
        trial?.close()
        trialId = SystemClock.elapsedRealtimeNanos()
        val directory = File(cacheDir, "mission-check-$trialId").apply { mkdirs() }
        trial = CompanionSession(this, GrowthRepository(GrowthStore(directory)), CompanionPreferences(this)).also {
            if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) it.resume()
        }
    }

    override fun onResume() { super.onResume(); trial?.resume() }
    override fun onPause() { trial?.pause(); super.onPause() }
    override fun onDestroy() { trial?.close(); super.onDestroy() }
}
