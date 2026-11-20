package com.example.ui.components

import android.media.AudioManager
import android.media.ToneGenerator
import com.example.engine.BallOutcomeType

/**
 * Lightweight, zero-asset match sound cue synthesizer using Android ToneGenerator.
 * Fails silently on devices without audio stream support so playback never crashes.
 */
class MatchSoundEffects {
    private var toneGenerator: ToneGenerator? = null
    var muted: Boolean = false

    private fun ensureGenerator(): ToneGenerator? {
        if (muted) return null
        if (toneGenerator == null) {
            toneGenerator = runCatching {
                ToneGenerator(AudioManager.STREAM_MUSIC, 65)
            }.getOrNull()
        }
        return toneGenerator
    }

    fun playReleaseCue() {
        if (muted) return
        runCatching {
            ensureGenerator()?.startTone(ToneGenerator.TONE_PROP_BEEP, 45)
        }
    }

    fun playOutcomeCue(outcome: BallOutcomeType) {
        if (muted) return
        runCatching {
            val gen = ensureGenerator() ?: return
            when (outcome) {
                BallOutcomeType.SIX -> gen.startTone(ToneGenerator.TONE_PROP_ACK, 180)
                BallOutcomeType.FOUR -> gen.startTone(ToneGenerator.TONE_PROP_BEEP2, 135)
                BallOutcomeType.WICKET -> gen.startTone(ToneGenerator.TONE_CDMA_ABBR_ALERT, 220)
                BallOutcomeType.SINGLE, BallOutcomeType.DOUBLE, BallOutcomeType.TRIPLE ->
                    gen.startTone(ToneGenerator.TONE_PROP_BEEP, 55)
                BallOutcomeType.WIDE, BallOutcomeType.NO_BALL ->
                    gen.startTone(ToneGenerator.TONE_PROP_NACK, 90)
                BallOutcomeType.DOT -> Unit
            }
        }
    }

    fun release() {
        runCatching { toneGenerator?.release() }
        toneGenerator = null
    }
}
