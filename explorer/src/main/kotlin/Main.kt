package org.astronkt.explorer

import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.astronkt.*
import org.astronkt.internal.AstronInternalMessage
import org.astronkt.internal.AstronInternalRepositoryConfig

object Locking {
    val outLock = Mutex()

    inline fun withLock(crossinline block: () -> Unit) {
        runBlocking {
            outLock.withLock {
                block()
            }
        }
    }

    fun print(any: Any?) {
        withLock {
            kotlin.io.print(any)
            System.out.flush()
        }
    }

    fun println(any: Any?) {
        withLock {
            kotlin.io.println(any)
        }
    }
}

suspend fun main() =
    supervisorScope {
        setupAstronInternalRepository(
            ClassSpecRepository.build {
            },
            AstronInternalRepositoryConfig(
                "localhost:7199",
                10000U.toChannelId(),
                4002U.toChannelId(),
            ),
        )

        internalRepository.launch()

        launch {
            internalRepository.astronInternalNetwork.networkMessages.collect {
                Locking.println(it.toString())
            }
        }

        var wantsExit = false
        while (!wantsExit) {
            Locking.print("> ")
            val input = readln()
            if (input.startsWith("q")) {
                wantsExit = true
                continue
            }

            if (input.startsWith("c")) {
                MessageBuilder().runCatching {
                    buildMessage()
                }.fold({
                    if (it == null) {
                        wantsExit = true
                        return@fold
                    }
                    internalRepository.astronInternalNetwork.sendMessage(
                        AstronInternalMessage(
                            it.recipients,
                            internalRepository.config.repoControlId,
                            it.msgType!!,
                            it.data.toBytes(),
                        ),
                    )
                }, { err ->
                    Locking.withLock {
                        err.printStackTrace(System.out)
                    }
                })
            }
        }
    }

class MessageBuilder(
    val recipients: MutableList<ChannelId> = mutableListOf(),
    var msgType: UShort? = null,
    val data: MutableList<FieldValue> = mutableListOf(),
) {
    fun buildMessage(): MessageBuilder? {
        Locking.print("msgtype: ")
        val resp = readln()
        if (resp == "q") return null

        val msgSpec = ProtocolMessageRepository.byMsgType[resp.toUShort()]
        if (msgSpec == null) {
            Locking.println("unknown msgtype")
            return null
        }
        msgType = resp.toUShort()

        Locking.println(msgSpec.name)

        if (!msgSpec.isControl) {
            while (true) {
                Locking.print("recipients: ")
                @Suppress("NAME_SHADOWING")
                val resp = readln()
                if (resp == "q") return null
                if (resp.isEmpty() && recipients.isNotEmpty()) break

                val channelId = resp.toULong().toChannelId()
                recipients.add(channelId)
                if (channelId == ChannelId.CONTROL) break
            }
        } else {
            recipients.add(ChannelId.CONTROL)
        }

        for (arg in msgSpec.args) {
            buildFieldValue(arg.name, arg.type) ?: return null
        }

        return this
    }

    private fun buildFieldValueTuple(
        name: String,
        type: FieldValue.Type.Tuple,
    ): MessageBuilder? {
        for (ty in type.types) {
            buildFieldValue(name, ty) ?: return null
        }
        return this
    }

    private fun buildFieldValue(
        name: String,
        type: FieldValue.Type,
    ): MessageBuilder? {
        while (true) {
            val (prompt, reader) =
                when (type) {
                    FieldValue.Type.UInt8 -> "$name (uint8): " to { i: String -> i.toUByte().toFieldValue() }
                    FieldValue.Type.UInt16 -> "$name (uint16): " to { i: String -> i.toUShort().toFieldValue() }
                    FieldValue.Type.UInt32 -> "$name (uint32): " to { i: String -> i.toUInt().toFieldValue() }
                    FieldValue.Type.UInt64 -> "$name (uint64): " to { i: String -> i.toULong().toFieldValue() }
                    FieldValue.Type.String -> "$name (string): " to { i: String -> i.toFieldValue() }
                    FieldValue.Type.Char -> "$name (char): " to { i: String -> i[0].toFieldValue() }
                    FieldValue.Type.Float64 -> "$name (float64): " to { i: String -> i.toDouble().toFieldValue() }
                    FieldValue.Type.Int16 -> "$name (int16): " to { i: String -> i.toShort().toFieldValue() }
                    FieldValue.Type.Int32 -> "$name (int32): " to { i: String -> i.toInt().toFieldValue() }
                    FieldValue.Type.Int64 -> "$name (int64): " to { i: String -> i.toLong().toFieldValue() }
                    FieldValue.Type.Int8 -> "$name (int8): " to { i: String -> i.toByte().toFieldValue() }
                    FieldValue.Type.Blob -> return buildUnknownData()
                    is FieldValue.Type.Array -> return buildUnknownData()
                    is FieldValue.Type.Tuple -> return buildFieldValueTuple(name, type)
                    FieldValue.Type.Empty -> TODO()
                }

            Locking.print(prompt)
            val input = readln()
            val value =
                runCatching {
                    reader(input)
                }.getOrNull() ?: continue

            data.add(value)
            return this
        }
    }

    private fun buildUnknownData(): MessageBuilder? {
        while (true) {
            Locking.println("x/u/U/UL/S (byte, short, int, long, string): ")
            val dataType = readln()
            if (dataType.isEmpty()) break
            if (dataType == "q") return null
            if (!(
                        dataType.startsWith("UL") ||
                                dataType.startsWith("U") ||
                                dataType.startsWith("u") ||
                                dataType.startsWith("S") ||
                                dataType.startsWith("x")
                        )
            ) {
                continue
            }

            Locking.println("value: ")
            val resp = readln()

            if (dataType.startsWith("u")) {
                data.add(resp.toUShort().toFieldValue())
            } else if (dataType.startsWith("UL")) {
                data.add(resp.toULong().toFieldValue())
            } else if (dataType.startsWith("U")) {
                data.add(resp.toUInt().toFieldValue())
            } else if (dataType.startsWith("S")) {
                data.add(resp.toFieldValue())
            } else if (dataType.startsWith("x")) {
                data.add(resp.toUByte().toFieldValue())
            }
        }

        return this
    }
}
