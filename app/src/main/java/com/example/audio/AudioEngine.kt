package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

/**
 * High-performance procedural synthesizer using Android AudioTrack.
 * Generates punchy retro-arcade SFX and a dynamic synthwave soundtrack
 * with zero external audio assets required.
 */
class AudioEngine {

    private val sampleRate = 22050
    var soundEnabled: Boolean = true
    var musicEnabled: Boolean = true
    var sfxVolume: Float = 0.8f
    var musicVolume: Float = 0.5f

    private var musicJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default)

    // Pre-synthesized SFX buffers for zero-latency instant playback
    private val sfxBlaster: ShortArray = generateLaser(freqStart = 850.0, freqEnd = 240.0, durationMs = 110)
    private val sfxShotgun: ShortArray = generateShotgunBlast()
    private val sfxRailgun: ShortArray = generateRailgunZap()
    private val sfxKatana: ShortArray = generateBladeWhoosh()
    private val sfxJump: ShortArray = generatePitchSweep(freqStart = 180.0, freqEnd = 440.0, durationMs = 120)
    private val sfxDoubleJump: ShortArray = generatePitchSweep(freqStart = 320.0, freqEnd = 680.0, durationMs = 140)
    private val sfxDash: ShortArray = generateDashWhoosh()
    private val sfxWallRun: ShortArray = generateWallRunHum()
    private val sfxSlide: ShortArray = generateSlideScrape()
    private val sfxHitMarker: ShortArray = generateHitMarker()
    private val sfxExplosion: ShortArray = generateExplosion()
    private val sfxPickup: ShortArray = generatePickupChime()
    private val sfxPlayerHurt: ShortArray = generatePitchSweep(freqStart = 200.0, freqEnd = 90.0, durationMs = 180)

    fun playBlaster() = playSample(sfxBlaster, volume = 0.7f)
    fun playShotgun() = playSample(sfxShotgun, volume = 0.9f)
    fun playRailgun() = playSample(sfxRailgun, volume = 0.85f)
    fun playKatana() = playSample(sfxKatana, volume = 0.75f)
    fun playJump() = playSample(sfxJump, volume = 0.6f)
    fun playDoubleJump() = playSample(sfxDoubleJump, volume = 0.65f)
    fun playDash() = playSample(sfxDash, volume = 0.8f)
    fun playWallRun() = playSample(sfxWallRun, volume = 0.45f)
    fun playSlide() = playSample(sfxSlide, volume = 0.5f)
    fun playHitMarker() = playSample(sfxHitMarker, volume = 0.8f)
    fun playExplosion() = playSample(sfxExplosion, volume = 0.85f)
    fun playPickup() = playSample(sfxPickup, volume = 0.7f)
    fun playPlayerHurt() = playSample(sfxPlayerHurt, volume = 0.8f)

    private fun playSample(sample: ShortArray, volume: Float = 1.0f) {
        if (!soundEnabled) return
        val finalVol = (volume * sfxVolume).coerceIn(0f, 1f)
        if (finalVol <= 0.01f) return

        scope.launch {
            try {
                val minBufferSize = AudioTrack.getMinBufferSize(
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                )
                val bufferSize = maxOf(sample.size * 2, minBufferSize)
                val track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(bufferSize)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()

                // Apply volume scaling
                val scaled = ShortArray(sample.size) { i ->
                    (sample[i] * finalVol).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                }
                track.write(scaled, 0, scaled.size)
                track.play()
                delay((sample.size * 1000L / sampleRate) + 50)
                track.stop()
                track.release()
            } catch (_: Exception) {
            }
        }
    }

    /**
     * Dynamic Synthwave background rhythm generator.
     * Generates an iconic retro synth bassline + drum beats.
     */
    fun startDynamicBgm() {
        if (musicJob != null && musicJob?.isActive == true) return
        musicJob = scope.launch {
            val bassFreqs = doubleArrayOf(
                110.0, 110.0, 130.81, 110.0,
                146.83, 110.0, 164.81, 146.83,
                98.0, 98.0, 110.0, 98.0,
                130.81, 98.0, 110.0, 123.47
            )
            var step = 0
            while (isActive) {
                if (musicEnabled && musicVolume > 0.05f) {
                    val freq = bassFreqs[step % bassFreqs.size]
                    val isKick = (step % 4 == 0)
                    val isSnare = (step % 4 == 2)
                    val isHiHat = (step % 2 == 1)

                    val chordSample = generateSynthBeat(freq, isKick, isSnare, isHiHat, durationMs = 130)
                    playSample(chordSample, volume = musicVolume * 0.45f)
                }
                step++
                delay(135) // ~111 BPM energetic cyber tempo
            }
        }
    }

    fun stopDynamicBgm() {
        musicJob?.cancel()
        musicJob = null
    }

    // --- Sound Synthesis Algorithms ---

    private fun generateLaser(freqStart: Double, freqEnd: Double, durationMs: Int): ShortArray {
        val count = (sampleRate * (durationMs / 1000.0)).toInt()
        val buffer = ShortArray(count)
        var phase = 0.0
        for (i in 0 until count) {
            val progress = i.toDouble() / count
            val freq = freqStart + (freqEnd - freqStart) * progress
            phase += 2.0 * PI * freq / sampleRate
            val envelope = 1.0 - progress
            val wave = sin(phase) + 0.3 * sin(phase * 2.0)
            buffer[i] = (wave * envelope * 24000.0).toInt().coerceIn(-32767, 32767).toShort()
        }
        return buffer
    }

    private fun generateShotgunBlast(): ShortArray {
        val durationMs = 240
        val count = (sampleRate * (durationMs / 1000.0)).toInt()
        val buffer = ShortArray(count)
        var phase = 0.0
        for (i in 0 until count) {
            val progress = i.toDouble() / count
            val freq = 140.0 * (1.0 - progress * 0.8)
            phase += 2.0 * PI * freq / sampleRate
            val subBass = sin(phase)
            val noise = Random.nextDouble(-1.0, 1.0)
            val envelope = (1.0 - progress) * (1.0 - progress)
            val mixed = subBass * 0.5 + noise * 0.5
            buffer[i] = (mixed * envelope * 28000.0).toInt().coerceIn(-32767, 32767).toShort()
        }
        return buffer
    }

    private fun generateRailgunZap(): ShortArray {
        val durationMs = 300
        val count = (sampleRate * (durationMs / 1000.0)).toInt()
        val buffer = ShortArray(count)
        var phase1 = 0.0
        var phase2 = 0.0
        for (i in 0 until count) {
            val progress = i.toDouble() / count
            val freq = 1200.0 - progress * 800.0
            phase1 += 2.0 * PI * freq / sampleRate
            phase2 += 2.0 * PI * (freq * 1.5) / sampleRate
            val envelope = Math.exp(-progress * 4.0)
            val wave = (sin(phase1) + 0.5 * sin(phase2) + 0.2 * Random.nextDouble(-1.0, 1.0)) * envelope
            buffer[i] = (wave * 26000.0).toInt().coerceIn(-32767, 32767).toShort()
        }
        return buffer
    }

    private fun generateBladeWhoosh(): ShortArray {
        val durationMs = 160
        val count = (sampleRate * (durationMs / 1000.0)).toInt()
        val buffer = ShortArray(count)
        var phase = 0.0
        for (i in 0 until count) {
            val progress = i.toDouble() / count
            val freq = 300.0 + sin(progress * PI) * 500.0
            phase += 2.0 * PI * freq / sampleRate
            val envelope = sin(progress * PI)
            val noise = Random.nextDouble(-0.3, 0.3)
            val wave = (sin(phase) * 0.7 + noise) * envelope
            buffer[i] = (wave * 22000.0).toInt().coerceIn(-32767, 32767).toShort()
        }
        return buffer
    }

    private fun generatePitchSweep(freqStart: Double, freqEnd: Double, durationMs: Int): ShortArray {
        val count = (sampleRate * (durationMs / 1000.0)).toInt()
        val buffer = ShortArray(count)
        var phase = 0.0
        for (i in 0 until count) {
            val progress = i.toDouble() / count
            val freq = freqStart + (freqEnd - freqStart) * progress
            phase += 2.0 * PI * freq / sampleRate
            val envelope = sin(progress * PI)
            val wave = sin(phase) * envelope
            buffer[i] = (wave * 23000.0).toInt().coerceIn(-32767, 32767).toShort()
        }
        return buffer
    }

    private fun generateDashWhoosh(): ShortArray {
        val durationMs = 180
        val count = (sampleRate * (durationMs / 1000.0)).toInt()
        val buffer = ShortArray(count)
        var phase = 0.0
        for (i in 0 until count) {
            val progress = i.toDouble() / count
            val freq = 600.0 * (1.0 - progress) + 120.0
            phase += 2.0 * PI * freq / sampleRate
            val envelope = (1.0 - progress)
            val wave = (sin(phase) * 0.5 + Random.nextDouble(-0.5, 0.5)) * envelope
            buffer[i] = (wave * 25000.0).toInt().coerceIn(-32767, 32767).toShort()
        }
        return buffer
    }

    private fun generateWallRunHum(): ShortArray {
        val durationMs = 90
        val count = (sampleRate * (durationMs / 1000.0)).toInt()
        val buffer = ShortArray(count)
        var phase = 0.0
        for (i in 0 until count) {
            val progress = i.toDouble() / count
            phase += 2.0 * PI * 180.0 / sampleRate
            val wave = (sin(phase) + 0.3 * sin(phase * 3.0)) * (0.8 + 0.2 * Random.nextDouble(-1.0, 1.0))
            buffer[i] = (wave * 16000.0).toInt().coerceIn(-32767, 32767).toShort()
        }
        return buffer
    }

    private fun generateSlideScrape(): ShortArray {
        val durationMs = 120
        val count = (sampleRate * (durationMs / 1000.0)).toInt()
        val buffer = ShortArray(count)
        for (i in 0 until count) {
            val progress = i.toDouble() / count
            val noise = Random.nextDouble(-1.0, 1.0)
            val envelope = (1.0 - progress * 0.5)
            buffer[i] = (noise * envelope * 15000.0).toInt().coerceIn(-32767, 32767).toShort()
        }
        return buffer
    }

    private fun generateHitMarker(): ShortArray {
        val count = (sampleRate * 0.04).toInt()
        val buffer = ShortArray(count)
        var phase = 0.0
        for (i in 0 until count) {
            phase += 2.0 * PI * 1400.0 / sampleRate
            buffer[i] = (sin(phase) * 26000.0).toInt().toShort()
        }
        return buffer
    }

    private fun generateExplosion(): ShortArray {
        val durationMs = 380
        val count = (sampleRate * (durationMs / 1000.0)).toInt()
        val buffer = ShortArray(count)
        var phase = 0.0
        for (i in 0 until count) {
            val progress = i.toDouble() / count
            val freq = 90.0 * (1.0 - progress * 0.7)
            phase += 2.0 * PI * freq / sampleRate
            val bass = sin(phase)
            val noise = Random.nextDouble(-1.0, 1.0)
            val envelope = Math.exp(-progress * 3.5)
            val mixed = (bass * 0.4 + noise * 0.6) * envelope
            buffer[i] = (mixed * 28000.0).toInt().coerceIn(-32767, 32767).toShort()
        }
        return buffer
    }

    private fun generatePickupChime(): ShortArray {
        val durationMs = 200
        val count = (sampleRate * (durationMs / 1000.0)).toInt()
        val buffer = ShortArray(count)
        var phase1 = 0.0
        var phase2 = 0.0
        for (i in 0 until count) {
            val progress = i.toDouble() / count
            val freq = if (progress < 0.5) 523.25 else 783.99 // C5 -> G5 chime
            phase1 += 2.0 * PI * freq / sampleRate
            phase2 += 2.0 * PI * (freq * 2.0) / sampleRate
            val envelope = 1.0 - progress
            val wave = (sin(phase1) + 0.4 * sin(phase2)) * envelope
            buffer[i] = (wave * 22000.0).toInt().coerceIn(-32767, 32767).toShort()
        }
        return buffer
    }

    private fun generateSynthBeat(bassFreq: Double, isKick: Boolean, isSnare: Boolean, isHiHat: Boolean, durationMs: Int): ShortArray {
        val count = (sampleRate * (durationMs / 1000.0)).toInt()
        val buffer = ShortArray(count)
        var bassPhase = 0.0
        var kickPhase = 0.0

        for (i in 0 until count) {
            val progress = i.toDouble() / count
            // Synth Bass (saw-like)
            bassPhase += 2.0 * PI * bassFreq / sampleRate
            val bassWave = (sin(bassPhase) + 0.5 * sin(bassPhase * 2.0) + 0.25 * sin(bassPhase * 3.0)) * (1.0 - progress * 0.7)

            var drumWave = 0.0
            if (isKick) {
                val kickFreq = 150.0 * (1.0 - progress * 0.8) + 40.0
                kickPhase += 2.0 * PI * kickFreq / sampleRate
                drumWave += sin(kickPhase) * Math.exp(-progress * 5.0) * 1.4
            }
            if (isSnare && progress < 0.8) {
                drumWave += Random.nextDouble(-1.0, 1.0) * Math.exp(-progress * 4.0) * 0.9
            }
            if (isHiHat && progress < 0.3) {
                drumWave += Random.nextDouble(-1.0, 1.0) * (1.0 - progress / 0.3) * 0.4
            }

            val mix = (bassWave * 0.6 + drumWave * 0.6)
            buffer[i] = (mix * 18000.0).toInt().coerceIn(-32767, 32767).toShort()
        }
        return buffer
    }
}
