package cn.kona.transport

import java.io.Closeable
import java.nio.channels.Channel
import java.nio.channels.SelectionKey
import java.util.concurrent.Executor

interface EventLoop: Runnable, Executor, Closeable {
    fun loopAction(key: SelectionKey)
    fun registerChannel(channel: Channel, attach: Any?): SelectionKey?
    fun isRunning(): Boolean
}