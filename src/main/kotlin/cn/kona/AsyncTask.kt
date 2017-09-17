package cn.kona

import com.lmax.disruptor.EventFactory
import com.lmax.disruptor.EventHandler
import com.lmax.disruptor.RingBuffer
import com.lmax.disruptor.YieldingWaitStrategy
import com.lmax.disruptor.dsl.Disruptor
import com.lmax.disruptor.dsl.ProducerType
import kotlinx.coroutines.experimental.*
import kotlinx.coroutines.experimental.channels.Channel
import java.io.Closeable
import java.util.concurrent.Executors
import kotlin.coroutines.experimental.CoroutineContext

open class AsyncTask<T> {
    @JvmField internal var data: T? = null
    internal lateinit var handler: suspend (T?) -> Any
    internal lateinit var errorHandler: (Exception) -> Unit
    private val chan = Channel<Any>()

    fun start(context: CoroutineContext) = launch(context) {
        try {
            val ret = handler(data)
            chan.send(ret)
        } catch (e: Exception) {
            errorHandler(e)
        }
    }

    suspend fun await() = chan.receive()

    fun end() = chan.close()
}

class DisruptorExecutor<T>(multiProducer: Boolean = false) : Closeable {

    private lateinit var task: AsyncTask<T>

    private val disruptor = Disruptor<AsyncTaskWrapper<T>>(
            EventFactory { AsyncTaskWrapper<T>() },
            1024,
            Executors.defaultThreadFactory(),
            if (multiProducer) ProducerType.MULTI else ProducerType.SINGLE,
            YieldingWaitStrategy())

    init {
        listen()
    }

    suspend fun execute(task: AsyncTask<T>): Any {
        this.task = task
        disruptor.ringBuffer.publishEvent { wrapper, _ ->
            wrapper.task = task
        }
        return task.await()
    }

    override fun close() {
        task.end()
        disruptor.shutdown()
    }

    private fun listen(): RingBuffer<AsyncTaskWrapper<T>> {
        disruptor.handleEventsWith(EventHandler { taskWrapper, _, _ ->
            taskWrapper.task.start(Unconfined)
        })
        return disruptor.start()
    }
}

class AsyncTaskWrapper<T> {
    internal lateinit var task: AsyncTask<T>
}