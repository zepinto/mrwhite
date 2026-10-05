package com.zepinto.mrwhite

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/**
 * Two short sounds synthesised at start-up, so the app needs no audio files:
 * a bright chime for the first look at a card, a harsh buzz for looking again.
 */
class Sfx {
    enum class Kind { REVEAL, WARNING }

    private val tracks: Map<Kind, AudioTrack?> = mapOf(
        Kind.REVEAL to build(chime()),
        Kind.WARNING to build(buzz()),
    )

    fun play(kind: Kind) {
        val track = tracks[kind] ?: return
        try {
            track.stop()
            track.reloadStaticData()
            track.play()
        } catch (_: IllegalStateException) {
            // A sound that fails to play must never break the game.
        }
    }

    fun release() = tracks.values.forEach { it?.release() }

    private fun build(samples: ShortArray): AudioTrack? = try {
        AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(RATE)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(samples.size * 2)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()
            .also { it.write(samples, 0, samples.size) }
    } catch (_: Exception) {
        null
    }

    private companion object {
        const val RATE = 22_050

        /** Rising D6 then G6, each with a quick attack and a soft decay. */
        fun chime(): ShortArray = tone(1175.0, 110, 0.7) + tone(1568.0, 240, 0.7)

        /** Two low square-wave pulses with a gap, clearly unpleasant. */
        fun buzz(): ShortArray = tone(150.0, 170, 0.6, square = true, decay = false) +
            ShortArray(RATE * 60 / 1000) +
            tone(120.0, 230, 0.6, square = true, decay = false)

        fun tone(freq: Double, ms: Int, volume: Double, square: Boolean = false, decay: Boolean = true): ShortArray {
            val n = RATE * ms / 1000
            val attack = RATE / 200 // 5 ms, avoids clicks
            return ShortArray(n) { i ->
                val phase = sin(2 * PI * freq * i / RATE)
                val wave = if (square) (if (phase >= 0) 1.0 else -1.0) else phase
                val env = (if (decay) exp(-4.0 * i / n) else 1.0) * minOf(1.0, i / attack.toDouble()) * minOf(1.0, (n - i) / attack.toDouble())
                (wave * env * volume * Short.MAX_VALUE).toInt().toShort()
            }
        }
    }
}
