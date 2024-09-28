package org.astronkt.client

import Utils.org.astronkt.getRemaining
import org.astronkt.ProtocolMessageArgumentSpec
import org.astronkt.ProtocolMessageRepository
import org.astronkt.toFieldValue
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

data class AstronClientMessage(
    val msgType: UShort,
    val msgData: ByteArray
) {
    companion object {
        fun fromBytes(bytes: ByteArray): AstronClientMessage {
            val buf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)

            val msgType = buf.getShort().toUShort()

            return AstronClientMessage(
                msgType,
                buf.getRemaining()
            )
        }
    }

    fun toBytes(): ByteArray {
        val payload = ByteArrayOutputStream().apply {
            write(msgType.toFieldValue().toBytes())
            write(msgData)
        }.toByteArray()
        return ByteArrayOutputStream().apply {
            // length
            write(
                ByteBuffer.allocate(2)
                    .order(ByteOrder.LITTLE_ENDIAN)
                    .putShort(payload.size.toShort())
                    .array()
            )
            // payload
            write(payload)
        }.toByteArray()
    }

    @OptIn(ExperimentalStdlibApi::class)
    override fun toString(): String {
        val inner = StringBuilder().apply {
            val spec = ProtocolMessageRepository.byMsgType[msgType] ?: return@apply

            val buf = ByteBuffer.wrap(msgData).order(ByteOrder.LITTLE_ENDIAN)

            append("\t${spec.name}\n")
            runCatching {
                for (arg in spec.args) {
                    if (arg is ProtocolMessageArgumentSpec.Dynamic) {
                        val data = buf.getRemaining()
                        val dataStr = buildString {
                            append("[")
                            for ((index, i) in data.withIndex()) {
                                if (index != 0) append(", ")
                                append("0x${i.toHexString()}")
                            }
                            append("]")
                        }
                        append("\tDYNAMIC = $dataStr\n")
                        break
                    } else {
                        append("\t${arg.name} = ${arg.type.readValue(buf)}\n")
                    }
                }
            }
        }

        return "AstronClientMessage(msgType=$msgType) {\n$inner}"
    }

}