package com.aaditya.melyvio.playback.audio

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import timber.log.Timber
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sign
import kotlin.math.tanh

@UnstableApi
class VolumeNormalizationAudioProcessor : BaseAudioProcessor() {
    private var encoding = C.ENCODING_INVALID
    private var bytesPerSample = 0

    @Volatile
    var enabled = false
        set(value) {
            if (field != value) {
                field = value
                Timber.tag(TAG).d("Normalization processor enabled: $value")
            }
        }

    private data class GainState(val targetGainMb: Int, val linearGain: Double)

    @Volatile
    private var currentGain = GainState(0, 1.0)

    /** Extra loudness chosen in the Audio booster. The percentage is squared: 100% = x4, 50% = x2.25, 25% = x1.56. */
    @Volatile
    private var boostLinear = 1.0

    fun setBoostPercent(percent: Int) {
        val k = 1.0 + percent.coerceIn(0, 100) / 100.0
        boostLinear = k * k
    }

    /** Keeps a boosted signal from harsh digital clipping: gentle curve near full scale. */
    private fun limit(value: Double): Double {
        val a = abs(value)
        return if (a <= 0.7) value else sign(value) * (0.7 + 0.3 * tanh((a - 0.7) / 0.3))
    }

    @Synchronized
    fun setTargetGain(gainMb: Int) {
        if (currentGain.targetGainMb != gainMb) {
            val linearGain = 10.0.pow(gainMb / 2000.0)
            currentGain = GainState(gainMb, linearGain)
            Timber.tag(TAG).d("Target gain set to $gainMb mB (Linear multiplier: $linearGain)")
        }
    }

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        encoding = inputAudioFormat.encoding
        bytesPerSample =
            when (encoding) {
                C.ENCODING_PCM_16BIT -> 2
                C.ENCODING_PCM_24BIT -> 3
                C.ENCODING_PCM_32BIT, C.ENCODING_PCM_FLOAT -> 4
                else -> throw AudioProcessor.UnhandledAudioFormatException(inputAudioFormat)
            }

        Timber.tag(TAG).d(
            "Configured: sampleRate=${inputAudioFormat.sampleRate}, channels=${inputAudioFormat.channelCount}, encoding=$encoding",
        )
        return inputAudioFormat
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        if (!inputBuffer.hasRemaining()) return

        val gain = currentGain
        val boost = boostLinear
        val normalization = if (enabled && gain.targetGainMb != 0) gain.linearGain else 1.0
        val linear = normalization * boost
        val applyGain = linear != 1.0
        val soft = boost > 1.0
        val sampleCount = inputBuffer.remaining() / bytesPerSample
        val output = replaceOutputBuffer(sampleCount * bytesPerSample)

        inputBuffer.order(ByteOrder.LITTLE_ENDIAN)
        output.order(ByteOrder.LITTLE_ENDIAN)

        when (encoding) {
            C.ENCODING_PCM_16BIT ->
                repeat(sampleCount) {
                    val sample = inputBuffer.getShort()
                    output.putShort(
                        if (applyGain) {
                            if (soft) {
                                (limit(sample / 32768.0 * linear) * 32767.0).toInt().toShort()
                            } else {
                                (sample * linear).coerceIn(-32768.0, 32767.0).toInt().toShort()
                            }
                        } else {
                            sample
                        },
                    )
                }

            C.ENCODING_PCM_24BIT ->
                repeat(sampleCount) {
                    val b0 = inputBuffer.get().toInt() and 0xFF
                    val b1 = inputBuffer.get().toInt() and 0xFF
                    val b2 = inputBuffer.get().toInt()
                    val sample = (b2 shl 16) or (b1 shl 8) or b0
                    val processed =
                        if (applyGain) {
                            if (soft) {
                                (limit(sample / 8388608.0 * linear) * 8388607.0).toInt()
                            } else {
                                (sample * linear).coerceIn(-8388608.0, 8388607.0).toInt()
                            }
                        } else {
                            sample
                        }
                    output.put((processed and 0xFF).toByte())
                    output.put(((processed shr 8) and 0xFF).toByte())
                    output.put(((processed shr 16) and 0xFF).toByte())
                }

            C.ENCODING_PCM_32BIT ->
                repeat(sampleCount) {
                    val sample = inputBuffer.getInt()
                    output.putInt(
                        if (applyGain) {
                            if (soft) {
                                (limit(sample / 2147483648.0 * linear) * 2147483647.0).toLong().toInt()
                            } else {
                                (sample * linear).coerceIn(-2147483648.0, 2147483647.0).toLong().toInt()
                            }
                        } else {
                            sample
                        },
                    )
                }

            C.ENCODING_PCM_FLOAT ->
                repeat(sampleCount) {
                    val sample = inputBuffer.getFloat()
                    output.putFloat(
                        if (applyGain) {
                            if (soft) {
                                limit(sample * linear).toFloat()
                            } else {
                                (sample * linear.toFloat()).coerceIn(-1.0f, 1.0f)
                            }
                        } else {
                            sample
                        },
                    )
                }
        }

        output.flip()
    }

    override fun onReset() {
        encoding = C.ENCODING_INVALID
        bytesPerSample = 0
    }

    private companion object {
        const val TAG = "VolumeNormalizationProcessor"
    }
}

