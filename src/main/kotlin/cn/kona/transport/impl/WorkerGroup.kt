package cn.kona.transport.impl

import cn.kona.clone
import cn.kona.getLogger
import cn.kona.transport.nio.NioEventLoopGroup
import java.nio.ByteBuffer
import java.nio.channels.SocketChannel
import java.nio.charset.Charset

internal class WorkerGroup(nThreads: Int = Runtime.getRuntime().availableProcessors()) : NioEventLoopGroup(nThreads) {

    private val log = getLogger()

    override fun readBytes(buffer: ByteBuffer, channel: SocketChannel) {
        buffer.clear()
        val read = channel.read(buffer)
        if (read > 0) {
            buffer.flip()
            val bytes = buffer.clone().array()
            log.info(bytes.toString(Charset.defaultCharset()))
        } else if (read == -1) {
            channel.close()
        }
    }
}