package cn.kona.transport

import java.nio.channels.SocketChannel
import java.util.*

class PipelineBuilder {

    private var _startByte: Byte = 0
    private var _endByte: Byte = '\n'.toByte()
    private var _noStart: Boolean = true
    private var _end: (Any, SocketChannel) -> Unit = {_,_ ->}

    private lateinit var _thisChannel: SocketChannel

    private val _cells: LinkedList<Class<out Cell>> = LinkedList()

    fun startByte(value: Byte): PipelineBuilder {
        _startByte = value
        return this
    }

    fun endByte(value: Byte): PipelineBuilder {
        _endByte = value
        return this
    }

    fun noStart(value: Boolean): PipelineBuilder {
        _noStart = value
        return this
    }

    fun end(value: (Any, SocketChannel) -> Unit): PipelineBuilder {
        _end = value
        return this
    }

    fun thisChannel(value: SocketChannel): PipelineBuilder {
        _thisChannel = value
        return this
    }

    fun cells(vararg cells: Class<out Cell>) {
        _cells.addAll(cells)
    }

    fun create(): Pipeline {
        val pipeline = Pipeline(_startByte, _endByte, _noStart, _end)
        if (this::_thisChannel.isInitialized) {
            pipeline.setChannel(_thisChannel)
        }
        _cells.listIterator().forEachRemaining {
            pipeline.addCells(it.newInstance())
        }
        return pipeline
    }

}