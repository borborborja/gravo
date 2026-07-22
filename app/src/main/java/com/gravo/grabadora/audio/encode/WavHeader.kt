package com.gravo.grabadora.audio.encode

import java.io.RandomAccessFile

/**
 * Cabecera RIFF/WAVE. PCM entero (formato 1) para 16/24 bits y float IEEE (formato 3,
 * con chunk "fact") para 32 bits. JVM puro, testeable.
 */
object WavHeader {
    const val FORMAT_PCM = 1
    const val FORMAT_FLOAT = 3

    fun headerSize(audioFormat: Int): Int = if (audioFormat == FORMAT_FLOAT) 58 else 44

    /**
     * Genera la cabecera con [dataSize] bytes de audio. Para escribirla antes de conocer el
     * tamaño usa dataSize=0 y parchea después con [patchSizes].
     */
    fun build(audioFormat: Int, sampleRate: Int, channels: Int, bitsPerSample: Int, dataSize: Int): ByteArray {
        val blockAlign = channels * bitsPerSample / 8
        val byteRate = sampleRate * blockAlign
        val isFloat = audioFormat == FORMAT_FLOAT
        val header = ByteArray(headerSize(audioFormat))
        var p = 0
        fun str(s: String) { for (c in s) header[p++] = c.code.toByte() }
        fun u32(v: Int) { repeat(4) { header[p++] = ((v shr (8 * it)) and 0xFF).toByte() } }
        fun u16(v: Int) { repeat(2) { header[p++] = ((v shr (8 * it)) and 0xFF).toByte() } }

        str("RIFF"); u32(header.size - 8 + dataSize); str("WAVE")
        str("fmt "); u32(if (isFloat) 18 else 16)
        u16(audioFormat); u16(channels); u32(sampleRate); u32(byteRate); u16(blockAlign); u16(bitsPerSample)
        if (isFloat) {
            u16(0) // cbSize
            str("fact"); u32(4); u32(dataSize / blockAlign)
        }
        str("data"); u32(dataSize)
        return header
    }

    /** Parchea RIFF size, fact (si float) y data size una vez conocido el tamaño final. */
    fun patchSizes(file: RandomAccessFile, audioFormat: Int, channels: Int, bitsPerSample: Int, dataSize: Int) {
        val isFloat = audioFormat == FORMAT_FLOAT
        val headerLen = headerSize(audioFormat)
        fun writeU32(pos: Long, v: Int) {
            file.seek(pos)
            file.write(byteArrayOf(
                (v and 0xFF).toByte(), ((v shr 8) and 0xFF).toByte(),
                ((v shr 16) and 0xFF).toByte(), ((v shr 24) and 0xFF).toByte(),
            ))
        }
        writeU32(4, headerLen - 8 + dataSize)
        if (isFloat) {
            val blockAlign = channels * bitsPerSample / 8
            writeU32(46, dataSize / blockAlign) // valor del chunk fact
        }
        writeU32((headerLen - 4).toLong(), dataSize)
    }
}
