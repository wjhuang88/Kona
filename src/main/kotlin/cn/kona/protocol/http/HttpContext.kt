package cn.kona.protocol.http

import cn.kona.MultiValuesMap
import cn.kona.systemCharset
import java.nio.ByteBuffer
import java.nio.CharBuffer

class HttpContext {

    // MARK: meta data

    private lateinit var method: String
    private var path: String = "/"
    private lateinit var protocol: String
    internal var contextLength = 0

    // MARK: data containers

    private val headersLines = MultiValuesMap<String, String>()
    private val bodyLines = mutableListOf<ByteBuffer>()

    // MARK: flags

    private var valid = false
    private var started = false
    internal var ended = false
    internal var headersFinished = false
    private var bodyRead = 0

    // MARK: main functions

    internal fun inputLine(buffer: ByteBuffer) {

        // read meta data when a request first line come.
        if (!started) {
            started = true
            if (readMeta(buffer)) valid = true
            return
        }

        // if not a valid request, do nothing.
        if (!valid) {
            return
        }

        // when first empty line, end header analysis
        if (isDelimiter(buffer)) {
            headersFinished = true
            contextLength = readFirstHeader("content-length")?.toInt() ?: 0
            if (contextLength <= 0) {
                ended = true
            }
            return
        }

        if (!headersFinished) { // analyse header
            val pair = systemCharset.decode(buffer).toPair(':')
            val headerKey = pair.first.toString()
            val headerValue = pair.second.toString()
            headersLines.putValue(headerKey.toLowerCase(), headerValue)
        } else { // save body
            if (contextLength <= 0) {
                ended = true
                return
            }

            if (bodyRead < contextLength) {
                bodyLines.add(buffer)
                bodyRead += buffer.capacity()
                if (bodyRead >= contextLength) {
                    ended = true
                }
            }
        }
    }

    internal fun readHeader(key: String): List<String> {
        return headersLines[key.toLowerCase()].orEmpty()
    }

    internal fun readFirstHeader(key: String): String? {
        return headersLines.getFirst(key.toLowerCase())
    }

    // MARK: tools

    private fun readMeta(buffer: ByteBuffer): Boolean {
        val startPos: Int

        // handle request method
        when {
            startWithGet(buffer) -> {
                method = "GET"
                startPos = 4
            }
            startWithPost(buffer) -> {
                method = "POST"
                startPos = 5
            }
            startWithPut(buffer) -> {
                method = "PUT"
                startPos = 4
            }
            startWithDelete(buffer) -> {
                method = "DELETE"
                startPos = 7
            }
            startWithPatch(buffer) -> {
                method = "PATCH"
                startPos = 6
            }
            startWithHead(buffer) -> {
                method = "HEAD"
                startPos = 5
            }
            startWithOptions(buffer) -> {
                method = "OPTIONS"
                startPos = 8
            }
            else -> {
                return false // TODO: support custom method.
            }
        }

        // handle path and protocol info
        buffer.limit(buffer.capacity())
        buffer.position(startPos)
        val pathBuffer = StringBuilder()
        val protocolBuffer = StringBuilder()
        var pathEnded = false
        while (buffer.hasRemaining()) {
            val b = buffer.get()
            if (b == '\r'.toByte()) break

            when {
                pathEnded -> protocolBuffer.append(b.toChar())
                b != ' '.toByte() -> pathBuffer.append(b.toChar())
                else -> pathEnded = true
            }
        }

        path = pathBuffer.toString()
        protocol = protocolBuffer.toString()

        return true
    }

    private fun isDelimiter(buffer: ByteBuffer): Boolean {
        return buffer.limit() != 0 && buffer[0] == '\r'.toByte()
    }

    private fun startWithOptions(buffer: ByteBuffer): Boolean {
        return buffer[0] == 'O'.toByte()
                && buffer[1] == 'P'.toByte()
                && buffer[2] == 'T'.toByte()
                && buffer[3] == 'I'.toByte()
                && buffer[4] == 'O'.toByte()
                && buffer[5] == 'N'.toByte()
                && buffer[6] == 'S'.toByte()
    }

    private fun startWithPost(buffer: ByteBuffer): Boolean {
        return buffer[0] == 'P'.toByte()
                && buffer[1] == 'O'.toByte()
                && buffer[2] == 'S'.toByte()
                && buffer[3] == 'T'.toByte()
    }

    private fun startWithHead(buffer: ByteBuffer): Boolean {
        return buffer[0] == 'H'.toByte()
                && buffer[1] == 'E'.toByte()
                && buffer[2] == 'A'.toByte()
                && buffer[3] == 'D'.toByte()
    }

    private fun startWithGet(buffer: ByteBuffer): Boolean {
        return buffer[0] == 'G'.toByte()
                && buffer[1] == 'E'.toByte()
                && buffer[2] == 'T'.toByte()
    }

    private fun startWithPut(buffer: ByteBuffer): Boolean {
        return buffer[0] == 'P'.toByte()
                && buffer[1] == 'U'.toByte()
                && buffer[2] == 'T'.toByte()
    }

    private fun startWithDelete(buffer: ByteBuffer): Boolean {
        return buffer[0] == 'D'.toByte()
                && buffer[1] == 'E'.toByte()
                && buffer[2] == 'L'.toByte()
                && buffer[3] == 'E'.toByte()
                && buffer[4] == 'T'.toByte()
                && buffer[5] == 'E'.toByte()
    }

    private fun startWithPatch(buffer: ByteBuffer): Boolean {
        return buffer[0] == 'P'.toByte()
                && buffer[1] == 'A'.toByte()
                && buffer[2] == 'T'.toByte()
                && buffer[3] == 'C'.toByte()
                && buffer[4] == 'H'.toByte()
    }

    private fun CharBuffer.toPair(on: Char): Pair<CharSequence, CharSequence> {

        val key = StringBuilder()
        val value = StringBuilder()

        var keyFinished = false

        this.iterator().let {
            while (it.hasNext()) {
                val thisChar = it.next()
                if (thisChar == on && !keyFinished) {
                    keyFinished = true
                } else {
                    if (keyFinished)
                        value.append(thisChar)
                    else
                        key.append(thisChar)
                }
            }
        }

        return key.trim() to value.trim()
    }

    // MARK: test functions

    internal fun testGet() {
        if (ended) {
            println("method: $method")
            println("path: $path")
            println("protocol: $protocol")
            headersLines.forEach { println(it) }
            bodyLines.forEach { println("body: ${systemCharset.decode(it)}") }
            ended = false
        }
    }
}