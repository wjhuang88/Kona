package cn.kona.transport

internal class BytePumper(private val startByte: Byte,
                          private val endByte: Byte,
                          private val noStart: Boolean,
                          private val callback: (Any) -> Unit) {

    private var frameStart = false

    private val bytePool = ByteGroup()

    fun push(b: Byte) {
        if (!frameStart && (noStart || b == startByte)) {
            frameStart = true
        }
        if (!frameStart || b == startByte) {
            return
        }
        if (b == endByte) {
            val buffer = bytePool.flush()
            callback(buffer)
            frameStart = false
        } else {
            bytePool.put(b)
        }
    }
}
