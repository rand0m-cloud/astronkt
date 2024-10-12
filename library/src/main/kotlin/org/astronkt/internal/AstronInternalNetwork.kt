@file:Suppress("MemberVisibilityCanBePrivate")

package org.astronkt.internal

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import java.io.OutputStream
import java.net.Socket
import java.nio.ByteBuffer
import java.nio.ByteOrder

class AstronInternalNetwork(val coroutineScope: CoroutineScope) {
    private lateinit var socket: Socket
    private lateinit var socketOutputStream: OutputStream

    private val _networkMessages = MutableSharedFlow<AstronInternalMessage>()
    val networkMessages: MutableSharedFlow<AstronInternalMessage> = _networkMessages

    fun connect(mdAddress: String) {
        val (host, port) = mdAddress.split(":")
        socket = Socket(host, port.toInt())
        socketOutputStream = socket.getOutputStream()

        coroutineScope.launch {
            val input = socket.getInputStream()
            input.use {
                while (isActive) {
                    yield()

                    if (input.available() < 2) {
                        continue
                    }

                    val len = ByteBuffer.wrap(input.readNBytes(2)).order(
                        ByteOrder.LITTLE_ENDIAN
                    ).getShort()
                    val data = input.readNBytes(len.toInt()).also {
                        assert(it.size == len.toInt())
                    }

                    _networkMessages.emit(AstronInternalMessage.fromBytes(data))
                }
            }
        }
    }

    fun sendMessage(msg: AstronInternalMessage) {
        socketOutputStream.write(msg.toBytes())
        socketOutputStream.flush()
    }
}