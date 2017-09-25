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
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory
import kotlin.coroutines.experimental.CoroutineContext

open internal class AsyncTask<T>(private var data: T, private var handler: (T) -> Any) {
    fun start() = handler(data)
}

internal class DisruptorExecutor<T>(executor: Executor, multiProducer: Boolean = false) : Closeable {

    private lateinit var task: AsyncTask<T>

    @Suppress("deprecation")
    private val disruptor = Disruptor<AsyncTaskWrapper<T>>(
            EventFactory { AsyncTaskWrapper<T>() },
            1024,
            executor,
            if (multiProducer) ProducerType.MULTI else ProducerType.SINGLE,
            YieldingWaitStrategy())

    init {
        listen()
    }

    private fun execute(task: AsyncTask<T>): DisruptorExecutor<T> {
        this.task = task
        disruptor.ringBuffer.publishEvent { wrapper, _ ->
            wrapper.task = task
        }
        return this
    }

    fun execute(data: T, handler: (T) -> Any) = execute(AsyncTask(data, handler))

    override fun close() {
        disruptor.shutdown()
    }

    private fun listen(): RingBuffer<AsyncTaskWrapper<T>> {
        disruptor.handleEventsWith(EventHandler { taskWrapper, _, _ ->
            taskWrapper.task.start()
        })
        return disruptor.start()
    }
}

private class AsyncTaskWrapper<T> {
    internal lateinit var task: AsyncTask<T>
}