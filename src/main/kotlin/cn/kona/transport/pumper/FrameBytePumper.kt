package cn.kona.transport.pumper

import java.nio.ByteBuffer
import kotlin.math.min

class FrameBytePumper(private val startByte: Byte,
                      private val endByte: Byte,
                      private val noStart: Boolean) : BytePumper() {

    private var frameStart = false

    private val bytePool = ByteGroup()

    override fun push(b: Byte) {
        if (!frameStart && (noStart || b == startByte)) {
            frameStart = true
        }
        if (!frameStart || b == startByte) {
            return
        }
        if (b == endByte) {
            val buffer = bytePool.flush()
            pipeline.startup(buffer)
            frameStart = false
        } else {
            bytePool.put(b)
        }
    }

    override fun flush(len: Int): ByteBuffer {
        if (len <= 0) return ByteBuffer.allocate(0)

        val buffer = bytePool.flush()
        buffer.position(0)
        buffer.limit(min(len, buffer.capacity()))
        frameStart = false
        return buffer
    }
}
