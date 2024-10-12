@file:Suppress("MemberVisibilityCanBePrivate")

package org.astronkt.client

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import java.io.OutputStream
import java.net.Socket
import java.nio.ByteBuffer
import java.nio.ByteOrder

class AstronClientNetwork(val coroutineScope: CoroutineScope) {
    private lateinit var socket: Socket
    private lateinit var socketOutputStream: OutputStream

    private val _networkMessages = MutableSharedFlow<AstronClientMessage>()
    val networkMessages: MutableSharedFlow<AstronClientMessage> = _networkMessages

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

                    _networkMessages.emit(AstronClientMessage.fromBytes(data))
                }
            }
        }
    }

    fun sendMessage(msg: AstronClientMessage) {
        socketOutputStream.write(msg.toBytes())
        socketOutputStream.flush()
    }
}