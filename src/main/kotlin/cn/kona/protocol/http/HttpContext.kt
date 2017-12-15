package cn.kona.protocol.http

import java.nio.ByteBuffer

class HttpContext {
    private val headersLines = mutableListOf<ByteBuffer>()
    private val bodyLines = mutableListOf<ByteBuffer>()
    private var headersFinished = false

    internal fun readInLine(buffer: ByteBuffer) {

        if (!headersFinished) {
            headersLines.add(buffer)
        } else {
            bodyLines.add(buffer)
        }
    }
}