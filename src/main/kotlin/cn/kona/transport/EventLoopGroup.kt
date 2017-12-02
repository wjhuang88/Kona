package cn.kona.transport

import java.nio.channels.Channel
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread

interface EventLoopGroup : EventLoop, Iterable<EventLoop> {

    companion object {
        val threadCounter = AtomicInteger(0)
    }

    override fun run() {
        // do nothing.
    }

    override fun execute(command: Runnable?) {
        selectItem().execute(command)
    }

    override fun close() {
        iterator().forEachRemaining {
            it.close()
        }
    }

    override fun registerChannel(channel: Channel, attach: Any?) {
        val selectItem = selectItem()
        if (!selectItem.isRunning()) {
            thread(true, false, null, "Kona-worker-${threadCounter.getAndIncrement()}", -1, selectItem::run)
        }
        selectItem.registerChannel(channel, attach)
    }

    fun selectItem(): EventLoop
}