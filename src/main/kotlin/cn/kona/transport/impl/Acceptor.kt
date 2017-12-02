package cn.kona.transport.impl

import cn.kona.getLogger
import cn.kona.transport.ChannelMeta
import cn.kona.transport.Pipeline
import cn.kona.transport.nio.NioEventLoop
import java.nio.channels.SelectionKey
import java.nio.channels.ServerSocketChannel
import java.util.*

internal class Acceptor
    @JvmOverloads
    constructor(private val pipeline: Pipeline, private val workerGroup: WorkerGroup = WorkerGroup()) : NioEventLoop() {

    private val log = getLogger()

    override fun loopAction(key: SelectionKey) {
        if (!isRunning()) {
            key.channel().close()
            key.cancel()
        } else if (key.isAcceptable) {
            val serverChannel = key.channel() as? ServerSocketChannel
            val socketChannel = serverChannel?.accept()
            socketChannel?.configureBlocking(false)
            socketChannel?.let {
                workerGroup.registerChannel(it, ChannelMeta(UUID.randomUUID().toString(), serverChannel, pipeline))
                log.debug("Accept a connection from {}", it.remoteAddress)
            }
        }
    }
}