package cn.kona.transport.pumper

class HttpBytePumper : BytePumper() {

    private var bodyStart = false
    private var flipped = false

    private val bytePool = ByteGroup()

    private var bodyRemaining = 0

    override fun push(b: Byte) {
        if (!bodyStart) {
            pushToHeader(b)
        } else {
            pushToBody(b)
        }
    }

    fun flip(len: Int) {
        if (!flipped) {
            flipped = true
            bodyStart = true
            bodyRemaining = len
        }
    }

    fun reset() {
        bodyStart = false
        flipped = false
        bodyRemaining = 0
    }

    private fun pushToHeader(b: Byte) {
        if (b == '\n'.toByte()) {
            val buffer = bytePool.flush()
            pipeline.startup(buffer)
        } else {
            bytePool.put(b)
        }
    }

    private fun pushToBody(b: Byte) {
        if (bodyRemaining > 0) {
            bytePool.put(b)
            bodyRemaining--
            if (bodyRemaining <= 0) {
                val buffer = bytePool.flush()
                pipeline.startup(buffer)
            }
        }
    }
}
