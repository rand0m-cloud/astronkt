package org.astronkt

interface TransformScope {
    fun divide(divisor: Float)

    fun modulo(modulus: Float)
}

fun Double.unTransform(
    type: FieldValue.Type,
    block: TransformScope.() -> Unit,
): FieldValue {
    var value = this
    object : TransformScope {
        override fun divide(divisor: Float) {
            value *= divisor
        }

        override fun modulo(modulus: Float) {
            value %= modulus
        }
    }.block()

    return when (type) {
        FieldValue.Type.Float64 -> value.toFieldValue()
        FieldValue.Type.Int8 -> value.toInt().toByte().toFieldValue()
        FieldValue.Type.Int16 -> value.toInt().toShort().toFieldValue()
        FieldValue.Type.Int32 -> value.toInt().toFieldValue()
        FieldValue.Type.Int64 -> value.toLong().toFieldValue()
        FieldValue.Type.UInt8 -> value.toUInt().toUByte().toFieldValue()
        FieldValue.Type.UInt16 -> value.toUInt().toUShort().toFieldValue()
        FieldValue.Type.UInt32 -> value.toUInt().toFieldValue()
        FieldValue.Type.UInt64 -> value.toULong().toFieldValue()
        else -> throw Throwable("cannot untransform to $type")
    }
}

fun UByte.transform(block: TransformScope.() -> Unit) = toULong().transform(block)
fun UShort.transform(block: TransformScope.() -> Unit) = toULong().transform(block)
fun UInt.transform(block: TransformScope.() -> Unit) = toULong().transform(block)
fun ULong.transform(block: TransformScope.() -> Unit): Double {
    var value = toDouble()
    object : TransformScope {
        override fun divide(divisor: Float) {
            value /= divisor
        }

        override fun modulo(modulus: Float) {
            value %= modulus
        }
    }.block()
    return value
}

fun Byte.transform(block: TransformScope.() -> Unit) = toLong().transform(block)
fun Short.transform(block: TransformScope.() -> Unit) = toLong().transform(block)
fun Int.transform(block: TransformScope.() -> Unit) = toLong().transform(block)
fun Long.transform(block: TransformScope.() -> Unit): Double {
    var value = toDouble()
    object : TransformScope {
        override fun divide(divisor: Float) {
            value /= divisor
        }

        override fun modulo(modulus: Float) {
            value %= modulus
        }
    }.block()

    return value
}