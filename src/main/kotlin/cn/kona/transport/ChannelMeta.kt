package cn.kona.transport

import java.nio.ByteBuffer
import java.nio.channels.ServerSocketChannel

internal data class ChannelMeta(
        val id: String,
        val sch: ServerSocketChannel,
        var pipeline: Pipeline? = null,
        var writeBuffer: ByteBuffer? = null)