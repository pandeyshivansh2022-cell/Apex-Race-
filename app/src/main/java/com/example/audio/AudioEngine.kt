package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.sin
import kotlin.random.Random

/**
 * Procedural low-latency audio synthesizer for Apex Racer.
 * Generates dynamic engine sounds, nitro effects, tire drifts, crashes, and chimes
 * without requiring external sound asset files.
 */
class AudioEngine {

  private val sampleRate = 22050
  private var isMuted = false
  private val scope = CoroutineScope(Dispatchers.Default)

  // Pre-generated sound buffers
  private var coinBuffer: ShortArray? = null
  private var countdownLowBuffer: ShortArray? = null
  private var countdownHighBuffer: ShortArray? = null
  private var crashBuffer: ShortArray? = null
  private var nitroBuffer: ShortArray? = null
  private var driftBuffer: ShortArray? = null
  private var powerupBuffer: ShortArray? = null
  private var nearMissBuffer: ShortArray? = null

  // Continuous Engine Synthesizer
  private var engineTrack: AudioTrack? = null
  private var engineJob: Job? = null
  @Volatile private var currentEngineFreq = 80f
  @Volatile private var isEngineRunning = false

  init {
    try {
      scope.launch {
        initBuffers()
      }
    } catch (e: Exception) {
      Log.w("AudioEngine", "Audio init error: ${e.message}")
    }
  }

  fun setMuted(muted: Boolean) {
    isMuted = muted
    if (muted) {
      stopEngine()
    }
  }

  private fun initBuffers() {
    coinBuffer = generateCoinSound()
    countdownLowBuffer = generateBeep(440f, 0.15f)
    countdownHighBuffer = generateBeep(880f, 0.35f)
    crashBuffer = generateCrashSound()
    nitroBuffer = generateNitroSound()
    driftBuffer = generateDriftSound()
    powerupBuffer = generatePowerupSound()
    nearMissBuffer = generateNearMissSound()
  }

  private fun generateBeep(frequency: Float, durationSec: Float): ShortArray {
    val totalSamples = (sampleRate * durationSec).toInt()
    val buffer = ShortArray(totalSamples)
    for (i in 0 until totalSamples) {
      val t = i.toDouble() / sampleRate
      val envelope = (1.0 - (i.toDouble() / totalSamples))
      val sample = sin(2.0 * Math.PI * frequency * t) * envelope * Short.MAX_VALUE * 0.6
      buffer[i] = sample.toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
    }
    return buffer
  }

  private fun generateCoinSound(): ShortArray {
    val duration = 0.22f
    val totalSamples = (sampleRate * duration).toInt()
    val buffer = ShortArray(totalSamples)
    val half = totalSamples / 2
    for (i in 0 until totalSamples) {
      val freq = if (i < half) 987.77f else 1318.51f // B5 -> E6
      val t = i.toDouble() / sampleRate
      val env = (1.0 - (i.toDouble() / totalSamples))
      val sample = sin(2.0 * Math.PI * freq * t) * env * Short.MAX_VALUE * 0.5
      buffer[i] = sample.toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
    }
    return buffer
  }

  private fun generateCrashSound(): ShortArray {
    val duration = 0.5f
    val totalSamples = (sampleRate * duration).toInt()
    val buffer = ShortArray(totalSamples)
    val random = Random(42)
    for (i in 0 until totalSamples) {
      val progress = i.toDouble() / totalSamples
      val noise = (random.nextFloat() * 2.0 - 1.0)
      val lowThump = sin(2.0 * Math.PI * 65.0 * (1.0 - progress) * (i.toDouble() / sampleRate))
      val env = (1.0 - progress) * (1.0 - progress)
      val sample = (noise * 0.7 + lowThump * 0.7) * env * Short.MAX_VALUE * 0.75
      buffer[i] = sample.toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
    }
    return buffer
  }

  private fun generateNitroSound(): ShortArray {
    val duration = 0.65f
    val totalSamples = (sampleRate * duration).toInt()
    val buffer = ShortArray(totalSamples)
    val random = Random(123)
    for (i in 0 until totalSamples) {
      val progress = i.toDouble() / totalSamples
      val noise = (random.nextFloat() * 2.0 - 1.0)
      val sweepFreq = 300.0 + progress * 500.0
      val tone = sin(2.0 * Math.PI * sweepFreq * (i.toDouble() / sampleRate))
      val env = sin(Math.PI * progress) // bell curve
      val sample = (noise * 0.6 + tone * 0.4) * env * Short.MAX_VALUE * 0.65
      buffer[i] = sample.toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
    }
    return buffer
  }

  private fun generateDriftSound(): ShortArray {
    val duration = 0.35f
    val totalSamples = (sampleRate * duration).toInt()
    val buffer = ShortArray(totalSamples)
    val random = Random(55)
    for (i in 0 until totalSamples) {
      val progress = i.toDouble() / totalSamples
      val noise = (random.nextFloat() * 2.0 - 1.0)
      val screech = sin(2.0 * Math.PI * 1800.0 * (i.toDouble() / sampleRate))
      val env = (1.0 - progress) * 0.5
      val sample = (noise * 0.4 + screech * 0.6) * env * Short.MAX_VALUE * 0.4
      buffer[i] = sample.toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
    }
    return buffer
  }

  private fun generatePowerupSound(): ShortArray {
    val duration = 0.3f
    val totalSamples = (sampleRate * duration).toInt()
    val buffer = ShortArray(totalSamples)
    val notes = floatArrayOf(523.25f, 659.25f, 783.99f, 1046.50f) // C5, E5, G5, C6
    val noteSamples = totalSamples / notes.size
    for (i in 0 until totalSamples) {
      val noteIndex = (i / noteSamples).coerceIn(0, notes.size - 1)
      val freq = notes[noteIndex]
      val t = i.toDouble() / sampleRate
      val env = 1.0 - ((i % noteSamples).toDouble() / noteSamples)
      val sample = sin(2.0 * Math.PI * freq * t) * env * Short.MAX_VALUE * 0.5
      buffer[i] = sample.toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
    }
    return buffer
  }

  private fun generateNearMissSound(): ShortArray {
    val duration = 0.25f
    val totalSamples = (sampleRate * duration).toInt()
    val buffer = ShortArray(totalSamples)
    for (i in 0 until totalSamples) {
      val progress = i.toDouble() / totalSamples
      // Doppler sweep
      val freq = 800.0 - progress * 400.0
      val t = i.toDouble() / sampleRate
      val env = sin(Math.PI * progress)
      val sample = sin(2.0 * Math.PI * freq * t) * env * Short.MAX_VALUE * 0.5
      buffer[i] = sample.toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
    }
    return buffer
  }

  private fun playBuffer(buffer: ShortArray?) {
    if (isMuted || buffer == null) return
    scope.launch(Dispatchers.Default) {
      try {
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
          .setBufferSizeInBytes(buffer.size * 2)
          .setTransferMode(AudioTrack.MODE_STATIC)
          .build()

        track.write(buffer, 0, buffer.size)
        track.play()
        // Wait and release
        val playDuration = (buffer.size.toFloat() / sampleRate * 1000).toLong() + 50
        kotlinx.coroutines.delay(playDuration)
        track.stop()
        track.release()
      } catch (e: Exception) {
        // Safe catch
      }
    }
  }

  fun playCoin() = playBuffer(coinBuffer)
  fun playCountdownLow() = playBuffer(countdownLowBuffer)
  fun playCountdownHigh() = playBuffer(countdownHighBuffer)
  fun playCrash() = playBuffer(crashBuffer)
  fun playNitro() = playBuffer(nitroBuffer)
  fun playDrift() = playBuffer(driftBuffer)
  fun playPowerup() = playBuffer(powerupBuffer)
  fun playNearMiss() = playBuffer(nearMissBuffer)

  /**
   * Starts procedural engine pitch drone. Dynamically changes pitch based on vehicle RPM.
   */
  fun startEngine() {
    if (isMuted || isEngineRunning) return
    isEngineRunning = true

    engineJob = scope.launch(Dispatchers.Default) {
      val minBufferSize = AudioTrack.getMinBufferSize(
        sampleRate,
        AudioFormat.CHANNEL_OUT_MONO,
        AudioFormat.ENCODING_PCM_16BIT
      ).coerceAtLeast(1024)

      try {
        engineTrack = AudioTrack.Builder()
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
          .setBufferSizeInBytes(minBufferSize * 2)
          .setTransferMode(AudioTrack.MODE_STREAM)
          .build()

        engineTrack?.play()
        val chunk = ShortArray(512)
        var phase = 0.0

        while (isActive && isEngineRunning && !isMuted) {
          val freq = currentEngineFreq
          val phaseInc = 2.0 * Math.PI * freq / sampleRate
          for (i in chunk.indices) {
            phase += phaseInc
            if (phase > 2.0 * Math.PI) phase -= 2.0 * Math.PI
            // Dual harmonic sawtooth + low sine for engine growl
            val saw = (phase / Math.PI - 1.0)
            val sub = sin(phase * 0.5)
            val sample = (saw * 0.6 + sub * 0.4) * Short.MAX_VALUE * 0.22
            chunk[i] = sample.toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
          }
          engineTrack?.write(chunk, 0, chunk.size)
        }
      } catch (e: Exception) {
        // Safe catch
      } finally {
        try {
          engineTrack?.stop()
          engineTrack?.release()
        } catch (ignored: Exception) {}
        engineTrack = null
      }
    }
  }

  fun updateEngineRPM(rpmNormalized: Float, isAccelerating: Boolean) {
    // rpmNormalized between 0.0 (idle ~700 RPM) and 1.0 (redline ~8000 RPM)
    val baseFreq = 55f
    val maxFreq = 220f
    val boost = if (isAccelerating) 18f else 0f
    currentEngineFreq = (baseFreq + rpmNormalized * (maxFreq - baseFreq) + boost).coerceIn(40f, 300f)
  }

  fun stopEngine() {
    isEngineRunning = false
    engineJob?.cancel()
    engineJob = null
  }

  fun release() {
    stopEngine()
  }
}
