@file:JvmName("TCP")
package cn.kona.protocol.tcp

import cn.kona.transport.Cell
import cn.kona.transport.Pipeline
import cn.kona.transport.impl.Acceptor
import org.slf4j.LoggerFactory
import java.net.InetSocketAddress
import java.nio.channels.ServerSocketChannel

private val log = LoggerFactory.getLogger(TCPServer::class.java)

class TCPServer internal constructor(private val pipeline: Pipeline,
                                     private val acceptor: Acceptor,
                                     private val address: InetSocketAddress) {

    fun start() {
        log.info("Prepared for server at $address")
        acceptor.run()
    }

    fun registerHandler(vararg cells: Cell): TCPServer {
        pipeline.addCells(*cells)
        log.info("Registered ${cells.size} cells")
        return this
    }
}

@JvmOverloads
fun create(host: String? = null,
           port: Int = 8080,
           start: Byte = 0,
           end: Byte = '\n'.toByte(),
           noStart: Boolean = true): TCPServer {

    log.info("Initiating tcp server.")
    val address = if (host != null) InetSocketAddress(host, port) else InetSocketAddress(port)
    val channel = ServerSocketChannel.open()
    channel.socket().bind(address)
    val pipeline = Pipeline(start, end, noStart) { data ->
        log.info(data.toString())
        Unit
    }
    log.info("Binding acceptor.")
    val acceptor = Acceptor(pipeline)
    acceptor.registerChannel(channel, "boss")
    return TCPServer(pipeline, acceptor, address)
}