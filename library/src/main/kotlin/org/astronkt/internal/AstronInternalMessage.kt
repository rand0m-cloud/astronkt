package org.astronkt.internal

import Utils.org.astronkt.getRemaining
import org.astronkt.*
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

data class AstronInternalMessage(
    val recipients: List<ChannelId>,
    val sender: ChannelId,
    val msgType: UShort,
    val msgData: ByteArray
) {
    companion object {
        fun controlMessage(msgType: UShort, msgData: ByteArray) =
            AstronInternalMessage(listOf(ChannelId.CONTROL), ChannelId.ZERO, msgType, msgData)

        fun fromBytes(bytes: ByteArray): AstronInternalMessage {
            val buf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)

            val recipients = mutableListOf<ChannelId>()
            val recipientCount = buf.get()
            for (i in 0..<recipientCount) {
                recipients.add(buf.getLong().toChannelId())
            }
            val sender = buf.getLong().toChannelId()
            val msgType = buf.getShort().toUShort()

            return AstronInternalMessage(
                recipients,
                sender,
                msgType,
                buf.getRemaining()
            )
        }
    }

    val isControl: Boolean
        get() = recipients[0] == ChannelId.CONTROL

    fun toBytes(): ByteArray {
        val payload = ByteArrayOutputStream().apply {
            write(recipients.size.toUByte().toFieldValue().toBytes())
            for (recipient in recipients) {
                write(recipient.id.toFieldValue().toBytes())
            }

            if (!isControl) {
                write(sender.id.toFieldValue().toBytes())
            }

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

    override fun toString(): String {
        val inner = StringBuilder().apply {
            val spec = ProtocolMessageRepository.byMsgType[msgType] ?: return@apply

            val buf = ByteBuffer.wrap(msgData).order(ByteOrder.LITTLE_ENDIAN)

            append("\t${spec.name}\n")
            for (arg in spec.args) {
                if (arg is ProtocolMessageArgumentSpec.Dynamic) {
                    val data = buf.getRemaining().asUByteArray()
                    append("\tDYNAMIC = $data\n")
                    break
                } else {
                    append("\t${arg.name} = ${arg.type.readValue(buf)}\n")
                }
            }
        }

        return "AstronInternalMessage(recipients=$recipients, sender=$sender, msgType=$msgType) {\n$inner}"
    }

}