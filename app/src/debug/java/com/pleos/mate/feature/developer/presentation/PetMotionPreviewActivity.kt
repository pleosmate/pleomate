package com.pleos.mate.feature.developer.presentation

import android.animation.ValueAnimator
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.pleos.mate.R
import com.pleos.mate.core.pet.model.PetCharacter
import com.pleos.mate.core.pet.presentation.labelResource
import com.pleos.mate.core.pet.model.PetMotion
import com.pleos.mate.core.pet.model.PetMotionRules
import com.pleos.mate.core.pet.model.PetRunDirection
import com.pleos.mate.core.pet.rendering.PetMotionView

class PetMotionPreviewActivity : AppCompatActivity() {
    private lateinit var pet: PetMotionView
    private lateinit var greeting: TextView
    private lateinit var greetButton: Button
    private lateinit var restrictionButton: Button
    private var restricted = false
    private var firstGreeting = true
    private lateinit var runDirections: RadioGroup

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pet_motion_preview)
        pet = findViewById(R.id.pet_motion)
        pet.level = intent.getIntExtra("preview_level", 1)
        val walking = findViewById<android.widget.CheckBox>(R.id.pet_preview_walking)
        walking.isChecked = savedInstanceState?.getBoolean("walking") ?: intent.getBooleanExtra("preview_walking", false)
        pet.walking = walking.isChecked
        walking.setOnCheckedChangeListener { _, checked -> pet.walking = checked }
        pet.character = PetCharacter.fromId(savedInstanceState?.getString("character") ?: intent.getStringExtra("preview_character")) ?: PetCharacter.BEAR
        findViewById<TextView>(R.id.pet_preview_level).text = "Lv.${pet.level} · 모션 미리보기"
        greeting = findViewById(R.id.pet_greeting)
        greetButton = findViewById(R.id.pet_greet)
        restrictionButton = findViewById(R.id.pet_restriction)
        restricted = savedInstanceState?.getBoolean("restricted") ?: false
        firstGreeting = savedInstanceState == null
        val characters = findViewById<RadioGroup>(R.id.pet_preview_characters)
        PetCharacter.entries.forEach { character ->
            characters.addView(RadioButton(this).apply {
                id = View.generateViewId()
                text = getString(character.labelResource)
                isChecked = character == pet.character
                setOnClickListener { pet.character = character }
            })
        }
        runDirections = findViewById(R.id.pet_preview_run_directions)
        (listOf(getString(R.string.pet_preview_idle)) + PetRunDirection.entries.map { it.label }).forEach { label ->
            runDirections.addView(RadioButton(this).apply { id = View.generateViewId(); text = label })
        }
        runDirections.setOnCheckedChangeListener { group, checkedId ->
            pet.runningDirection = PetRunDirection.entries.getOrNull(group.indexOfChild(group.findViewById(checkedId)) - 1)
        }
        val direction = savedInstanceState?.getString("run_direction") ?: intent.getStringExtra("preview_run_direction")
        val row = PetRunDirection.entries.indexOfFirst { it.name == direction }
        runDirections.check(runDirections.getChildAt(row + 1).id)
        val motions = findViewById<android.widget.LinearLayout>(R.id.pet_preview_motions)
        PetMotion.entries.forEach { motion ->
            motions.addView(Button(this).apply {
                text = motion.label
                isEnabled = PetMotionRules.canPlay(pet.level, motion)
                setOnClickListener {
                    runDirections.check(runDirections.getChildAt(0).id)
                    pet.playMotion(motion)
                }
            })
        }
        greetButton.setOnClickListener {
            runDirections.check(runDirections.getChildAt(0).id)
            playPreview()
        }
        restrictionButton.setOnClickListener {
            restricted = !restricted
            updateState()
        }
        updateState()
    }

    private fun playPreview() {
        val motion = PetMotion.entries.firstOrNull { it.name == intent.getStringExtra("preview_motion") }
        if (motion == null) pet.greet() else pet.playMotion(motion)
    }

    private fun updateState() {
        pet.setRestricted(restricted)
        val motionAllowed = !restricted && ValueAnimator.areAnimatorsEnabled()
        greetButton.isEnabled = motionAllowed
        for (index in 0 until runDirections.childCount) runDirections.getChildAt(index).isEnabled = motionAllowed
        greeting.setText(if (restricted) R.string.pet_paused else R.string.pet_hello)
        restrictionButton.setText(if (restricted) R.string.pet_resume else R.string.pet_restrict)
    }

    override fun onResume() {
        super.onResume()
        updateState()
        pet.setHostActive(true)
        if (firstGreeting) {
            pet.post {
                playPreview()
                firstGreeting = false
            }
        }
    }

    override fun onPause() {
        pet.setHostActive(false)
        super.onPause()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean("restricted", restricted)
        outState.putBoolean("walking", pet.walking)
        outState.putString("character", pet.character.id)
        outState.putString("run_direction", pet.runningDirection?.name)
        super.onSaveInstanceState(outState)
    }
}
