package cn.kona.transport

/**
 * Data handle cell in a pipeline
 *
 * @author HuangWj
 */
abstract class Cell {

    lateinit var pipeline: Pipeline

    /**
     * Make data from source and delivery the returned data to next cell
     */
    abstract fun make(data: Any): Any
}