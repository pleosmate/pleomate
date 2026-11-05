package com.pleos.mate.feature.developer.presentation

import android.animation.ValueAnimator
import android.content.Context
import android.util.Log
import android.view.Choreographer
import android.view.TextureView
import android.widget.FrameLayout
import com.google.android.filament.IndirectLight
import com.google.android.filament.View
import com.google.android.filament.utils.ModelViewer
import com.google.android.filament.utils.Utils
import com.pleos.mate.R
import java.nio.ByteBuffer

/** Owns a fresh child TextureView per attachment; ModelViewer destroys itself on child detach. */
class GlbProbeView(context: Context, private val report: (String, Boolean) -> Unit) : FrameLayout(context), Choreographer.FrameCallback {
    private var viewer: ModelViewer? = null
    private var environment: IndirectLight? = null
    private var active = false
    private var paused = false
    private var scheduled = false
    private var loaded = false
    private var lastFrame = 0L
    private var elapsed = 0f
    private var greetingStart = -10f
    private var reportTime = 0L
    private var submitted = 0
    private var failed = false

    fun setActive(value: Boolean) {
        if (active == value) return
        active = value
        lastFrame = 0L
        reportTime = 0L
        submitted = 0
        if (value) schedule() else stopFrames()
    }

    fun setPaused(value: Boolean) {
        if (paused == value) return
        paused = value
        lastFrame = 0L
        if (!value || !loaded) schedule() else stopFrames()
        report(context.getString(if (!loaded) R.string.glb_loading else if (value) R.string.glb_paused else R.string.glb_running), loaded)
    }

    fun greet() {
        if (active && loaded && !paused && ValueAnimator.areAnimatorsEnabled()) {
            greetingStart = elapsed
            schedule()
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (viewer == null && !failed) createViewer()
        schedule()
    }

    private fun createViewer() {
        try {
            Utils.init()
            val texture = object : TextureView(context) {
                override fun onDetachedFromWindow() {
                    // Runs before ModelViewer's detach listener destroys its engine.
                    releaseEnvironment()
                    super.onDetachedFromWindow()
                }
            }
            // The helper releases its GPU resources automatically when this child detaches.
            val model = ModelViewer(texture)
            viewer = model
            addView(texture, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
            model.autoPlayAnimations = false
            val clear = model.renderer.clearOptions
            clear.clear = true
            clear.clearColor = doubleArrayOf(0.965, 0.96, 0.945, 1.0)
            model.renderer.clearOptions = clear
            model.view.dynamicResolutionOptions = model.view.dynamicResolutionOptions.apply { enabled = false }
            model.view.antiAliasing = View.AntiAliasing.FXAA
            model.view.multiSampleAntiAliasingOptions = model.view.multiSampleAntiAliasingOptions.apply {
                enabled = true
                sampleCount = 4
            }
            model.view.renderQuality = model.view.renderQuality.apply { hdrColorBuffer = View.QualityLevel.HIGH }
            model.view.ambientOcclusionOptions = model.view.ambientOcclusionOptions.apply {
                enabled = true
                radius = 0.15f
                intensity = 0.65f
                quality = View.QualityLevel.MEDIUM
            }
            environment = IndirectLight.Builder()
                .irradiance(1, floatArrayOf(0.65f, 0.63f, 0.60f))
                .intensity(25_000f).build(model.engine)
            model.scene.indirectLight = environment
            model.cameraFocalLength = 32f
            val lights = model.engine.lightManager
            val light = lights.getInstance(model.light)
            lights.setDirection(light, -0.36f, -0.72f, -0.59f)
            lights.setIntensity(light, 65_000f)
            lights.setSunAngularRadius(light, 1.8f)
            val bytes = context.assets.open("models/companion-probe.glb").use { it.readBytes() }
            val buffer = ByteBuffer.allocateDirect(bytes.size).apply { put(bytes); flip() }
            model.loadModelGlb(buffer)
            checkNotNull(model.asset) { "GLB asset creation failed" }
            check(model.animator?.animationCount == 2) { "Expected Idle and Greeting clips" }
            model.transformToUnitCube()
            loaded = false
            report(context.getString(R.string.glb_loading), false)
        } catch (e: Exception) {
            fail(e)
        } catch (e: LinkageError) {
            fail(e)
        }
    }

    private fun fail(error: Throwable) {
        failed = true
        stopFrames()
        Log.e("GlbProbe", "3D probe failed", error)
        // Detachment lets ModelViewer release any fully constructed native resources.
        removeAllViews()
        viewer = null
        report(context.getString(R.string.glb_error, "${error.javaClass.simpleName}: ${error.message}"), false)
    }

    private fun schedule() {
        if (active && isAttachedToWindow && viewer != null && !failed && (!paused || !loaded) && !scheduled) {
            scheduled = true
            Choreographer.getInstance().postFrameCallback(this)
        }
    }

    private fun stopFrames() {
        Choreographer.getInstance().removeFrameCallback(this)
        scheduled = false
    }

    override fun doFrame(frameTimeNanos: Long) {
        scheduled = false
        val model = viewer ?: return
        if (!active || failed) return
        try {
            val animate = !paused && ValueAnimator.areAnimatorsEnabled()
            if (lastFrame != 0L && animate) elapsed += ((frameTimeNanos - lastFrame) / 1_000_000_000.0).toFloat()
            lastFrame = frameTimeNanos
            model.animator?.let { animator ->
                val idleDuration = animator.getAnimationDuration(0)
                animator.applyAnimation(0, if (idleDuration > 0f) elapsed % idleDuration else 0f)
                val helloTime = elapsed - greetingStart
                animator.applyAnimation(1, if (helloTime in 0f..1.5f) helloTime else 0f)
                animator.updateBoneMatrices()
            }
            if (model.render(frameTimeNanos)) submitted++
            if (!loaded && model.progress >= 1f) {
                loaded = true
                Log.i("GlbProbe", "GLB resources loaded; Idle/Greeting available")
                report(context.getString(if (paused) R.string.glb_paused else R.string.glb_loaded), true)
            }
            if (loaded && !paused && frameTimeNanos - reportTime >= 1_000_000_000L) {
                if (reportTime != 0L) report(context.getString(R.string.glb_render_count, submitted), true)
                submitted = 0
                reportTime = frameTimeNanos
            }
            // With system animations disabled, finish loading then keep a static frame.
            if (animate || !loaded) schedule()
        } catch (e: Exception) {
            fail(e)
        }
    }

    private fun releaseEnvironment() {
        val light = environment ?: return
        environment = null
        viewer?.let { model ->
            model.scene.indirectLight = null
            model.engine.destroyIndirectLight(light)
        }
    }

    fun close() {
        stopFrames()
        removeAllViews()
        viewer = null
        loaded = false
        lastFrame = 0L
        reportTime = 0L
        submitted = 0
    }

    override fun onDetachedFromWindow() {
        close()
        super.onDetachedFromWindow()
    }
}
