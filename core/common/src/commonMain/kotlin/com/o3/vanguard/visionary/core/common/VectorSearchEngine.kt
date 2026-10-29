package com.o3.vanguard.visionary.core.common

import kotlin.math.sin
import kotlin.math.sqrt

fun FloatArray.toByteArray(): ByteArray {
    val byteArray = ByteArray(size * 4)
    for (i in indices) {
        val bits = this[i].toRawBits()
        byteArray[i * 4] = (bits and 0xFF).toByte()
        byteArray[i * 4 + 1] = ((bits shr 8) and 0xFF).toByte()
        byteArray[i * 4 + 2] = ((bits shr 16) and 0xFF).toByte()
        byteArray[i * 4 + 3] = ((bits shr 24) and 0xFF).toByte()
    }
    return byteArray
}

fun ByteArray.toFloatArray(): FloatArray {
    val floatArray = FloatArray(size / 4)
    for (i in floatArray.indices) {
        val bits = (this[i * 4].toInt() and 0xFF) or
                ((this[i * 4 + 1].toInt() and 0xFF) shl 8) or
                ((this[i * 4 + 2].toInt() and 0xFF) shl 16) or
                ((this[i * 4 + 3].toInt() and 0xFF) shl 24)
        floatArray[i] = Float.fromBits(bits)
    }
    return floatArray
}

class VectorSearchEngine {

    fun cosineSimilarity(vector1: FloatArray, vector2: FloatArray): Float {
        if (vector1.size != vector2.size) return 0f

        var dotProduct = 0f
        var magnitude1 = 0f
        var magnitude2 = 0f

        for (i in vector1.indices) {
            dotProduct += vector1[i] * vector2[i]
            magnitude1 += vector1[i] * vector1[i]
            magnitude2 += vector2[i] * vector2[i]
        }

        magnitude1 = sqrt(magnitude1)
        magnitude2 = sqrt(magnitude2)

        return if (magnitude1 > 0 && magnitude2 > 0) {
            dotProduct / (magnitude1 * magnitude2)
        } else {
            0f
        }
    }

    fun generateQueryEmbedding(query: String): FloatArray {
        val words = query.trim().lowercase()
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }

        return generateEmbedding(words)
    }

    fun generateEmbedding(texts: List<String>): FloatArray {
        val embedding = FloatArray(128) { 0f }

        if (texts.isEmpty()) return embedding

        texts.forEachIndexed { index, text ->
            val cleanText = text.lowercase().trim()
            if (cleanText.isNotEmpty()) {
                val hash = cleanText.hashCode()
                for (i in 0 until 128) {
                    val value = (hash * (index + 1) * (i + 1)).toFloat()
                    embedding[i] += sin(value / 1000000f)
                }
            }
        }

        val magnitude = sqrt(embedding.sumOf { (it * it).toDouble() }).toFloat()
        if (magnitude > 0) {
            for (i in embedding.indices) {
                embedding[i] /= magnitude
            }
        }

        return embedding
    }
}
