@file:JvmName("TCP")
package cn.kona.protocol.tcp

import cn.kona.systemCharset
import cn.kona.transport.Cell
import cn.kona.transport.PipelineBuilder
import cn.kona.transport.impl.Acceptor
import org.slf4j.LoggerFactory
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.ServerSocketChannel

private val log = LoggerFactory.getLogger(TCPServer::class.java)

class TCPServer internal constructor(private val pipeBuilder: PipelineBuilder,
                                     private val acceptor: Acceptor,
                                     private val address: InetSocketAddress) {

    fun start() {
        log.info("Prepared for server at $address")
        acceptor.run()
    }

    @SafeVarargs
    fun registerHandler(vararg cells: Class<out Cell>): TCPServer {
        pipeBuilder.cells(*cells)
        log.info("Registered ${cells.size} cells")
        return this
    }
}

@JvmOverloads
fun create(host: String? = null,
           port: Int = 8080,
           start: Byte = 0,
           end: Byte = '\n'.toByte(),
           noStart: Boolean = true,
           endHandler: (Any) -> Unit = System.out::println): TCPServer {

    log.info("Initiating tcp server.")
    val address = if (host != null) InetSocketAddress(host, port) else InetSocketAddress(port)
    val channel = ServerSocketChannel.open()
    channel.socket().bind(address)

    val builder = PipelineBuilder()
            .startByte(start).endByte(end).noStart(noStart).end { data, ch ->
        when (data) {
            is ByteBuffer -> ch.write(data)
            is ByteArray -> ch.write(ByteBuffer.wrap(data))
            is String -> ch.write(ByteBuffer.wrap(data.toByteArray(systemCharset)))
        }
        endHandler(data)
    }
    log.info("Binding acceptor.")
    val acceptor = Acceptor(builder)
    acceptor.registerChannel(channel, "boss")

    return TCPServer(builder, acceptor, address)
}