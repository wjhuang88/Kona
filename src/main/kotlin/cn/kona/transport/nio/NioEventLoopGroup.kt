package cn.kona.transport.nio

import cn.kona.transport.EventLoop
import cn.kona.transport.EventLoopGroup
import java.nio.ByteBuffer
import java.nio.channels.SelectionKey
import java.nio.channels.SocketChannel
import java.util.concurrent.atomic.AtomicBoolean

internal abstract class NioEventLoopGroup(private val n: Int) : EventLoopGroup {

    private val items: MutableList<DirectBufferNioEventLoop> = ArrayList(n)

    @Volatile
    private var index = 0

    private val running = AtomicBoolean(false)

    init {
        repeat(n) {
            items.add(object : DirectBufferNioEventLoop() {
                override fun loopAction(key: SelectionKey) {
                    if (!isRunning()) {
                        key.channel().close()
                        key.cancel()
                    } else if (key.isReadable) {
                        (key.channel() as? SocketChannel)?.let {
                            readBytes(getBuffer(), it)
                        }
                    }
                }
            })
        }
    }

    abstract fun readBytes(buffer: ByteBuffer, channel: SocketChannel)

    override fun run() {
        running.getAndSet(true)
        super.run()
    }

    override fun selectItem(): EventLoop {
        return items[determineIndex()]
    }

    override fun iterator(): Iterator<EventLoop> {
        return items.iterator()
    }

    private fun determineIndex(): Int {
        return index++ % n
    }

    override fun close() {
        super.close()
        running.getAndSet(false)
    }

    override fun isRunning(): Boolean {
        return running.get()
    }

    override fun loopAction(key: SelectionKey) {
        // do nothing.
    }
}