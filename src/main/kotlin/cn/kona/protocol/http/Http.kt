@file:JvmName("HTTP")
package cn.kona.protocol.http

import cn.kona.protocol.tcp.TCPServer
import cn.kona.transport.Cell
import cn.kona.transport.pumper.HttpBytePumper
import cn.kona.transport.pumper.HttpBytePumperFactory
import org.slf4j.LoggerFactory
import java.nio.ByteBuffer

class HTTPServer internal constructor(private val tcp: TCPServer) {

    fun start() {
        tcp.start()
    }
}

private val log = LoggerFactory.getLogger(HTTPServer::class.java)

fun create(host: String? = null, port: Int = 8080): HTTPServer {

    val tcp = cn.kona.protocol.tcp.create(host, port, HttpBytePumperFactory(), {})
    tcp.registerHandler(HttpContextReader::class.java, HttpContextWriter::class.java)

    return HTTPServer(tcp)
}

class HttpContextReader : Cell() {

    private val httpContext = HttpContext()

    private val pumper: HttpBytePumper
        get() { return pipeline.bytePumper as HttpBytePumper }

    override fun make(data: Any): Any {
        if (data is ByteBuffer) {
            httpContext.inputLine(data)
            if (httpContext.headersFinished) {
                pumper.flip(httpContext.contextLength)
            }
            if (httpContext.ended) {
                pumper.reset()
            }
        }
        return httpContext
    }
}

class HttpContextWriter : Cell() {

    override fun make(data: Any): Any {
        if (data is HttpContext) {
            data.testGet()
        }
        return "HTTP/1.1 200 OK\nConnection: keep-alive\nContent-Length: 9\n\ntest res."
    }
}