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
            val children = object : DirectBufferNioEventLoop() {
                override fun loopAction(key: SelectionKey) {
                    if (!isRunning()) {
                        key.channel().close()
                        key.cancel()
                    } else if (key.isWritable) {
                        writeBytes(key)
                    } else if (key.isReadable) {
                        readBytes(getBuffer(), key)
                    }
                }
            }
            items.add(children)
        }
    }

    abstract fun readBytes(buffer: ByteBuffer, key: SelectionKey)

    abstract fun writeBytes(key: SelectionKey)

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