package cn.kona.socket

import cn.kona.getLogger
import java.io.Closeable
import java.net.InetSocketAddress
import java.nio.channels.SelectionKey
import java.nio.channels.Selector
import java.nio.channels.ServerSocketChannel
import java.nio.channels.SocketChannel
import java.util.concurrent.atomic.AtomicBoolean

internal class Acceptor(private val selector: Selector) : Closeable {

    private val running = AtomicBoolean(false)

    private val log = getLogger()

    private val serverSocketChannel = ServerSocketChannel.open()

    internal fun start(port: Int, host: String = ""): Acceptor {
        if (running.get()) return this
        log.info("Initiating acceptor")
        val address = if (host.isBlank()) InetSocketAddress(port) else InetSocketAddress(host, port)
        serverSocketChannel.socket().bind(address)
        serverSocketChannel.configureBlocking(false)
        log.info("Registering server socket {}:{}", host, port)
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT)
        running.getAndSet(true)
        return this
    }

    internal inline fun accept(key: SelectionKey, register: (SocketChannel) -> Unit): Boolean {
        return if (!running.get()) {
            key.cancel()
            key.channel().close()
            false
        } else if (key.isAcceptable) {
            val channel = key.channel() as? ServerSocketChannel
            val accept = channel?.accept()
            accept?.let {
                log.debug("Accept a connection from {}", it.remoteAddress)
                it.configureBlocking(false)
                register(it)
            }
            true
        } else {
            false
        }
    }

    override fun close() {
        log.info("Closing acceptor")
        running.getAndSet(false)
        selector.close()
        serverSocketChannel.close()
    }
}