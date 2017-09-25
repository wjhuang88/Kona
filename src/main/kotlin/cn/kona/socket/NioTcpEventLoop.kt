package cn.kona.socket

import cn.kona.getLogger
import java.nio.channels.SelectionKey
import java.nio.channels.Selector
import java.util.concurrent.atomic.AtomicBoolean

internal class NioTcpEventLoop internal constructor(private val port: Int): EventLoop {

    private val running = AtomicBoolean(false)

    private val selector = Selector.open()

    private val acceptor = Acceptor(selector)

    private val worker = WorkerGroup(selector)

    private val log = getLogger()

    init {
        Runtime.getRuntime().addShutdownHook(Thread {
            this.close()
        })
    }

    override fun loop(): EventLoop {
        if (running.get()) return this
        log.info("Initiating socket dispatcher loop")
        running.getAndSet(true)
        acceptor.start(port)
        worker.start()
        dispatchLoop()
        return this
    }

    override fun close() {
        running.getAndSet(false)
        log.info("Closing socket dispatcher loop")
        acceptor.close()
        worker.close()
    }

    private tailrec fun dispatchLoop() {
        val n = selector.select()
        if (n > 0) selector.selectedKeys().iterator().let {
            while (it.hasNext()) {
                val key = it.next()
                it.remove()
                val accepted = acceptor.accept(key) {
                    worker.register(it, SelectionKey.OP_READ)
                }
                if (!accepted) worker.work(key)
            }
        } else {
            // TODO: If JVM bug exists, fix it!
            log.debug("Selector.select() return 0, a JVM bug may be triggered")
        }
        if (running.get()) dispatchLoop()
    }
}