package cn.kona.transport.impl

import cn.kona.getLogger
import cn.kona.systemCharset
import cn.kona.transport.ChannelMeta
import cn.kona.transport.nio.NioEventLoopGroup
import java.nio.ByteBuffer
import java.nio.channels.SocketChannel

internal class WorkerGroup(nThreads: Int = Runtime.getRuntime().availableProcessors()) : NioEventLoopGroup(nThreads) {

    private val log = getLogger()

    override fun readBytes(buffer: ByteBuffer, channel: SocketChannel, attach: Any?) {
        if (attach !is ChannelMeta) {
            log.warn("This is not our socket channel.")
            return
        }
        buffer.clear()
        val read = channel.read(buffer)
        if (read > 0) {
            buffer.flip()
            while (buffer.hasRemaining()) {
                attach.pipeline.pump(buffer.get())
            }
        } else if (read == -1) {
            channel.close()
        }
    }
}