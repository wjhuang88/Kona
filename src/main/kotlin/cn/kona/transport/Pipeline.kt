package cn.kona.transport

internal class Pipeline {

    private val bytePumper = BytePumper(this::startup)

    private val startCell = ChainedCell(object : Cell {
        override fun make(data: Any): Any {
            return data
        }
    }, null)

    private fun startup(data: Any) {
        var finalData = startCell.cell.make(data)
        var wrappedCell = startCell
        while (null != wrappedCell.next) {
            finalData = wrappedCell.cell.make(finalData)
            wrappedCell.next?.let { wrappedCell = it }
        }
        wrappedCell.cell.make(finalData)
    }

    fun pump(byte: Byte) = bytePumper.push(byte)

    fun addCells(vararg cells: Cell) {
        when {
            cells.isEmpty() -> return
            cells.size == 1 -> addToLast(ChainedCell(cells[0], null))
            else -> {
                val itr = cells.iterator()
                var last = ChainedCell(itr.next(), null)
                addToLast(last)
                while (itr.hasNext()) {
                    val current = itr.next()
                    val wrapped = ChainedCell(current, null)
                    last.next = wrapped
                    last = wrapped
                }
            }
        }
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