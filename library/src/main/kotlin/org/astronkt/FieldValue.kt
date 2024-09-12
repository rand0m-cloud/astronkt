package org.astronkt

import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.Charset

sealed class FieldValue {
    data class UInt64Value(val value: ULong) : FieldValue()
    data class UInt32Value(val value: UInt) : FieldValue()
    data class UInt16Value(val value: UShort) : FieldValue()
    data class UInt8Value(val value: UByte) : FieldValue()
    data class StringValue(val value: String) : FieldValue()
    data class BlobValue(val value: ByteArray) : FieldValue()
    class TupleValue(vararg val value: FieldValue) : FieldValue()

    sealed class Type {
        companion object {
            val Bool = UInt8
        }

        data object UInt8 : Type() {
            fun read(buf: ByteBuffer): UByte = buf.get().toUByte()
            override fun readValue(buf: ByteBuffer): FieldValue = read(buf).toFieldValue()
        }

        data object UInt16 : Type() {
            fun read(buf: ByteBuffer): UShort = buf.getShort().toUShort()
            override fun readValue(buf: ByteBuffer): FieldValue = read(buf).toFieldValue()
        }

        data object UInt32 : Type() {
            fun read(buf: ByteBuffer): UInt = buf.getInt().toUInt()
            override fun readValue(buf: ByteBuffer): FieldValue = read(buf).toFieldValue()
        }

        data object UInt64 : Type() {
            fun read(buf: ByteBuffer): ULong = buf.getLong().toULong()
            override fun readValue(buf: ByteBuffer): FieldValue = read(buf).toFieldValue()
        }

        data object String : Type() {
            fun read(buf: ByteBuffer): kotlin.String {
                val len = buf.getShort()
                val data = ByteArray(len.toInt())
                buf.get(data)
                return data.toString(Charset.forName("ASCII"))
            }

            override fun readValue(buf: ByteBuffer): FieldValue = read(buf).toFieldValue()
        }

        data object Blob : Type() {
            fun read(buf: ByteBuffer): ByteArray {
                val len = buf.getShort()
                val data = ByteArray(len.toInt()).also { buf.get(it) }
                return data
            }

            override fun readValue(buf: ByteBuffer): FieldValue = BlobValue(read(buf))
        }

        class Tuple(vararg val types: Type) : Type() {
            override fun readValue(buf: ByteBuffer): FieldValue = TupleValue(
                *types.map { it.readValue(buf) }.toTypedArray()
            )
        }

        abstract fun readValue(buf: ByteBuffer): FieldValue
    }

    fun toBytes(): ByteArray {
        return ByteArrayOutputStream().apply {
            when (this@FieldValue) {
                is BlobValue -> write(value)
                is StringValue -> {
                    val data = value.toByteArray(Charset.forName("ASCII"))
                    write(UInt16Value(data.size.toUShort()).toBytes())
                    write(data)
                }

                is TupleValue -> value.map { write(it.toBytes()) }
                is UInt64Value -> write(
                    ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN).putLong(value.toLong()).array()
                )

                is UInt32Value -> write(
                    ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(value.toInt()).array()
                )

                is UInt16Value -> write(
                    ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort(value.toShort()).array()
                )

                is UInt8Value -> write(arrayOf(value.toByte()).toByteArray())
            }
        }.toByteArray()
    }

    fun type(): Type = when (this) {
        is BlobValue -> Type.Blob
        is UInt64Value -> Type.UInt64
        is UInt32Value -> Type.UInt32
        is UInt16Value -> Type.UInt16
        is UInt8Value -> Type.UInt8
        is StringValue -> Type.String
        is TupleValue -> Type.Tuple(
            *value.map {
                type()
            }.toTypedArray()
        )
    }

    companion object {
        internal fun fromBytes(type: Type, bytes: ByteBuffer): FieldValue {
            return when (type) {
                is Type.UInt64 -> bytes.getLong().toULong().toFieldValue()
                is Type.UInt32 -> bytes.getInt().toUInt().toFieldValue()
                is Type.UInt16 -> bytes.getShort().toUShort().toFieldValue()
                is Type.UInt8 -> bytes.get().toUByte().toFieldValue()
                is Type.String -> {
                    val len = bytes.getShort()
                    val data = ByteArray(len.toInt())
                    bytes.get(data)
                    Charsets.US_ASCII.decode(ByteBuffer.wrap(data)).toString().toFieldValue()
                }

                is Type.Blob -> {
                    val len = bytes.getShort()
                    val data = ByteArray(len.toInt())
                    bytes.get(data)
                    BlobValue(data)
                }

                is Type.Tuple -> {
                    TupleValue(
                        *type.types.map {
                            fromBytes(it, bytes)
                        }.toTypedArray()
                    )
                }
            }
        }
    }

    fun toUInt64(): ULong? = when (this) {
        is UInt64Value -> value
        else -> null
    }

    fun toUInt32(): UInt? = when (this) {
        is UInt32Value -> value
        else -> null
    }

    fun toUInt16(): UShort? = when (this) {
        is UInt16Value -> value
        else -> null
    }

    fun toUInt8(): UByte? = when (this) {
        is UInt8Value -> value
        else -> null
    }

    fun toStringValue(): String? = when (this) {
        is StringValue -> value
        else -> null
    }

    fun toBlob(): ByteArray? = when (this) {
        is BlobValue -> value
        else -> null
    }

    fun toTuple(): List<FieldValue>? = when (this) {
        is TupleValue -> value.toList()
        else -> null
    }
}

fun ULong.toFieldValue(): FieldValue = FieldValue.UInt64Value(this)
fun UInt.toFieldValue(): FieldValue = FieldValue.UInt32Value(this)
fun UShort.toFieldValue(): FieldValue = FieldValue.UInt16Value(this)
fun UByte.toFieldValue(): FieldValue = FieldValue.UInt8Value(this)
fun String.toFieldValue(): FieldValue = FieldValue.StringValue(this)
fun List<FieldValue>.toBytes(): ByteArray = fold(ByteArrayOutputStream(), { out, value ->
    out.apply {
        write(value.toBytes())
    }
}).toByteArray()