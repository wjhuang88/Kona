package cn.kona

import cn.kona.event.EventBus
import cn.kona.event.EventBusImpl
import cn.kona.socket.NioTcpServer
import cn.kona.socket.Server

object Kona {
    init {
        System.setProperty("Log4jContextSelector", "org.apache.logging.log4j.core.async.AsyncLoggerContextSelector")
    }
    @JvmStatic fun eventBus(): EventBus = EventBusImpl()
    @JvmStatic fun tcp(): Server = NioTcpServer()
}