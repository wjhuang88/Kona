package cn.kona.transport

import java.nio.channels.ServerSocketChannel

internal data class ChannelMeta(val id: String, val sch: ServerSocketChannel)