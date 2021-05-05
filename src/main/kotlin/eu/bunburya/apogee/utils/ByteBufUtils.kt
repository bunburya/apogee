package eu.bunburya.apogee.utils

import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled

fun ByteArray.toByteBuf() = Unpooled.wrappedBuffer(this)
fun String.toByteBuf() = Unpooled.wrappedBuffer(this.toByteArray())