package com.example.audio

import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import java.util.concurrent.Executors

object SoundManager {
    private val executor = Executors.newSingleThreadExecutor()
    var isMuted = false

    // Pre-generate PCM bytes to enable instant loading on tapping
    private val coinSoundBytes: ShortArray by lazy {
        generateCoinSound()
    }

    private fun generateCoinSound(): ShortArray {
        val sampleRate = 44100
        val duration = 0.15 // 150 ms
        val numSamples = (duration * sampleRate).toInt()
        val samples = ShortArray(numSamples)

        // Classic retro coin chime is composed of 2 key harmonic frequencies (B5 = ~987.77 Hz and E6 = ~1318.51 Hz)
        val freq1 = 987.77
        val freq2 = 1318.51

        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            // Sharp exponential decay
            val decay = Math.pow(0.005, t / duration)
            
            // Blended sine wave tone
            val wave = 0.5 * Math.sin(2 * Math.PI * freq1 * t) + 0.5 * Math.sin(2 * Math.PI * freq2 * t)
            
            samples[i] = (wave * decay * Short.MAX_VALUE * 0.7).toInt().toShort()
        }
        return samples
    }

    fun playCoinSound() {
        if (isMuted) return
        executor.execute {
            try {
                val size = coinSoundBytes.size
                val audioTrack = AudioTrack(
                    AudioManager.STREAM_MUSIC,
                    44100,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    size * 2,
                    AudioTrack.MODE_STATIC
                )
                
                // Write audio chunks to buffer
                audioTrack.write(coinSoundBytes, 0, size)
                audioTrack.play()
                
                // Give it sufficient time to finish playback, then discard
                Thread.sleep(170)
                audioTrack.stop()
                audioTrack.release()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
