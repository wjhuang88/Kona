package cn.kona.socket

import cn.kona.AsyncTask
import cn.kona.DisruptorExecutor
import kotlinx.coroutines.experimental.runBlocking
import java.io.Closeable
import java.io.FileInputStream
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.SelectionKey
import java.nio.channels.Selector
import java.nio.channels.ServerSocketChannel
import java.nio.channels.SocketChannel
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.system.measureTimeMillis

interface EventLoop : Closeable {
    fun loop(): EventLoop
}

internal class NioTcpEventLoop internal constructor(private val port: Int): EventLoop {

    private val executor = DisruptorExecutor<SelectionKey>(false)
    private val isRunning = AtomicBoolean(false)

    override fun loop(): EventLoop {
        if (isRunning.get()) return this
        isRunning.getAndSet(true)
        val serverSocketChannel = ServerSocketChannel.open()
        val serverSocket = serverSocketChannel.socket()
        serverSocket.bind(InetSocketAddress(port))
        serverSocketChannel.configureBlocking(false)
        val selector = Selector.open()
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT)
        runBlocking { dispatchSocket(selector) }
        return this
    }

    override fun close() {
        executor.close()
        isRunning.compareAndSet(true, false)
    }

    private tailrec suspend fun dispatchSocket(selector: Selector) {
        val n = selector.select()
        if (n > 0) selector.selectedKeys().iterator().let {
            while (it.hasNext()) {
                val key = it.next()
                if (!isRunning.get()) {
                    key.cancel()
                    key.channel().close()
                } else when {
                    key.isAcceptable -> {
                        val channel = key.channel() as? ServerSocketChannel
                        val accept = channel?.accept()
                        accept?.configureBlocking(false)
                        accept?.register(selector, SelectionKey.OP_READ)
                    }
                    key.isReadable -> {
                        val task = AsyncTask<SelectionKey>()
                        task.data = key
                        task.errorHandler = errorHandler
                        task.handler = readHandler
                        measureTimeMillis { executor.execute(task) }.let {
                            // println("read time: $it")
                        }
                    }
                    key.isWritable -> {
                        val task = AsyncTask<SelectionKey>()
                        task.data = key
                        task.errorHandler = errorHandler
                        task.handler = writeHandler
                        measureTimeMillis { executor.execute(task) }.let {
                            // println("write time: $it")
                        }
                    }
                }; it.remove()
            }
        }
        if (isRunning.get()) dispatchSocket(selector)
    }

    private val byteBuffer = ByteBuffer.allocateDirect(1024)

    private val errorHandler: (Exception) -> Unit = { e ->
        e.printStackTrace()
    }

    private val readHandler: suspend (SelectionKey?) -> Any = { key: SelectionKey? ->
        // println("Read: ${Thread.currentThread().name}")
        (key?.channel() as? SocketChannel)?.let {
            byteBuffer.clear()
            var accepted = arrayOf<Byte>()
            while (it.read(byteBuffer) > 0) {
                byteBuffer.flip()
                while (byteBuffer.hasRemaining()) {
                    accepted += byteBuffer.get()
                }
                byteBuffer.clear()
            }
            if (it.read(byteBuffer) == -1) {
                it.close()
            } else {
                key?.interestOps(SelectionKey.OP_WRITE)
                val r = java.lang.String(accepted.toByteArray())
                key?.attach(r)
            }
        }
        Unit
    }

    private val writeHandler: suspend (SelectionKey?) -> Any = { key: SelectionKey? ->
        // println("Write: ${Thread.currentThread().name}")
        (key?.channel() as? SocketChannel)?.let {
            val r = key?.attachment() as String
            if (r != "\n" && r != "\r" && r != "\r\n") {
                val fileChannel = FileInputStream("/Users/GHuang/WorkSpace/car-loan/web-admin/README.md").channel
                it.write(ByteBuffer.wrap("HTTP/1.1 200 OK\nConnection: keep-alive\nContent-Length: ${fileChannel.size()}\n\n".toByteArray()))
                try {
                    fileChannel.let { f ->
                        f.transferTo(0, f.size(), it)
                    }
                } catch (e: Exception) {

                }
            }
            key.interestOps(SelectionKey.OP_READ)
        }
        Unit
    }
}