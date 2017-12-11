package cn.kona.transport.nio

import cn.kona.forEachAndRemove
import cn.kona.getLogger
import cn.kona.transport.EventLoop
import cn.kona.transport.TaskWeaver
import cn.kona.transport.impl.QueueTaskWeaver
import java.nio.channels.*
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

internal abstract class NioEventLoop(private var selector: Selector = Selector.open()): EventLoop {

    companion object {
        const val SELECTOR_REBUILD_COUNT = 512
    }

    private val log = getLogger()

    private val running = AtomicBoolean(false)

    private val taskWeaver: TaskWeaver = QueueTaskWeaver()

    override fun run() {
        running.getAndSet(true)
        log.debug("Starting main loop.")
        mainLoop(this::loopAction)
    }

    override fun execute(command: Runnable?) {
        if (running.get()) {
            command?.let(taskWeaver::addTask)
        }
    }

    override fun close() {
        running.getAndSet(false)
    }

    override fun isRunning(): Boolean {
        return running.get()
    }

    override fun registerChannel(channel: Channel, attach: Any?) {
        if (channel is SocketChannel) {
            channel.configureBlocking(false)
            channel.register(selector, SelectionKey.OP_READ, attach)
        } else if (channel is ServerSocketChannel) {
            channel.configureBlocking(false)
            channel.register(selector, SelectionKey.OP_ACCEPT, attach)
        }
    }

    private tailrec fun mainLoop(action: (SelectionKey) -> Unit) {

        val timeout = calculateTimeout()
        val before = System.nanoTime()
        doSelect(before, timeout, 0)

        selector.selectedKeys().forEachAndRemove(action)

        // Have to work again and again until death!
        if (running.get()) mainLoop(action)
    }

    private tailrec fun doSelect(startTime: Long, timeout: Long, count: Int): Int {
        var nextCount = count
        if (timeout <= 0) {
            if (nextCount == 0) {
                selector.selectNow()
                return 1
            }
            return nextCount
        }

        val n = selector.select(timeout)
        nextCount++
        // Here we will return if everything is ok.
        if (n != 0) return nextCount

        if (Thread.interrupted()) {
            if (log.isDebugEnabled)
                log.debug("Selector.select() returned prematurely because " +
                        "Thread.currentThread().interrupt() was called. Use ")
            return nextCount
        }

        val after = System.nanoTime()
        if (after - TimeUnit.MILLISECONDS.toNanos(timeout) >= startTime) {
            nextCount = 1
        } else if (nextCount >= SELECTOR_REBUILD_COUNT) {
            log.warn("Selector.select() returned prematurely {} times in a row; rebuilding Selector {}.",
                    nextCount, selector)
            rebuildSelector()
            selector.selectNow()
            return 1
        }

        // Maybe there's a JVM bug and let's run another loop anyway.
        return doSelect(after, timeout, nextCount)
    }

    private fun calculateTimeout(): Long = 30

    // rebuild selector to fix cpu 100% bug
    private fun rebuildSelector() {
        val oldSelector = selector
        val newSelector = try {
            Selector.open()
        } catch (e: Exception) {
            log.warn("Failed to create a new Selector.", e)
            return@rebuildSelector
        }

        var nChannels = 0
        oldSelector.keys().filter { key ->
            key.isValid && key.channel().keyFor(newSelector) == null
        }.forEach { key ->
            val a = key.attachment()
            val interestOps = key.interestOps()
            key.cancel()
            try {
                key.channel().register(newSelector, interestOps, a)
            } catch (e: Exception) {
                log.warn("Failed to re-register a Channel to the new Selector.", e)
            }
            nChannels++
            selector = newSelector

            try {
                oldSelector.close()
            } catch (t: Throwable) {
                if (log.isWarnEnabled) {
                    log.warn("Failed to close the old Selector.", t)
                }
            }

            log.info("Migrated $nChannels channel(s) to the new Selector.")
        }
    }
}
