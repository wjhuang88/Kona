package cn.kona.transport

/**
 * Data handle cell in a pipeline
 *
 * @author HuangWj
 */
interface Cell {

    /**
     * Make data from source and delivery the returned data to next cell
     */
    fun make(data: Any): Any
}