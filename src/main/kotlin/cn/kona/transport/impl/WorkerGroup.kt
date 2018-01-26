package cn.kona.transport.impl

import cn.kona.getLogger
import cn.kona.systemCharset
import cn.kona.transport.ChannelMeta
import cn.kona.transport.nio.NioEventLoopGroup
import java.nio.ByteBuffer
import java.nio.channels.SelectionKey
import java.nio.channels.SocketChannel

internal class WorkerGroup(nThreads: Int = Runtime.getRuntime().availableProcessors()) : NioEventLoopGroup(nThreads) {

    private val log = getLogger()

    override fun readBytes(buffer: ByteBuffer, key: SelectionKey) {
        (key.channel() as? SocketChannel)?.let { ch ->
            if (ch.isOpen.not()) {
                log.warn("Channel closed.")
                return
            }
            val attach = key.attachment()
            if (attach !is ChannelMeta) {
                log.warn("This is not our socket channel.")
                return
            }
            buffer.clear()
            val read = ch.read(buffer)
            if (read > 0) {
                buffer.flip()
                while (buffer.hasRemaining()) {
                    attach.pipeline?.pump(buffer.get())
                }
            } else if (read == -1) {
                key.cancel()
            }
        }
    }

    override fun writeBytes(key: SelectionKey) {
        (key.channel() as? SocketChannel)?.let { ch ->
            if (ch.isOpen.not()) {
                log.warn("Channel closed.")
                return
            }
            val attach = key.attachment()
            if (attach is ChannelMeta) {
                attach.writeBuffer
            } else {
                null
            }?.let { bf ->
                ch.write(bf)
            }
        }
        key.interestOps(SelectionKey.OP_READ)
    }
}