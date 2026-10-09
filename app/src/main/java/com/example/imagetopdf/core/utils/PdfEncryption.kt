package com.example.imagetopdf.core.utils

import java.io.ByteArrayOutputStream
import java.security.MessageDigest

object PdfEncryption {

    private val PAD = byteArrayOf(
        0x28, 0xBF.toByte(), 0x4E, 0x5E, 0x4E, 0x75, 0x8A.toByte(), 0x41,
        0x64, 0x00, 0x4E, 0x56, 0xFF.toByte(), 0xFA.toByte(), 0x01, 0x08,
        0x2E, 0x2E, 0x00, 0xB6.toByte(), 0xD0.toByte(), 0x68, 0x3E.toByte(), 0x80.toByte(),
        0x2F, 0x0C, 0xA9.toByte(), 0xFE.toByte(), 0x64, 0x53, 0x69, 0x7A
    )

    fun encrypt(pdfBytes: ByteArray, userPassword: String): ByteArray {
        val paddedUser = padPassword(userPassword)
        val paddedOwner = padPassword(userPassword)

        val oValue = computeOwnerValue(paddedOwner, paddedUser)
        val fileId = extractFileId(pdfBytes)

        val key = computeEncryptionKey(paddedUser, oValue, fileId, 5)
        val uValue = computeUserValue(key, fileId)

        val encrypted = encryptStreams(pdfBytes, key, fileId)

        return buildEncryptedPdf(encrypted, oValue, uValue, fileId)
    }

    private fun padPassword(password: String): ByteArray {
        val bytes = password.toByteArray(Charsets.ISO_8859_1)
        val result = ByteArray(32)
        System.arraycopy(PAD, 0, result, 0, 32)
        val len = minOf(bytes.size, 32)
        System.arraycopy(bytes, 0, result, 0, len)
        return result
    }

    private fun computeOwnerValue(ownerPad: ByteArray, userPad: ByteArray): ByteArray {
        var hash = md5(ownerPad)
        repeat(50) { hash = md5(hash) }
        val key = hash.copyOf(5)

        var rc4Key = key
        repeat(20) { i ->
            val newKey = ByteArray(5)
            for (j in 0 until 5) newKey[j] = (rc4Key[j].toInt() xor i).toByte()
            rc4Key = newKey
        }

        val encrypted = rc4(userPad, rc4Key)
        var result = encrypted
        repeat(19) { i ->
            val newKey = ByteArray(5)
            for (j in 0 until 5) newKey[j] = (key[j].toInt() xor i).toByte()
            result = rc4(result, newKey)
        }
        return result
    }

    private fun computeEncryptionKey(userPad: ByteArray, oValue: ByteArray, fileId: ByteArray, keyLength: Int): ByteArray {
        val input = ByteArrayOutputStream()
        input.write(userPad)
        input.write(oValue)
        if (fileId.isNotEmpty()) input.write(fileId.copyOf(minOf(fileId.size, 16)))
        input.write(byteArrayOf(0xFF.toByte(), 0, 4))

        var hash = md5(input.toByteArray())
        repeat(50) { hash = md5(hash.copyOf(keyLength)) }

        return hash.copyOf(keyLength)
    }

    private fun computeUserValue(key: ByteArray, fileId: ByteArray): ByteArray {
        val result = ByteArrayOutputStream()
        result.write(PAD, 0, 32)
        if (fileId.isNotEmpty()) result.write(fileId.copyOf(minOf(fileId.size, 16)))
        return md5(result.toByteArray())
    }

    private fun extractFileId(pdfBytes: ByteArray): ByteArray {
        val text = String(pdfBytes, Charsets.ISO_8859_1)
        val idStart = text.indexOf("/ID [")
        if (idStart < 0) return byteArrayOf(0x12, 0x34, 0x56, 0x78)
        val hexStart = text.indexOf("<", idStart)
        val hexEnd = text.indexOf(">", hexStart)
        if (hexStart < 0 || hexEnd < 0) return byteArrayOf(0x12, 0x34, 0x56, 0x78)
        val hex = text.substring(hexStart + 1, hexEnd).trim()
        return try {
            hex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
        } catch (e: Exception) {
            byteArrayOf(0x12, 0x34, 0x56, 0x78)
        }
    }

    private fun encryptStreams(pdfBytes: ByteArray, key: ByteArray, fileId: ByteArray): ByteArray {
        val text = String(pdfBytes, Charsets.ISO_8859_1)
        val result = StringBuilder()
        var pos = 0

        while (pos < text.length) {
            val streamStart = text.indexOf("stream", pos)
            if (streamStart < 0) {
                result.append(text.substring(pos))
                break
            }

            result.append(text.substring(pos, streamStart + 6))

            var dataStart = streamStart + 6
            if (dataStart < text.length && text[dataStart] == '\r') dataStart++
            if (dataStart < text.length && text[dataStart] == '\n') dataStart++

            val endStream = text.indexOf("endstream", dataStart)
            if (endStream < 0) {
                result.append(text.substring(streamStart + 6))
                break
            }

            var dataEnd = endStream
            if (dataEnd > dataStart && text[dataEnd - 1] == '\n') dataEnd--
            if (dataEnd > dataStart && text[dataEnd - 1] == '\r') dataEnd--

            val streamData = text.substring(dataStart, dataEnd).toByteArray(Charsets.ISO_8859_1)
            val encrypted = rc4(streamData, key)

            result.append('\n')
            result.append(String(encrypted, Charsets.ISO_8859_1))
            result.append("\nendstream")

            pos = endStream + 9
        }

        return result.toString().toByteArray(Charsets.ISO_8859_1)
    }

    private fun buildEncryptedPdf(
        encryptedContent: ByteArray,
        oValue: ByteArray,
        uValue: ByteArray,
        fileId: ByteArray
    ): ByteArray {
        val text = String(encryptedContent, Charsets.ISO_8859_1)
        val result = StringBuilder(text)

        val encryptDict = buildString {
            append("<< /Filter /Standard /V 1 /R 2 /O (")
            append(escapePdfString(oValue))
            append(") /U (")
            append(escapePdfString(uValue))
            append(") /P -1 >>")
        }

        val objNum = findMaxObjectNumber(text) + 1
        val insertPos = text.indexOf("trailer")
        if (insertPos > 0) {
            result.insert(insertPos, "$objNum 0 obj\n$encryptDict\nendobj\n\n")
            val newText = result.toString()
            val updatedTrailer = newText.replace(
                "trailer",
                "trailer\n<< /Encrypt $objNum 0 R /Root 1 0 R >>",
                ignoreCase = false
            )
            return updatedTrailer.toByteArray(Charsets.ISO_8859_1)
        }

        return encryptedContent
    }

    private fun findMaxObjectNumber(text: String): Int {
        var max = 0
        val regex = Regex("""(\d+)\s+\d+\s+obj""")
        for (match in regex.findAll(text)) {
            val num = match.groupValues[1].toIntOrNull() ?: continue
            if (num > max) max = num
        }
        return max
    }

    private fun escapePdfString(bytes: ByteArray): String {
        return bytes.joinToString("") { b ->
            val c = b.toInt() and 0xFF
            when (c) {
                0x08 -> "\\b"
                0x09 -> "\\t"
                0x0A -> "\\n"
                0x0C -> "\\f"
                0x0D -> "\\r"
                0x28 -> "\\("
                0x29 -> "\\)"
                0x5C -> "\\\\"
                else -> if (c in 32..126) c.toChar().toString() else "\\${c.toString(8).padStart(3, '0')}"
            }
        }
    }

    private fun rc4(data: ByteArray, key: ByteArray): ByteArray {
        val s = IntArray(256)
        for (i in 0..255) s[i] = i

        var j = 0
        for (i in 0..255) {
            j = (j + s[i] + key[i % key.size].toInt()) and 0xFF
            val temp = s[i]
            s[i] = s[j]
            s[j] = temp
        }

        val result = ByteArray(data.size)
        var i = 0
        j = 0
        for (k in data.indices) {
            i = (i + 1) and 0xFF
            j = (j + s[i]) and 0xFF
            val temp = s[i]
            s[i] = s[j]
            s[j] = temp
            result[k] = (data[k].toInt() xor s[(s[i] + s[j]) and 0xFF]).toByte()
        }
        return result
    }

    private fun md5(input: ByteArray): ByteArray {
        return MessageDigest.getInstance("MD5").digest(input)
    }
}
