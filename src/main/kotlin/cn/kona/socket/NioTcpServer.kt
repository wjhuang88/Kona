package cn.kona.socket

internal class NioTcpServer internal constructor() : Server {

    override fun listen(port: Int): Server {
        NioTcpEventLoop(port).loop()
        return this
    }
}