package cn.kona

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.nio.ByteBuffer

@Suppress("unused")
inline fun <reified T> T.getLogger(): Logger = LoggerFactory.getLogger(T::class.java)

fun ByteBuffer.clone(): ByteBuffer = ByteBuffer.allocate(this.remaining()).put(this)