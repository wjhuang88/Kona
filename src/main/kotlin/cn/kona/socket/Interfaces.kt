package cn.kona.socket

import java.io.Closeable
import java.nio.ByteBuffer

interface EventLoop : Closeable {
    fun loop(): EventLoop
}

interface Server {
    fun listen(port: Int): Server
}

interface PipeHandler {
    var next: PipeHandler?
    fun handle(buf: ByteBuffer)
    fun next(buf: ByteBuffer) = next?.handle(buf)
}