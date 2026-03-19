package com.ovzenproject.voicebridgeclient

import kotlin.math.PI
import kotlin.math.sin

object VoiceEffects {
    fun applyEffect(data: FloatArray, effect: String): FloatArray {
        return when (effect) {
            "pitch_up" -> pitchShift(data, 1.2f)
            "pitch_down" -> pitchShift(data, 0.8f)
            "echo" -> echo(data, 0.3f, 0.5f)
            "robot" -> robot(data)
            "chorus" -> chorus(data)
            else -> data
        }
    }

    private fun pitchShift(data: FloatArray, factor: Float): FloatArray {
        val newSize = (data.size / factor).toInt()
        val out = FloatArray(newSize)
        for (i in 0 until newSize) {
            val srcIndex = i * factor
            val i0 = srcIndex.toInt()
            val i1 = (i0 + 1).coerceAtMost(data.size - 1)
            val frac = srcIndex - i0
            out[i] = (data[i0] * (1 - frac) + data[i1] * frac)
        }
        return out
    }

    private fun echo(data: FloatArray, delaySec: Float, decay: Float): FloatArray {
        val sampleRate = 44100
        val delaySamples = (delaySec * sampleRate).toInt()
        val out = data.copyOf()
        for (i in delaySamples until data.size) {
            out[i] += data[i - delaySamples] * decay
            out[i] = out[i].coerceIn(-1f, 1f)
        }
        return out
    }

    private fun robot(data: FloatArray): FloatArray {
        val out = FloatArray(data.size)
        for (i in data.indices) {
            var sample = data[i]
            sample = (sample * 2.0).toFloat()
            sample = (sample * sample).toFloat()
            out[i] = sample.coerceIn(-1f, 1f)
        }
        return out
    }

    private fun chorus(data: FloatArray): FloatArray {
        val out = data.copyOf()
        val mod = 0.005f
        for (i in 1 until data.size) {
            val delayedIndex = (i * (1 - mod)).toInt().coerceIn(0, data.size - 1)
            out[i] = (data[i] + data[delayedIndex]) * 0.7f
        }
        return out
    }
}