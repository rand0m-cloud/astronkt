package org.astronkt.dclassmacro

import org.astronkt.*

data class DClassFile(val decls: List<TypeDecl>) {
    sealed class TypeDecl {
        data class DClass(val name: String, val parents: List<String>, val fields: List<DClassField>) : TypeDecl()
        data class TypeDef(val type: DClassType, val newTypeName: String) : TypeDecl()
        data class Struct(val name: String, val parameters: List<DClassParameter>) : TypeDecl()
    }

    sealed class DClassField {
        data class ParameterField(
            val parameter: DClassParameter,
            val modifiers: List<DClassFieldModifier>
        ) : DClassField()

        data class AtomicField(
            val name: String,
            val parameters: List<DClassParameter>,
            val modifiers: List<DClassFieldModifier>
        ) : DClassField()

        data class MolecularField(
            val name: String,
            val fields: List<String>
        ) : DClassField()
    }

    sealed class DClassFieldModifier {
        data object Required : DClassFieldModifier()
        data object Broadcast : DClassFieldModifier()
        data object ClSend : DClassFieldModifier()
        data object ClRecv : DClassFieldModifier()
        data object Ram : DClassFieldModifier()
        data object Db : DClassFieldModifier()
        data object AiRecv : DClassFieldModifier()
        data object OwnSend : DClassFieldModifier()
        data object OwnRecv : DClassFieldModifier()
    }

    sealed class DClassParameter {
        data class IntParameter(
            val type: DClassFieldType.IntType,
            val range: IntRange?,
            val transform: IntTransform?,
            val name: String?,
            val default: IntConstant?
        ) : DClassParameter() {
            data class IntRange(val from: Literal.IntLiteral, val to: Literal.IntLiteral)
            data class IntTransform(val operator: Operator, val literal: Literal.IntLiteral, val next: IntTransform?)
            data class IntConstant(val literal: Literal.IntLiteral, val transform: IntTransform?)
        }

        data class FloatParameter(
            val type: DClassFieldType.FloatType,
            val range: FloatRange?,
            val transform: FloatTransform?,
            val name: String?,
            val default: FloatConstant?
        ) : DClassParameter() {
            data class FloatRange(val from: Literal.FloatLiteral, val to: Literal.FloatLiteral)
            data class FloatTransform(
                val operator: Operator,
                val literal: Literal.NumLiteral,
                val next: FloatTransform?
            )

            data class FloatConstant(val literal: Literal.NumLiteral, val transform: FloatTransform?)
        }

        data class CharParameter(
            val type: DClassFieldType.Char,
            val name: String?,
            val default: Literal.CharLiteral?
        ) : DClassParameter()

        data class SizedParameter(
            val type: DClassFieldType.SizedType,
            val constraint: SizeConstraint?,
            val name: String?,
            val default: Literal.SizedLiteral?
        ) : DClassParameter() {
            data class SizeConstraint(val minSize: Literal.IntLiteral, val maxSize: Literal.IntLiteral?)
        }

        data class StructParameter(val struct: String, val name: String?, val default: Literal.IntLiteral?) :
            DClassParameter()

        data class ArrayParameter(
            val type: DClassType,
            val name: String?,
            val range: ArrayRange,
            val default: Literal.ArrayLiteral?
        ) : DClassParameter() {
            sealed class ArrayRange {
                data object Empty : ArrayRange()
                data class Size(val size: Literal.IntLiteral) : ArrayRange()
                data class Range(val from: Literal.IntLiteral, val to: Literal.IntLiteral) : ArrayRange()
            }
        }
    }

    sealed class Operator {
        data object Modulo : Operator()
        data object Plus : Operator()
        data object Minus : Operator()
        data object Multiply : Operator()
        data object Divide : Operator()
    }

    sealed class Literal {
        sealed interface NumLiteral {}
        sealed interface ArrayValueLiteral {}
        sealed interface SizedLiteral {}

        sealed class IntLiteral : Literal(), NumLiteral, ArrayValueLiteral {
            abstract val value: Long

            override fun toString(): String = value.toString()

            data class DecLiteral(override val value: Long) : IntLiteral()
            data class OctLiteral(override val value: Long) : IntLiteral()
            data class BinLiteral(override val value: Long) : IntLiteral()
            data class HexLiteral(override val value: Long) : IntLiteral()

        }

        data class CharLiteral(val value: Byte) : Literal()
        data class StringLiteral(val value: String) : Literal(), SizedLiteral
        data class FloatLiteral(val value: Double) : Literal(), NumLiteral {
            override fun toString(): String = value.toString()
        }

        data class ArrayLiteral(val values: List<ArrayValueLiteral>) : Literal(), SizedLiteral {
            data class ArrayShorthandLiteral(val value: ArrayValueLiteral, val repeated: IntLiteral) :
                ArrayValueLiteral
        }


    }

    sealed class DClassFieldType {
        sealed class IntType : DClassFieldType() {}
        sealed class FloatType : DClassFieldType() {}
        sealed class SizedType : DClassFieldType() {}

        data object UInt64 : IntType()
        data object Int64 : IntType()

        data object UInt32 : IntType()
        data object Int32 : IntType()

        data object UInt16 : IntType()
        data object Int16 : IntType()

        data object UInt8 : IntType()
        data object Int8 : IntType()

        data object String : SizedType()
        data object Blob : SizedType()

        data object Char : DClassFieldType()
        data object Float64 : FloatType()

        data class UserDefined(val name: kotlin.String) : DClassFieldType()

        data class Array(val type: DClassFieldType) : DClassFieldType()
    }

    // TODO: refactor this name and possibly merge into DClassFieldType
    sealed class DClassType {
        data class Int(
            val type: DClassFieldType.IntType,
            val range: DClassParameter.IntParameter.IntRange?,
            val transform: DClassParameter.IntParameter.IntTransform?,
        ) : DClassType()

        data class Float(
            val type: DClassFieldType.FloatType,
            val range: DClassParameter.FloatParameter.FloatRange?,
            val transform: DClassParameter.FloatParameter.FloatTransform?,
        ) : DClassType()

        data class Array(val type: DClassType, val range: DClassParameter.ArrayParameter.ArrayRange) : DClassType()
        data class Other(val type: DClassFieldType) : DClassType()
    }
}

data class DClassFileIndex(
    val dClasses: List<String>,
    val byDClassName: Map<String, Pair<DClassId, DClassFile.TypeDecl.DClass>>,
    val byDClassField: Map<DClassFile.DClassField, FieldId>
) {
    fun getFieldId(field: DClassFile.DClassField): FieldId = byDClassField[field]!!
}

fun DClassFile.buildIndex(): DClassFileIndex {
    var dClassId = 0U
    var fieldId = 0U

    val dClasses = mutableListOf<String>()
    val byDClassName = mutableMapOf<String, Pair<DClassId, DClassFile.TypeDecl.DClass>>()
    val byDClassField = mutableMapOf<DClassFile.DClassField, FieldId>()

    for (decl in decls) {
        when (decl) {
            is DClassFile.TypeDecl.DClass -> {
                dClasses.add(decl.name)
                byDClassName[decl.name] = dClassId.toDClassId() to decl

                for (field in decl.fields) {
                    byDClassField[field] = fieldId.toFieldId()
                    fieldId += 1U
                }

                dClassId += 1U
            }

            else -> {}
        }
    }

    return DClassFileIndex(dClasses, byDClassName, byDClassField)
}

fun DClassFile.DClassParameter.toFieldValueType(): FieldValue.Type {
    return when (this) {
        is DClassFile.DClassParameter.IntParameter -> {
            type.toFieldValueType()
        }

        is DClassFile.DClassParameter.SizedParameter -> {
            type.toFieldValueType()
        }

        is DClassFile.DClassParameter.ArrayParameter -> TODO()
        is DClassFile.DClassParameter.CharParameter -> {
            DClassFile.DClassFieldType.Char.toFieldValueType()
        }

        is DClassFile.DClassParameter.FloatParameter -> {
            type.toFieldValueType()
        }

        is DClassFile.DClassParameter.StructParameter -> {
            TODO()
        }
    }
}

fun DClassFile.DClassField.toFieldValueType(): FieldValue.Type {
    return when (this) {
        is DClassFile.DClassField.ParameterField -> {
            parameter.toFieldValueType()
        }

        is DClassFile.DClassField.AtomicField -> {
            val types = parameters.map { it.toFieldValueType() }
            if (types.size == 1) types[0] else FieldValue.Type.Tuple(*types.toTypedArray())
        }

        is DClassFile.DClassField.MolecularField -> {
            FieldValue.Type.Tuple(*fields.map { toFieldValueType() }.toTypedArray())
        }
    }
}

fun DClassFile.DClassFieldType.toFieldValueType(): FieldValue.Type {
    return when (this) {
        is DClassFile.DClassFieldType.UInt64 -> FieldValue.Type.UInt64
        is DClassFile.DClassFieldType.Int64 -> FieldValue.Type.Int64

        is DClassFile.DClassFieldType.UInt32 -> FieldValue.Type.UInt32
        is DClassFile.DClassFieldType.Int32 -> FieldValue.Type.Int32

        is DClassFile.DClassFieldType.UInt16 -> FieldValue.Type.UInt16
        is DClassFile.DClassFieldType.Int16 -> FieldValue.Type.Int16

        is DClassFile.DClassFieldType.UInt8 -> FieldValue.Type.UInt8
        is DClassFile.DClassFieldType.Int8 -> FieldValue.Type.Int8

        is DClassFile.DClassFieldType.String -> FieldValue.Type.String
        is DClassFile.DClassFieldType.Blob -> FieldValue.Type.String
        is DClassFile.DClassFieldType.Char -> FieldValue.Type.Char
        is DClassFile.DClassFieldType.Float64 -> FieldValue.Type.Float64
        is DClassFile.DClassFieldType.Array -> FieldValue.Type.Array(type.toFieldValueType())
        is DClassFile.DClassFieldType.UserDefined -> TODO()
    }
}

val DClassFile.DClassField.modifiers: List<DClassFile.DClassFieldModifier>
    get() {
        return when (this) {
            is DClassFile.DClassField.AtomicField -> modifiers
            is DClassFile.DClassField.ParameterField -> modifiers
            is DClassFile.DClassField.MolecularField -> listOf()
        }
    }

fun DClassFile.DClassField.toDistributedFieldSpec(): DistributedFieldSpec {
    return DistributedFieldSpec(
        toFieldValueType(),
        modifiers.fold(DistributedFieldModifiers()) { acc, mod ->
            when (mod) {
                is DClassFile.DClassFieldModifier.Required -> acc.copy(required = true)
                is DClassFile.DClassFieldModifier.ClSend -> acc.copy(clsend = true)
                is DClassFile.DClassFieldModifier.Broadcast -> acc.copy(broadcast = true)
                is DClassFile.DClassFieldModifier.Ram -> acc.copy(ram = true)
                is DClassFile.DClassFieldModifier.ClRecv -> acc.copy(clrecv = true)
                is DClassFile.DClassFieldModifier.AiRecv -> acc.copy(airecv = true)
                is DClassFile.DClassFieldModifier.Db -> acc.copy(db = true)
                is DClassFile.DClassFieldModifier.OwnSend -> acc.copy(ownsend = true)
                is DClassFile.DClassFieldModifier.OwnRecv -> acc.copy(ownrecv = true)
            }
        }
    )
}

fun DClassFile.DClassFieldType.toType(): String = when (this) {
    is DClassFile.DClassFieldType.UInt8 -> "uint8"
    is DClassFile.DClassFieldType.Int8 -> "int8"
    is DClassFile.DClassFieldType.UInt16 -> "uint16"
    is DClassFile.DClassFieldType.Int16 -> "int16"
    is DClassFile.DClassFieldType.UInt32 -> "uint32"
    is DClassFile.DClassFieldType.Int32 -> "int32"
    is DClassFile.DClassFieldType.UInt64 -> "uint64"
    is DClassFile.DClassFieldType.Int64 -> "int64"
    is DClassFile.DClassFieldType.Blob -> "blob"
    is DClassFile.DClassFieldType.String -> "string"
    is DClassFile.DClassFieldType.Array -> "${type.toType()}[]"
    is DClassFile.DClassFieldType.Char -> "char"
    is DClassFile.DClassFieldType.Float64 -> "float64"
    is DClassFile.DClassFieldType.UserDefined -> name
}

fun DClassFile.DClassParameter.ArrayParameter.ArrayRange.toType(): String = when (this) {
    is DClassFile.DClassParameter.ArrayParameter.ArrayRange.Empty -> "[]"
    is DClassFile.DClassParameter.ArrayParameter.ArrayRange.Size -> "[$size]"
    is DClassFile.DClassParameter.ArrayParameter.ArrayRange.Range -> "[$from - $to]"

}

fun DClassFile.DClassParameter.IntParameter.IntTransform.toType(): String {
    return "${operator}${literal}${next?.toType() ?: ""}"
}

fun DClassFile.DClassParameter.FloatParameter.FloatTransform.toType(): String {
    return "${operator}${literal}${next?.toType() ?: ""}"
}

fun DClassFile.DClassParameter.SizedParameter.SizeConstraint.toType(): String =
    if (maxSize != null) "($minSize - $maxSize)" else "($minSize)"

fun DClassFile.DClassParameter.toType(): String {
    return when (this) {
        is DClassFile.DClassParameter.IntParameter -> {
            "${type.toType()}${transform?.toType() ?: ""}"
        }

        is DClassFile.DClassParameter.ArrayParameter -> {
            "$type${range.toType()}"
        }

        is DClassFile.DClassParameter.CharParameter -> {
            "char"
        }

        is DClassFile.DClassParameter.FloatParameter -> {
            "${type.toType()}${transform?.toType() ?: ""}"
        }

        is DClassFile.DClassParameter.SizedParameter -> {
            "${type.toType()}${constraint?.toType() ?: ""}"
        }

        is DClassFile.DClassParameter.StructParameter -> {
            struct
        }
    }
}

fun String.toDClassFieldType(): DClassFile.DClassFieldType {
    return when (this) {
        "uint8" -> DClassFile.DClassFieldType.UInt8
        "int8" -> DClassFile.DClassFieldType.Int8
        "uint16" -> DClassFile.DClassFieldType.UInt16
        "int16" -> DClassFile.DClassFieldType.Int16
        "uint32" -> DClassFile.DClassFieldType.UInt32
        "int32" -> DClassFile.DClassFieldType.Int32
        "uint64" -> DClassFile.DClassFieldType.UInt64
        "int64" -> DClassFile.DClassFieldType.Int64
        "float64" -> DClassFile.DClassFieldType.Float64
        "char" -> DClassFile.DClassFieldType.Char
        "string" -> DClassFile.DClassFieldType.String
        "blob" -> DClassFile.DClassFieldType.Blob
        else -> DClassFile.DClassFieldType.UserDefined(this)
    }
}