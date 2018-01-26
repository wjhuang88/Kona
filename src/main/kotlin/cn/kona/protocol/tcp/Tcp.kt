@file:JvmName("TCP")
package cn.kona.protocol.tcp

import cn.kona.systemCharset
import cn.kona.transport.Cell
import cn.kona.transport.ChannelMeta
import cn.kona.transport.PipelineBuilder
import cn.kona.transport.impl.Acceptor
import cn.kona.transport.pumper.BytePumper
import cn.kona.transport.pumper.FrameBytePumperFactory
import cn.kona.transport.pumper.PumperFactory
import org.slf4j.LoggerFactory
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.SelectionKey
import java.nio.channels.ServerSocketChannel

private val log = LoggerFactory.getLogger(TCPServer::class.java)

class TCPServer internal constructor(private val pipeBuilder: PipelineBuilder,
                                     private val acceptor: Acceptor,
                                     private val address: InetSocketAddress) {

    fun start() {
        log.info("Prepared server at $address")
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
           pumperFactory: PumperFactory<BytePumper> = FrameBytePumperFactory(),
           endHandler: (Any) -> Unit = System.out::println): TCPServer {

    log.info("Initiating tcp server.")
    val address = if (host != null) InetSocketAddress(host, port) else InetSocketAddress(port)
    val channel = ServerSocketChannel.open()
    channel.socket().bind(address)

    val builder = PipelineBuilder().bytePumper(pumperFactory).end { data, key ->
        when (data) {
            is ByteBuffer -> data
            is ByteArray -> ByteBuffer.wrap(data)
            is String -> ByteBuffer.wrap(data.toByteArray(systemCharset))
            else -> null
        }?.let { writeBuffer ->
            (key.attachment() as? ChannelMeta)?.let { meta ->
                meta.writeBuffer = writeBuffer
                key.interestOps(SelectionKey.OP_WRITE)
            }
        }
        endHandler(data)
    }
    log.info("Binding acceptor.")
    val acceptor = Acceptor(builder)
    acceptor.registerChannel(channel, "boss")

    return TCPServer(builder, acceptor, address)
}