package cn.kona.transport.pumper

class FrameBytePumperFactory(private val startByte: Byte = 0,
                             private val endByte: Byte = '\n'.toByte(),
                             private val noStart: Boolean = true) : PumperFactory<FrameBytePumper> {

    override fun create(): FrameBytePumper {
        return FrameBytePumper(startByte, endByte, noStart)
    }
}