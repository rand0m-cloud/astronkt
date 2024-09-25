package org.astronkt.dclassmacro

import org.astronkt.FieldValue

fun FieldValue.Type.toTypeCode(): String =
    when (this) {
        FieldValue.Type.Int8 -> "FieldValue.Type.Int8"
        FieldValue.Type.Int16 -> "FieldValue.Type.Int16"
        FieldValue.Type.Int32 -> "FieldValue.Type.Int32"
        FieldValue.Type.Int64 -> "FieldValue.Type.Int64"
        FieldValue.Type.UInt8 -> "FieldValue.Type.UInt8"
        FieldValue.Type.UInt16 -> "FieldValue.Type.UInt16"
        FieldValue.Type.UInt32 -> "FieldValue.Type.UInt32"
        FieldValue.Type.UInt64 -> "FieldValue.Type.UInt64"

        FieldValue.Type.Blob -> "FieldValue.Type.Blob"
        FieldValue.Type.Char -> "FieldValue.Type.Char"
        FieldValue.Type.Float64 -> "FieldValue.Type.Float64"
        FieldValue.Type.String -> "FieldValue.Type.String"

        is FieldValue.Type.Tuple -> {
            StringBuilder().apply {
                append("FieldValue.Type.Tuple(")
                for (type in types) {
                    append("${type.toTypeCode()}, ")
                }
                append(")")
            }.toString()
        }

        is FieldValue.Type.Array -> "FieldValue.Type.Array(${type.toTypeCode()})"
        FieldValue.Type.Empty -> "FieldValue.Type.Empty"
    }

fun FieldValue.Type.toKotlinType(): String =
    when (this) {
        is FieldValue.Type.UInt8 -> "UByte"
        is FieldValue.Type.UInt16 -> "UShort"
        is FieldValue.Type.UInt32 -> "UInt"
        is FieldValue.Type.UInt64 -> "ULong"

        is FieldValue.Type.Int8 -> "Byte"
        is FieldValue.Type.Int16 -> "Short"
        is FieldValue.Type.Int32 -> "Int"
        is FieldValue.Type.Int64 -> "Long"

        is FieldValue.Type.String -> "String"
        is FieldValue.Type.Array -> "List<${type.toKotlinType()}>"
        is FieldValue.Type.Tuple -> error("tuples are not a simple primitive")
        FieldValue.Type.Blob -> "ByteArray"
        FieldValue.Type.Char -> "Char"
        FieldValue.Type.Empty -> "Unit"
        FieldValue.Type.Float64 -> "Double"
    }

fun DClassFile.DClassFieldType.toUserKotlinType(): String =
    when (this) {
        is DClassFile.DClassFieldType.Array -> "List<${type.toUserKotlinType()}>"
        DClassFile.DClassFieldType.Char -> "Char"
        is DClassFile.DClassFieldType.Float -> "Double"
        is DClassFile.DClassFieldType.Int -> {
            if (transform == null && range == null) {
                type.toFieldValueType().toKotlinType()
            } else {
                "Double"
            }
        }

        is DClassFile.DClassFieldType.Sized -> type.toFieldValueType().toKotlinType()
        is DClassFile.DClassFieldType.User -> type.name
    }

fun DClassFile.DClassFieldType.toDestructureCodePrimitive(index: DClassFileIndex): String? =
    when (val raw = toRawFieldValueType(index)) {
        is FieldValue.Type.UInt8 -> "toUInt8()!!"
        is FieldValue.Type.UInt16 -> "toUInt16()!!"
        is FieldValue.Type.UInt32 -> "toUInt32()!!"
        is FieldValue.Type.UInt64 -> "toUInt64()!!"
        is FieldValue.Type.Int8 -> "toInt8()!!"
        is FieldValue.Type.Int16 -> "toInt16()!!"
        is FieldValue.Type.Int32 -> "toInt32()!!"
        is FieldValue.Type.Int64 -> "toInt64()!!"
        is FieldValue.Type.Float64 -> "toFloat64()!!"
        is FieldValue.Type.String -> "toStringValue()!!"
        is FieldValue.Type.Array -> "toList()!!"

        FieldValue.Type.Blob -> "toBlob()!!"
        FieldValue.Type.Char -> "toChar()!!"
        FieldValue.Type.Empty -> "toEmpty()!!"
        is FieldValue.Type.Tuple -> error("tuples are not a simple primitive")
    }

fun DClassFile.DClassParameter.IntParameter.IntTransform.toTransformCodeBody(prependIndent: String): String {
    val transforms = mutableListOf<String>()
    var transform: DClassFile.DClassParameter.IntParameter.IntTransform? = this

    while (transform != null) {
        val float = transform.literal.value.toFloat()
        transforms +=
            when (transform.operator) {
                DClassFile.Operator.Divide -> "${prependIndent}divide(${float}f)"
                DClassFile.Operator.Minus -> "${prependIndent}minus(${float}f)"
                DClassFile.Operator.Modulo -> "${prependIndent}modulo(${float}f)"
                DClassFile.Operator.Multiply -> "${prependIndent}multiply(${float}f)"
                DClassFile.Operator.Plus -> "${prependIndent}plus(${float}f)"
            }
        transform = transform.next
    }
    return transforms.asReversed().joinToString("\n")
}

fun DClassFile.DClassParameter.FloatParameter.FloatTransform.toTransformCodeBody(prependIndent: String): String {
    val transforms = mutableListOf<String>()
    var transform: DClassFile.DClassParameter.FloatParameter.FloatTransform? = this

    while (transform != null) {
        val float = transform.literal.toDouble().toFloat()
        transforms +=
            when (transform.operator) {
                DClassFile.Operator.Divide -> "${prependIndent}divide(${float}f)"
                DClassFile.Operator.Minus -> "${prependIndent}minus(${float}f)"
                DClassFile.Operator.Modulo -> "${prependIndent}modulo(${float}f)"
                DClassFile.Operator.Multiply -> "${prependIndent}multiply(${float}f)"
                DClassFile.Operator.Plus -> "${prependIndent}plus(${float}f)"
            }
        transform = transform.next
    }
    return transforms.asReversed().joinToString("\n")
}

fun DClassFile.DClassFieldType.needsTransform(): Boolean =
    when (this) {
        is DClassFile.DClassFieldType.Float -> {
            transform != null || range != null
        }

        is DClassFile.DClassFieldType.Int -> {
            transform != null || range != null
        }

        else -> false
    }

fun DClassFile.DClassFieldType.userDefinedType(): String? = when (this) {
    is DClassFile.DClassFieldType.Array -> type.userDefinedType()
    is DClassFile.DClassFieldType.User -> type.name
    else -> null
}

fun DClassFile.DClassFieldType.arrayType(): DClassFile.DClassFieldType? = when (this) {
    is DClassFile.DClassFieldType.Array -> type
    else -> null
}