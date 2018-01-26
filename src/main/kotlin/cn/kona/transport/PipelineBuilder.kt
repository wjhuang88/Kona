package cn.kona.transport

import cn.kona.transport.pumper.BytePumper
import cn.kona.transport.pumper.PumperFactory
import java.nio.channels.SelectionKey
import java.util.*

class PipelineBuilder {

    private lateinit var _bytePumper: PumperFactory<BytePumper>
    private var _end: (Any, SelectionKey) -> Unit = {_,_ ->}

    private lateinit var _thisKey: SelectionKey

    private val _cells: LinkedList<Class<out Cell>> = LinkedList()

    fun bytePumper(value: PumperFactory<BytePumper>): PipelineBuilder {
        _bytePumper = value
        return this
    }

    fun end(value: (Any, SelectionKey) -> Unit): PipelineBuilder {
        _end = value
        return this
    }

    fun thisKey(value: SelectionKey): PipelineBuilder {
        _thisKey = value
        return this
    }

    fun cells(vararg cells: Class<out Cell>) {
        _cells.addAll(cells)
    }

    fun create(): Pipeline {
        val pipeline = Pipeline(_bytePumper.create(), _end)
        if (this::_thisKey.isInitialized) {
            pipeline.setKey(_thisKey)
        }
        _cells.listIterator().forEachRemaining {
            pipeline.addCells(it.newInstance())
        }
        return pipeline
    }
}