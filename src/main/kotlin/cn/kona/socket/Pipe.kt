package cn.kona.socket

import java.nio.ByteBuffer

class Pipe {

    private val inputHandler = object : PipeHandler {

        override var next: PipeHandler? = null

        override fun handle(buf: ByteBuffer) {
            TODO()
            next(buf)
        }
    }

    private val endHandler = object : PipeHandler {

        override var next: PipeHandler? = null

        override fun handle(buf: ByteBuffer) {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }
    }

    private var lastHandler: PipeHandler = inputHandler

    fun pipe(vararg handlers: PipeHandler): Pipe {
        val last = when(handlers.size) {
            0 -> {
                lastHandler
            }
            1 -> {
                lastHandler.next = handlers[0]
                handlers[0]
            }
            else -> handlers.also { lastHandler.next = it[0] }
                    .reduce { acc, handler ->
                acc.next = handler
                handler
            }
        }
        lastHandler = last
        last.next = endHandler
        return this
    }

    internal fun delivery(buf: ByteBuffer) {
        inputHandler.handle(buf)
    }
}