package com.example.ui.components

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.example.engine.BallOutcomeType
import com.example.engine.TimingGrade

/**
 * Lightweight, zero-asset cricket audio & Android tactile haptic feedback engine.
 * Synthesizes crisp bat hits, perfect-timing six cracks + crowd cheer pulses,
 * boundary chimes, wicket rattles, and DRS radar pings with Android Vibrator feedback.
 * All hardware/audio calls are wrapped in runCatching so emulators and silent devices never crash.
 */
class MatchSoundEffects(context: Context? = null) {
    private val appContext: Context? = context?.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())
    private var toneGenerator: ToneGenerator? = null
    var muted: Boolean = false

    private fun ensureGenerator(): ToneGenerator? {
        if (muted) return null
        if (toneGenerator == null) {
            toneGenerator = runCatching {
                ToneGenerator(AudioManager.STREAM_MUSIC, 68)
            }.getOrNull()
        }
        return toneGenerator
    }

    private fun vibrateOneShot(durationMs: Long, amplitude: Int) {
        if (muted) return
        val ctx = appContext ?: return
        runCatching {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = ctx.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                ctx.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            } ?: return
            if (!vibrator.hasVibrator()) return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(durationMs, amplitude.coerceIn(1, 255)))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(durationMs)
            }
        }
    }

    private fun vibrateWaveform(timings: LongArray, amplitudes: IntArray) {
        if (muted) return
        val ctx = appContext ?: return
        runCatching {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = ctx.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                ctx.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            } ?: return
            if (!vibrator.hasVibrator()) return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(timings, -1)
            }
        }
    }

    fun playReleaseCue() {
        if (muted) return
        runCatching {
            ensureGenerator()?.startTone(ToneGenerator.TONE_PROP_BEEP, 35)
        }
    }

    fun playDrsRadarCue() {
        if (muted) return
        runCatching {
            ensureGenerator()?.startTone(ToneGenerator.TONE_CDMA_PIP, 95)
            vibrateOneShot(22L, 90)
        }
    }

    fun playDrsDecisionCue(overturned: Boolean) {
        if (muted) return
        runCatching {
            val gen = ensureGenerator()
            if (overturned) {
                gen?.startTone(ToneGenerator.TONE_PROP_ACK, 180)
                vibrateWaveform(longArrayOf(0L, 35L, 50L, 55L), intArrayOf(0, 140, 0, 220))
            } else {
                gen?.startTone(ToneGenerator.TONE_PROP_BEEP2, 140)
                vibrateOneShot(45L, 160)
            }
        }
    }

    fun playOutcomeCue(outcome: BallOutcomeType, timingGrade: TimingGrade? = null) {
        if (muted) return
        runCatching {
            val gen = ensureGenerator()
            when (outcome) {
                BallOutcomeType.SIX -> {
                    // Crisp bat crack followed by crowd cheer swell & celebratory double-pulse haptic
                    gen?.startTone(ToneGenerator.TONE_PROP_ACK, 165)
                    val isPerfect = timingGrade == TimingGrade.PERFECT
                    vibrateWaveform(
                        timings = if (isPerfect) longArrayOf(0L, 42L, 45L, 75L) else longArrayOf(0L, 36L, 50L, 50L),
                        amplitudes = if (isPerfect) intArrayOf(0, 210, 0, 255) else intArrayOf(0, 175, 0, 215)
                    )
                    mainHandler.postDelayed({
                        if (!muted) {
                            runCatching { ensureGenerator()?.startTone(ToneGenerator.TONE_CDMA_ANSWER, 210) }
                        }
                    }, 180L)
                }
                BallOutcomeType.FOUR -> {
                    gen?.startTone(ToneGenerator.TONE_PROP_BEEP2, 130)
                    vibrateOneShot(42L, 185)
                    mainHandler.postDelayed({
                        if (!muted) {
                            runCatching { ensureGenerator()?.startTone(ToneGenerator.TONE_PROP_ACK, 95) }
                        }
                    }, 145L)
                }
                BallOutcomeType.WICKET -> {
                    // Timber rattle & sharp wicket vibration
                    gen?.startTone(ToneGenerator.TONE_CDMA_ABBR_ALERT, 210)
                    vibrateWaveform(longArrayOf(0L, 70L, 45L, 65L), intArrayOf(0, 240, 0, 200))
                }
                BallOutcomeType.SINGLE, BallOutcomeType.DOUBLE, BallOutcomeType.TRIPLE -> {
                    // Crisp wood bat hit
                    gen?.startTone(ToneGenerator.TONE_PROP_BEEP, 50)
                    vibrateOneShot(20L, 110)
                }
                BallOutcomeType.WIDE, BallOutcomeType.NO_BALL -> {
                    gen?.startTone(ToneGenerator.TONE_PROP_NACK, 90)
                    vibrateOneShot(25L, 95)
                }
                BallOutcomeType.DOT -> {
                    vibrateOneShot(12L, 65)
                }
            }
        }
    }

    fun release() {
        runCatching { mainHandler.removeCallbacksAndMessages(null) }
        runCatching { toneGenerator?.release() }
        toneGenerator = null
    }
}
