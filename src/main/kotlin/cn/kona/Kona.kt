package cn.kona

import cn.kona.event.EventBus
import cn.kona.event.EventBusImpl
import cn.kona.socket.NioTcpServer
import cn.kona.socket.Server

object Kona {
    @JvmStatic fun eventBus(): EventBus = EventBusImpl()
    @JvmStatic fun tcp(): Server = NioTcpServer()
}