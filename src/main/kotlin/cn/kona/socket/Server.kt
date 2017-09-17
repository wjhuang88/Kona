package cn.kona.socket

interface Server {
    fun listen(port: Int): Server
}

internal class NioTcpServer internal constructor() : Server {

    override fun listen(port: Int): Server {
        NioTcpEventLoop(port).loop()
        return this
    }
}