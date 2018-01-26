@file:JvmName("HTTP")
package cn.kona.protocol.http

import cn.kona.protocol.tcp.TCPServer
import cn.kona.transport.Cell
import cn.kona.transport.pumper.FrameBytePumperFactory
import org.slf4j.LoggerFactory
import java.nio.ByteBuffer

class HTTPServer internal constructor(private val tcp: TCPServer) {

    fun start() {
        tcp.start()
    }
}

private val log = LoggerFactory.getLogger(HTTPServer::class.java)

fun create(host: String? = null, port: Int = 8080): HTTPServer {

    val tcp = cn.kona.protocol.tcp.create(host, port, FrameBytePumperFactory(), {})
    tcp.registerHandler(HttpContextReader::class.java, HttpContextWriter::class.java)

    return HTTPServer(tcp)
}

class HttpContextReader : Cell() {

    private val httpContext = HttpContext()

    override fun make(data: Any): Any {
        if (data is ByteBuffer) {
            httpContext.inputLine(data)
        }
        return httpContext
    }
}

class HttpContextWriter : Cell() {

    override fun make(data: Any): Any {
        if (data is HttpContext) {
            data.testGet()
        }
        return data
    }
}