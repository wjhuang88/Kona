@file:JvmName("HTTP")
package cn.kona.protocol.http

import cn.kona.systemCharset
import cn.kona.transport.Pipeline
import cn.kona.transport.impl.Acceptor
import org.slf4j.LoggerFactory
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.ServerSocketChannel

class HTTPServer {

}

private val log = LoggerFactory.getLogger(HTTPServer::class.java)

fun create(host: String? = null, port: Int = 8080): HTTPServer {

    return HTTPServer()
}