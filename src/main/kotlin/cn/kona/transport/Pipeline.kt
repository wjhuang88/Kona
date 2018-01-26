package cn.kona.transport

import cn.kona.transport.pumper.BytePumper
import java.nio.ByteBuffer
import java.nio.channels.SocketChannel

/**
 * Chained handlers to handle data
 *
 * @property end execute when chained cell finished
 * @property bytePumper used to cache byte and pump right frame
 *
 * @author HuangWj
 */
class Pipeline internal constructor(private val bytePumper: BytePumper,
                                    private val end: (Any, SocketChannel) -> Unit) {

    init {
        bytePumper.pipeline = this
    }

    private lateinit var thisChannel: SocketChannel

    private val startCell = ChainedCell(object : Cell() {

        init {
            this.pipeline = this@Pipeline
        }

        override fun make(data: Any): Any {
            return data
        }
    }, null)

    internal fun startup(data: Any) {
        var finalData = startCell.cell.make(data)
        var wrappedCell = startCell
        while (null != wrappedCell.next) {
            finalData = wrappedCell.cell.make(finalData)
            wrappedCell.next?.let { wrappedCell = it }
        }
        if (!this::thisChannel.isInitialized) {
            throw IllegalStateException("Channel is not registered.")
        }
        end(wrappedCell.cell.make(finalData), thisChannel)
    }

    /**
     * push a byte in
     */
    fun pump(byte: Byte) = bytePumper.push(byte)

    /**
     * flush all remaining data in pumper
     */
    fun flush(len: Int): ByteBuffer = bytePumper.flush(len)

    /**
     * add some pipeline handle cells
     */
    fun addCells(vararg cells: Cell) = when {
        cells.isEmpty() -> {}
        cells.size == 1 -> {
            cells[0].pipeline = this
            addToLast(ChainedCell(cells[0], null))
        }
        else -> {
            val itr = cells.iterator()
            var last = ChainedCell(itr.next(), null)
            last.cell.pipeline = this
            addToLast(last)
            while (itr.hasNext()) {
                val current = itr.next()
                current.pipeline = this
                val wrapped = ChainedCell(current, null)
                last.next = wrapped
                last = wrapped
            }
        }
    }

    internal fun setChannel(channel: SocketChannel) {
        thisChannel = channel
    }

    private fun addToLast(chainedCell: ChainedCell) {
        var current = startCell
        while (null != current.next) {
            current.next?.let { current = it }
        }
        current.next = chainedCell
    }

    inner class ChainedCell(val cell: Cell, var next: ChainedCell?)
}