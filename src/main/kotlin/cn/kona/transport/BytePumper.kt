package cn.kona.transport

internal class BytePumper(private val callback: (Any) -> Unit) {

    private var frameStart = false

    var startByte: Byte = '0'.toByte()
    val endByte: Byte = '1'.toByte()
    val noStart: Boolean = false

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
