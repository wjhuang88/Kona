package cn.kona.transport.nio

import sun.misc.Cleaner
import java.nio.ByteBuffer
import java.nio.channels.Selector


internal abstract class DirectBufferNioEventLoop(selector: Selector = Selector.open()) : NioEventLoop(selector) {
    private val byteBuffer = ByteBuffer.allocateDirect(1024)
    protected fun getBuffer(): ByteBuffer = byteBuffer
    override fun close() {
        val cleanerField = byteBuffer.javaClass.getDeclaredField("cleaner")
        cleanerField.isAccessible = true
        val cleaner = cleanerField.get(byteBuffer) as Cleaner
        cleaner.clean()
        super.close()
    }
}