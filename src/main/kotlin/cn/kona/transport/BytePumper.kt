package cn.kona.transport

import java.nio.charset.Charset
import java.util.*

internal class BytePumper(private val callback: (Any) -> Unit) {

    companion object {
        const val BUCKET_SIZE = 1024
    }
    private var index = 0
    private var offset = 0

    private var frameStart = false

    private var pool = mutableListOf<ByteArray>()

    init {
        pool.add(ByteArray(BUCKET_SIZE))
    }

    fun push(b: Byte) {
        val startByte: Byte = '0'.toByte() // TODO: test data
        val endByte: Byte = '1'.toByte()
        when (b) {
            startByte // frame start signal
            -> frameStart = true
            endByte // frame end signal
            -> {
                if (frameStart) {
                    handlePool()
                    frameStart = false
                }
                index = 0
                offset = 0
                pool = mutableListOf()
                pool.add(ByteArray(BUCKET_SIZE))
            }
            else // frame data
            -> if (frameStart) {
                pool[index][offset++] = b
                if (offset == BUCKET_SIZE) {
                    index++
                    offset = 0
                    pool.add(index, ByteArray(BUCKET_SIZE))
                }
            }
        }
    }

    private fun handlePool() {
        if (index == 0) {
            callback(String(pool[index], 0, offset, Charset.forName("UTF-8")))
        } else {
            val rst = Arrays.copyOf(pool[0], BUCKET_SIZE * index + offset)
            for (i in 1 until index) {
                val thisBytes = pool[i]
                System.arraycopy(thisBytes, 0, rst, BUCKET_SIZE * i, thisBytes.size)
            }
            val lastBytes = pool[index]
            System.arraycopy(lastBytes, 0, rst, BUCKET_SIZE * index, offset)
            callback(String(rst, Charset.forName("UTF-8")))
        }
    }
}
