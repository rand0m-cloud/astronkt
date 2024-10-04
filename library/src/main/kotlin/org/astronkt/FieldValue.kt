package org.astronkt

import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.Charset

sealed class FieldValue {
    sealed interface NumberType

    data class UInt64Value(val value: ULong) : FieldValue()

    data class Int64Value(val value: Long) : FieldValue()

    data class UInt32Value(val value: UInt) : FieldValue()

    data class Int32Value(val value: Int) : FieldValue()

    data class UInt16Value(val value: UShort) : FieldValue()

    data class Int16Value(val value: Short) : FieldValue()

    data class UInt8Value(val value: UByte) : FieldValue()

    data class Int8Value(val value: Byte) : FieldValue()

    data class CharValue(val value: UByte) : FieldValue() {
        override fun toString(): String = "CharValue($value '${value.toInt().toChar()}')"
    }

    data class Float64Value(val value: Double) : FieldValue()

    data class StringValue(val value: String) : FieldValue()

    data class BlobValue(val value: ByteArray) : FieldValue()

    data class ArrayValue(val type: Type, val values: List<FieldValue>) : FieldValue()

    data object EmptyValue : FieldValue()

    class TupleValue(vararg val value: FieldValue) : FieldValue() {
        override fun toString(): String = value.toList().toString()
    }

    sealed class Type {
        abstract fun readValue(buf: ByteBuffer): FieldValue

        data object UInt8 : Type() {
            fun read(buf: ByteBuffer): UByte = buf.get().toUByte()

            override fun readValue(buf: ByteBuffer): FieldValue = read(buf).toFieldValue()
        }

        data object Int8 : Type() {
            fun read(buf: ByteBuffer): Byte = buf.get()

            override fun readValue(buf: ByteBuffer): FieldValue = read(buf).toFieldValue()
        }

        data object UInt16 : Type() {
            fun read(buf: ByteBuffer): UShort = buf.getShort().toUShort()

            override fun readValue(buf: ByteBuffer): FieldValue = read(buf).toFieldValue()
        }

        data object Int16 : Type() {
            fun read(buf: ByteBuffer): Short = buf.getShort()

            override fun readValue(buf: ByteBuffer): FieldValue = read(buf).toFieldValue()
        }

        data object UInt32 : Type() {
            fun read(buf: ByteBuffer): UInt = buf.getInt().toUInt()

            override fun readValue(buf: ByteBuffer): FieldValue = read(buf).toFieldValue()
        }

        data object Int32 : Type() {
            fun read(buf: ByteBuffer): Int = buf.getInt()

            override fun readValue(buf: ByteBuffer): FieldValue = read(buf).toFieldValue()
        }

        data object UInt64 : Type() {
            fun read(buf: ByteBuffer): ULong = buf.getLong().toULong()

            override fun readValue(buf: ByteBuffer): FieldValue = read(buf).toFieldValue()
        }

        data object Int64 : Type() {
            fun read(buf: ByteBuffer): Long = buf.getLong()

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
            override fun readValue(buf: ByteBuffer): FieldValue =
                TupleValue(
                    *types.map { it.readValue(buf) }.toTypedArray(),
                )

            override fun toString(): kotlin.String = "Tuple(${types.toList()})"
        }

        class Array(val type: Type, val sized: UInt? = null) : Type() {
            fun read(buf: ByteBuffer): List<FieldValue> {
                val list = mutableListOf<FieldValue>()

                if (sized == null) {
                    val bytes = buf.getShort()
                    val data = ByteArray(bytes.toInt()).also { buf.get(it) }
                    val dataBuf = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN)

                    while (dataBuf.hasRemaining()) {
                        list += type.readValue(dataBuf)
                    }
                } else {
                    for (i in 0..<sized.toInt()) {
                        list += type.readValue(buf)
                    }
                }

                return list
            }

            override fun readValue(buf: ByteBuffer): FieldValue = ArrayValue(type, read(buf))
        }

        data object Char : Type() {
            fun read(buf: ByteBuffer): UByte = buf.get().toUByte()

            override fun readValue(buf: ByteBuffer): FieldValue = CharValue(read(buf))
        }

        data object Float64 : Type() {
            fun read(buf: ByteBuffer): Double = buf.getDouble()

            override fun readValue(buf: ByteBuffer): FieldValue = read(buf).toFieldValue()
        }

        data object Empty : Type() {
            override fun readValue(buf: ByteBuffer): FieldValue = EmptyValue
        }

        fun compositeType(): Boolean =
            when (this) {
                is Tuple -> true
                else -> false
            }
    }

    fun toBytes(): ByteArray {
        return ByteArrayOutputStream().apply {
            when (this@FieldValue) {
                is BlobValue -> {
                    write(UInt16Value(value.size.toUShort()).toBytes())
                    write(value)
                }

                is StringValue -> {
                    val data = value.toByteArray(Charset.forName("ASCII"))
                    write(UInt16Value(data.size.toUShort()).toBytes())
                    write(data)
                }

                is ArrayValue -> {
                    write(UInt16Value(values.size.toUShort()).toBytes())
                    values.forEach { write(it.toBytes()) }
                }

                is TupleValue -> value.map { write(it.toBytes()) }
                is CharValue -> write(byteArrayOf(value.toByte()))

                is UInt64Value ->
                    write(
                        ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN).putLong(value.toLong()).array(),
                    )

                is Int64Value ->
                    write(
                        ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN).putLong(value).array(),
                    )

                is UInt32Value ->
                    write(
                        ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(value.toInt()).array(),
                    )

                is Int32Value ->
                    write(
                        ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(value).array(),
                    )

                is UInt16Value ->
                    write(
                        ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort(value.toShort()).array(),
                    )

                is Int16Value ->
                    write(
                        ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort(value).array(),
                    )

                is UInt8Value -> write(arrayOf(value.toByte()).toByteArray())
                is Int8Value -> write(arrayOf(value).toByteArray())

                is Float64Value -> write(ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN).putDouble(value).array())
                EmptyValue -> {}
            }
        }.toByteArray()
    }

    fun type(): Type =
        when (this) {
            is UInt64Value -> Type.UInt64
            is Int64Value -> Type.Int64

            is UInt32Value -> Type.UInt32
            is Int32Value -> Type.Int32

            is UInt16Value -> Type.UInt16
            is Int16Value -> Type.Int16

            is UInt8Value -> Type.UInt8
            is Int8Value -> Type.Int8

            is StringValue -> Type.String
            is BlobValue -> Type.Blob
            is ArrayValue -> Type.Array(type)
            is CharValue -> Type.Char
            is Float64Value -> Type.Float64
            is TupleValue ->
                Type.Tuple(
                    *value.map {
                        type()
                    }.toTypedArray(),
                )

            EmptyValue -> Type.Empty
        }

    companion object {
        internal fun fromBytes(
            type: Type,
            bytes: ByteBuffer,
        ): FieldValue {
            return when (type) {
                is Type.UInt64 -> bytes.getLong().toULong().toFieldValue()
                is Type.Int64 -> bytes.getLong().toFieldValue()

                is Type.UInt32 -> bytes.getInt().toUInt().toFieldValue()
                is Type.Int32 -> bytes.getInt().toFieldValue()

                is Type.UInt16 -> bytes.getShort().toUShort().toFieldValue()
                is Type.Int16 -> bytes.getShort().toFieldValue()

                is Type.UInt8 -> bytes.get().toUByte().toFieldValue()
                is Type.Int8 -> bytes.get().toFieldValue()

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

                is Type.Array -> {
                    val len = bytes.getShort()
                    ArrayValue(type.type, type.read(bytes))
                }

                is Type.Tuple -> {
                    TupleValue(
                        *type.types.map {
                            fromBytes(it, bytes)
                        }.toTypedArray(),
                    )
                }

                is Type.Char -> {
                    type.read(bytes).toFieldValue()
                }

                is Type.Float64 -> {
                    type.read(bytes).toFieldValue()
                }

                Type.Empty -> EmptyValue
            }
        }
    }

    fun toUInt64(): ULong? =
        when (this) {
            is UInt64Value -> value
            else -> null
        }

    fun toUInt32(): UInt? =
        when (this) {
            is UInt32Value -> value
            else -> null
        }

    fun toUInt16(): UShort? =
        when (this) {
            is UInt16Value -> value
            else -> null
        }

    fun toUInt8(): UByte? =
        when (this) {
            is UInt8Value -> value
            else -> null
        }

    fun toInt64(): Long? =
        when (this) {
            is Int64Value -> value
            else -> null
        }

    fun toInt32(): Int? =
        when (this) {
            is Int32Value -> value
            else -> null
        }

    fun toInt16(): Short? =
        when (this) {
            is Int16Value -> value
            else -> null
        }

    fun toInt8(): Byte? =
        when (this) {
            is Int8Value -> value
            else -> null
        }

    fun toChar(): Char? =
        when (this) {
            is CharValue -> value.toInt().toChar()
            else -> null
        }

    fun toStringValue(): String? =
        when (this) {
            is StringValue -> value
            else -> null
        }

    fun toDouble(): Double? =
        when (this) {
            is Float64Value -> value
            else -> null
        }

    fun toBlob(): ByteArray? =
        when (this) {
            is BlobValue -> value
            else -> null
        }

    fun toTuple(): List<FieldValue>? =
        when (this) {
            is TupleValue -> value.toList()
            else -> null
        }

    fun toList(): List<FieldValue>? =
        when (this) {
            is ArrayValue -> values
            else -> null
        }

    fun <T : ToFieldValue<T>> toArray(t: T): List<T>? =
        when (this) {
            is ArrayValue -> with(t) { values.map { fromFieldValue(it) } }
            else -> null
        }

    fun toArray(t: ULong.Companion): List<ULong>? =
        when (this) {
            is ArrayValue -> values.map { toUInt64()!! }
            else -> null
        }

    fun toArray(t: UInt.Companion): List<UInt>? =
        when (this) {
            is ArrayValue -> values.map { toUInt32()!! }
            else -> null
        }

    fun toArray(t: UShort.Companion): List<UShort>? =
        when (this) {
            is ArrayValue -> values.map { toUInt16()!! }
            else -> null
        }

    fun toArray(t: UByte.Companion): List<UByte>? =
        when (this) {
            is ArrayValue -> values.map { toUInt8()!! }
            else -> null
        }

    fun toArray(t: Long.Companion): List<Long>? =
        when (this) {
            is ArrayValue -> values.map { toInt64()!! }
            else -> null
        }

    fun toArray(t: Int.Companion): List<Int>? =
        when (this) {
            is ArrayValue -> values.map { toInt32()!! }
            else -> null
        }

    fun toArray(t: Short.Companion): List<Short>? =
        when (this) {
            is ArrayValue -> values.map { toInt16()!! }
            else -> null
        }

    fun toArray(t: Byte.Companion): List<Byte>? =
        when (this) {
            is ArrayValue -> values.map { toInt8()!! }
            else -> null
        }

    fun toArray(t: String.Companion): List<String>? =
        when (this) {
            is ArrayValue -> values.map { toStringValue()!! }
            else -> null
        }
}

fun ULong.toFieldValue(): FieldValue = FieldValue.UInt64Value(this)

fun Long.toFieldValue(): FieldValue = FieldValue.Int64Value(this)

fun UInt.toFieldValue(): FieldValue = FieldValue.UInt32Value(this)

fun Int.toFieldValue(): FieldValue = FieldValue.Int32Value(this)

fun UShort.toFieldValue(): FieldValue = FieldValue.UInt16Value(this)

fun Short.toFieldValue(): FieldValue = FieldValue.Int16Value(this)

fun UByte.toFieldValue(): FieldValue = FieldValue.UInt8Value(this)

fun Byte.toFieldValue(): FieldValue = FieldValue.Int8Value(this)

fun Char.toFieldValue(): FieldValue = FieldValue.CharValue(code.toUByte())

fun Double.toFieldValue(): FieldValue = FieldValue.Float64Value(this)

fun String.toFieldValue(): FieldValue = FieldValue.StringValue(this)

fun ByteArray.toFieldValue(): FieldValue = FieldValue.BlobValue(this)

fun FieldValue.toFieldValue(): FieldValue = this

fun List<FieldValue>.toFieldValue(): FieldValue = error("todo")

fun List<FieldValue>.toBytes(): ByteArray =
    fold(ByteArrayOutputStream()) { out, value ->
        out.apply {
            write(value.toBytes())
        }
    }.toByteArray()

interface ToFieldValue<T> {
    val type: FieldValue.Type

    fun fromFieldValue(value: FieldValue): T

    fun T.toFieldValue(): FieldValue

    fun List<T>.toFieldValue(): FieldValue = FieldValue.ArrayValue(type, this.map { it.toFieldValue() })

    fun fromFieldValue(value: List<FieldValue>): List<T> = value.map { fromFieldValue(it) }
}
