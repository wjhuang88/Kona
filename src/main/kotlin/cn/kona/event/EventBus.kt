package cn.kona.event

import cn.kona.AsyncTask
import cn.kona.DisruptorExecutor
import java.util.concurrent.Executors

interface EventBus {
    fun publish(): EventBus
    fun send(): EventBus
    fun consume(): EventBus
}

class EventBusImpl internal constructor() : EventBus {

    private val executor = DisruptorExecutor<EventTask<*>>(Executors.newSingleThreadExecutor(), true)

    override fun publish(): EventBus {
        TODO()
    }

    override fun send(): EventBus {
        TODO()
    }

    override fun consume(): EventBus {
        TODO()
    }
}

class EventTask<T>(route: String, message: T)