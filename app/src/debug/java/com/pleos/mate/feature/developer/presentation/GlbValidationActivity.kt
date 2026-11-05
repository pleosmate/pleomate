package com.pleos.mate.feature.developer.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.pleos.mate.R

/** Debug-only Compose home study. Uses one shared probe, not either final character model. */
class GlbValidationActivity : ComponentActivity() {
    private var probe: GlbProbeView? = null
    private var resumed by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()) {
                var status by remember { mutableStateOf(getString(R.string.glb_loading)) }
                var ready by remember { mutableStateOf(false) }
                var paused by remember { mutableStateOf(false) }
                DisposableEffect(Unit) { onDispose { probe?.close(); probe = null } }
                Surface(Modifier.fillMaxSize()) {
                    Column(
                        Modifier.windowInsetsPadding(WindowInsets.safeDrawing)
                            .verticalScroll(rememberScrollState()).padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(stringResource(R.string.glb_title), style = MaterialTheme.typography.headlineMedium)
                        Text(stringResource(R.string.glb_notice))
                        AndroidView(
                            modifier = Modifier.fillMaxWidth().height(380.dp),
                            factory = { context -> GlbProbeView(context) { text, loaded -> status = text; ready = loaded }.also { probe = it } },
                            update = { it.setActive(resumed); it.setPaused(paused) },
                            onRelease = { it.close(); if (probe === it) probe = null }
                        )
                        Text(status, style = MaterialTheme.typography.bodyMedium)
                        Button(
                            onClick = { probe?.greet() }, enabled = ready && !paused,
                            modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp)
                        ) { Text(stringResource(R.string.glb_greet)) }
                        OutlinedButton(
                            onClick = { paused = !paused }, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)
                        ) { Text(stringResource(if (paused) R.string.glb_resume else R.string.glb_pause)) }
                        OutlinedButton(onClick = { finish() }, modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(R.string.glb_back))
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        resumed = true
        probe?.setActive(true)
    }

    override fun onPause() {
        resumed = false
        probe?.setActive(false)
        super.onPause()
    }
}
