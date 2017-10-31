package cn.kona.socket

import cn.kona.DisruptorExecutor
import cn.kona.clone
import cn.kona.getLogger
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.newSingleThreadContext
import kotlinx.coroutines.experimental.runBlocking
import kotlinx.coroutines.experimental.sync.Mutex
import sun.nio.ch.DirectBuffer
import java.io.Closeable
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.channels.SelectionKey
import java.nio.channels.Selector
import java.nio.channels.SocketChannel
import java.util.*
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

internal class WorkerGroup(private val selector: Selector) : Closeable {

    private val running = AtomicBoolean(false)

    private val log = getLogger()

    private val byteBuffer = ByteBuffer.allocateDirect(1024)

    private val disruptorExecutor = DisruptorExecutor<SelectionKey>(Executor {
        val thread = Executors.defaultThreadFactory().newThread(it)
        thread.name = "worker-dispatcher"
        thread.start()
    }, false)

    fun start() {
        log.info("Initiating worker-group")
        running.getAndSet(true)
    }

    internal fun register(channel: SocketChannel, ops: Int, id: UUID = UUID.randomUUID()) {
        channel.register(selector, ops, id)
        log.debug("Accept and register a connect, id: {}", id)
    }

    fun work(key: SelectionKey): Boolean {
        return if (!running.get()) {
            key.cancel()
            key.channel().close()
            false
        } else if (key.isReadable) {
            readHandler(key)
            true
        } else {
            false
        }
    }

    private fun readHandler(key: SelectionKey?) {
        (key?.channel() as? SocketChannel)?.let {
            byteBuffer.clear()
            val read = it.read(byteBuffer)
            if (read > 0) {
                byteBuffer.flip()
                byteBuffer.clone().let { buf ->
                    launch(newSingleThreadContext("worker-reader")) {
                        log.info(String(buf.array()))
                        // TODO: add Pipe logic to handle data, remove test code.
                        launch(newSingleThreadContext("worker-writer")) {
                            val fileChannel = FileInputStream("/Users/GHuang/WorkSpace/car-loan/web-admin/README.md").channel
                            it.write(ByteBuffer.wrap("HTTP/1.1 200 OK\nConnection: keep-alive\nContent-Length: ${fileChannel.size()}\n\n".toByteArray()))
                            fileChannel.let { f ->
                                f.transferTo(0, f.size(), it)
                            }
                        }
                    }
                }
            }
            if (read == -1) {
                it.close()
                key?.cancel()
            } else {
//                key?.interestOps(SelectionKey.OP_READ or SelectionKey.OP_WRITE)
//                val r = java.lang.String(accepted.toByteArray())
//                key?.attach(r)
            }
        }
    }

    override fun close() {
        running.getAndSet(false)
        log.info("Closing worker-group")
        selector.close()
        (byteBuffer as DirectBuffer).cleaner().clean()
    }
}