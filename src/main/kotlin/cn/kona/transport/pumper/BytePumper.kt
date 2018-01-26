package cn.kona.transport.pumper

import cn.kona.transport.Pipeline
import java.nio.ByteBuffer

abstract class BytePumper {

    internal lateinit var pipeline: Pipeline

    abstract fun push(b: Byte)

    abstract fun flush(len: Int): ByteBuffer
}
