package cn.kona.transport

import cn.kona.transport.pumper.BytePumper
import cn.kona.transport.pumper.PumperFactory
import java.nio.channels.SocketChannel
import java.util.*

class PipelineBuilder {

    private lateinit var _bytePumper: PumperFactory<BytePumper>
    private var _end: (Any, SocketChannel) -> Unit = {_,_ ->}

    private lateinit var _thisChannel: SocketChannel

    private val _cells: LinkedList<Class<out Cell>> = LinkedList()

    fun bytePumper(value: PumperFactory<BytePumper>): PipelineBuilder {
        _bytePumper = value
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
        val pipeline = Pipeline(_bytePumper.create(), _end)
        if (this::_thisChannel.isInitialized) {
            pipeline.setChannel(_thisChannel)
        }
        _cells.listIterator().forEachRemaining {
            pipeline.addCells(it.newInstance())
        }
        return pipeline
    }

}