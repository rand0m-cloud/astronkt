@file:Suppress("ktlint:standard:no-wildcard-imports")

package org.astronkt.dclassmacro

import org.astronkt.*

@Suppress("unused")
data class DClassFile(val decls: List<TypeDecl>) {
    sealed class TypeDecl {
        data class DClass(val name: String, val parents: List<String>, val fields: List<DClassField>) : TypeDecl()

        data class TypeDef(val type: DClassFieldType, val newTypeName: String) : TypeDecl()

        data class Struct(val name: String, val parameters: List<DClassParameter>) : TypeDecl()
    }

    sealed class DClassField {
        abstract val name: String?

        data class ParameterField(
            val parameter: DClassParameter,
            val modifiers: List<DClassFieldModifier>,
        ) : DClassField() {
            override val name: String?
                get() = parameter.name
        }

        data class AtomicField(
            override val name: String,
            val parameters: List<DClassParameter>,
            val modifiers: List<DClassFieldModifier>,
        ) : DClassField()

        data class MolecularField(
            override val name: String,
            val fields: List<String>,
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
        abstract val type: DClassFieldType
        abstract val name: String?

        data class IntParameter(
            override val type: DClassFieldType.Int,
            override val name: String?,
            val default: IntConstant?,
        ) : DClassParameter() {
            data class IntRange(val from: Literal.IntLiteral, val to: Literal.IntLiteral)

            data class IntTransform(val operator: Operator, val literal: Literal.IntLiteral, val next: IntTransform?)

            data class IntConstant(val literal: Literal.IntLiteral, val transform: IntTransform?)
        }

        data class FloatParameter(
            override val type: DClassFieldType.Float,
            override val name: String?,
            val default: FloatConstant?,
        ) : DClassParameter() {
            data class FloatRange(val from: Literal.FloatLiteral, val to: Literal.FloatLiteral)

            data class FloatTransform(
                val operator: Operator,
                val literal: Literal.NumLiteral,
                val next: FloatTransform?,
            )

            data class FloatConstant(val literal: Literal.NumLiteral, val transform: FloatTransform?)
        }

        data class CharParameter(
            override val type: DClassFieldType.Char,
            override val name: String?,
            val default: Literal.CharLiteral?,
        ) : DClassParameter()

        data class SizedParameter(
            override val type: DClassFieldType.Sized,
            override val name: String?,
            val default: Literal.SizedLiteral?,
        ) : DClassParameter()

        data class UserTypeParameter(
            override val type: DClassFieldType.User,
            override val name: String?,
            val default: Literal.IntLiteral?,
        ) : DClassParameter()

        data class ArrayParameter(
            override val type: DClassFieldType.Array,
            override val name: String?,
            val default: Literal.ArrayLiteral?,
        ) : DClassParameter() {
            sealed class ArrayRange {
                fun isExactSize(): UInt? =
                    when (this) {
                        is Size -> size.value.toUInt()
                        else -> null
                    }

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
        sealed interface NumLiteral {
            fun toDouble(): Double
        }

        sealed interface ArrayValueLiteral

        sealed interface SizedLiteral

        sealed class IntLiteral : Literal(), NumLiteral, ArrayValueLiteral {
            abstract val value: Long

            override fun toString(): String = value.toString()

            override fun toDouble(): Double = value.toDouble()

            data class DecLiteral(override val value: Long) : IntLiteral()

            data class OctLiteral(override val value: Long) : IntLiteral()

            data class BinLiteral(override val value: Long) : IntLiteral()

            data class HexLiteral(override val value: Long) : IntLiteral()
        }

        data class CharLiteral(val value: Byte) : Literal()

        data class StringLiteral(val value: String) : Literal(), SizedLiteral

        data class FloatLiteral(val value: Double) : Literal(), NumLiteral {
            override fun toDouble(): Double = value

            override fun toString(): String = value.toString()
        }

        data class ArrayLiteral(val values: List<ArrayValueLiteral>) : Literal(), SizedLiteral {
            data class ArrayShorthandLiteral(val value: ArrayValueLiteral, val repeated: IntLiteral) :
                ArrayValueLiteral
        }
    }

    sealed class DClassRawFieldType {
        sealed class IntType : DClassRawFieldType()

        sealed class FloatType : DClassRawFieldType()

        sealed class SizedType : DClassRawFieldType()

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

        data object Char : DClassRawFieldType()

        data object Float64 : FloatType()

        data class UserType(val name: kotlin.String) : DClassRawFieldType()
    }

    sealed class DClassFieldType {
        data class Int(
            val type: DClassRawFieldType.IntType,
            val range: DClassParameter.IntParameter.IntRange?,
            val transform: DClassParameter.IntParameter.IntTransform?,
        ) : DClassFieldType()

        data class Float(
            val type: DClassRawFieldType.FloatType,
            val range: DClassParameter.FloatParameter.FloatRange?,
            val transform: DClassParameter.FloatParameter.FloatTransform?,
        ) : DClassFieldType()

        data object Char : DClassFieldType()

        data class Array(val type: DClassFieldType, val range: DClassParameter.ArrayParameter.ArrayRange) :
            DClassFieldType()

        data class Sized(val type: DClassRawFieldType.SizedType, val sizeConstraint: SizeConstraint?) :
            DClassFieldType() {
            data class SizeConstraint(val minSize: Literal.IntLiteral, val maxSize: Literal.IntLiteral?)
        }

        data class User(val type: DClassRawFieldType.UserType) : DClassFieldType()
    }
}

sealed interface DClassFileIndex {
    val dClasses: List<String>
    val byDClassName: Map<String, Pair<DClassId, DClassFile.TypeDecl.DClass>>
    val byDClassField: Map<Pair<String, UShort>, FieldId>
    val userTypes: Map<String, FieldValue.Type>

    fun getFieldId(
        dClassName: String,
        localFieldIndex: UShort,
    ): FieldId = byDClassField[dClassName to localFieldIndex]!!

    fun getDClassId(dClassName: String): DClassId = byDClassName[dClassName]!!.first

    fun getDClass(dClassName: String): DClassFile.TypeDecl.DClass = byDClassName[dClassName]!!.second

    fun getFieldId(
        dClassName: String,
        fieldName: String,
    ): FieldId {
        val (localFieldIndex, _) =
            getDClass(dClassName).fields.withIndex().find { (_, field) -> field.name === fieldName }!!

        return getFieldId(dClassName, localFieldIndex.toUShort())
    }

    fun getDClassFields(dClassName: String): List<Pair<FieldId, DClassFile.DClassField>> {
        val dClass = getDClass(dClassName)
        val dClassId = getDClassId(dClassName)
        val fields =
            dClass.parents.asSequence().flatMap { getDClassFields(it) }
                .plus(
                    dClass.fields.mapIndexed { index, field ->
                        (
                            getFieldId(
                                dClassName,
                                index.toUShort(),
                            ) to field
                        )
                    },
                ).associate { it }.toList().sortedBy { it.first.id }.toMutableList()

        // if a dclass has fields with the same name only use the last one described.
        val duplicates = mutableSetOf<FieldId>()
        val fieldName = mutableMapOf<String, FieldId>()
        for ((id, field) in fields) {
            if (field.name == null) continue

            val old = fieldName.put(field.name!!, id)
            if (old != null) {
                duplicates.add(old)
            }
        }

        fields.removeIf {
            duplicates.contains(it.first)
        }

        return fields
    }

    fun DClassFile.DClassField.parameters(
        dClassName: String,
        fieldId: FieldId,
    ): List<DClassFile.DClassParameter> {
        val allFields = getDClassFields(dClassName)
        return when (this) {
            is DClassFile.DClassField.AtomicField -> parameters
            is DClassFile.DClassField.MolecularField ->
                fields.flatMap { name ->
                    allFields.find { it.second.name == name }!!.second.parameters(dClassName, fieldId)
                }

            is DClassFile.DClassField.ParameterField -> listOf(parameter)
        }
    }
}

private class MutableDClassFileIndex(
    override val dClasses: MutableList<String> = mutableListOf(),
    override val byDClassName: MutableMap<String, Pair<DClassId, DClassFile.TypeDecl.DClass>> = mutableMapOf(),
    override val byDClassField: MutableMap<Pair<String, UShort>, FieldId> = mutableMapOf(),
    override val userTypes: MutableMap<String, FieldValue.Type> = mutableMapOf(),
    var dClassId: UShort = 0U,
    var fieldId: UShort = 0U,
) : DClassFileIndex {
    fun incFieldId() {
        fieldId = fieldId.inc()
    }

    fun incDClassId() {
        dClassId = dClassId.inc()
    }
}

fun DClassFile.buildIndex(): DClassFileIndex {
    val index = MutableDClassFileIndex()

    for (decl in decls) {
        when (decl) {
            is DClassFile.TypeDecl.DClass -> {
                index.dClasses.add(decl.name)
                index.byDClassName[decl.name] = index.dClassId.toDClassId() to decl

                for ((localIndex, field) in decl.fields.withIndex()) {
                    index.byDClassField[decl.name to localIndex.toUShort()] = index.fieldId.toFieldId()
                    index.incFieldId()
                }

                index.incDClassId()
            }

            is DClassFile.TypeDecl.Struct -> {
                for (param in decl.parameters) {
                    index.incFieldId()
                }

                index.userTypes[decl.name] =
                    FieldValue.Type.Tuple(*decl.parameters.map { it.type.toRawFieldValueType(index) }.toTypedArray())
                index.incDClassId()
            }

            is DClassFile.TypeDecl.TypeDef -> {
                index.userTypes[decl.newTypeName] = decl.type.toRawFieldValueType(index)
            }
        }
    }

    return index
}

fun DClassFile.DClassField.toRawFieldValueType(
    index: DClassFileIndex,
    parentClass: String? = null,
): FieldValue.Type {
    return when (this) {
        is DClassFile.DClassField.ParameterField -> {
            parameter.type.toRawFieldValueType(index)
        }

        is DClassFile.DClassField.AtomicField -> {
            val types = parameters.map { it.type.toRawFieldValueType(index) }
            if (types.isEmpty()) return FieldValue.Type.Empty
            if (types.size == 1) types[0] else FieldValue.Type.Tuple(*types.toTypedArray())
        }

        is DClassFile.DClassField.MolecularField -> {
            val classFields = index.getDClassFields(parentClass!!).associateBy { it.second.name }
            if (fields.size == 1) return classFields[fields[0]]!!.second.toRawFieldValueType(index)
            return FieldValue.Type.Tuple(
                *fields.map {
                    classFields[it]!!.second.toRawFieldValueType(index)
                }.toTypedArray(),
            )
        }
    }
}

fun DClassFile.DClassRawFieldType.toFieldValueType(index: DClassFileIndex? = null): FieldValue.Type {
    return when (this) {
        DClassFile.DClassRawFieldType.UInt64 -> FieldValue.Type.UInt64
        DClassFile.DClassRawFieldType.Int64 -> FieldValue.Type.Int64

        DClassFile.DClassRawFieldType.UInt32 -> FieldValue.Type.UInt32
        DClassFile.DClassRawFieldType.Int32 -> FieldValue.Type.Int32

        DClassFile.DClassRawFieldType.UInt16 -> FieldValue.Type.UInt16
        DClassFile.DClassRawFieldType.Int16 -> FieldValue.Type.Int16

        DClassFile.DClassRawFieldType.UInt8 -> FieldValue.Type.UInt8
        DClassFile.DClassRawFieldType.Int8 -> FieldValue.Type.Int8

        DClassFile.DClassRawFieldType.String -> FieldValue.Type.String
        DClassFile.DClassRawFieldType.Blob -> FieldValue.Type.Blob
        DClassFile.DClassRawFieldType.Char -> FieldValue.Type.Char
        DClassFile.DClassRawFieldType.Float64 -> FieldValue.Type.Float64
        is DClassFile.DClassRawFieldType.UserType -> index!!.userTypes[name]!!
    }
}

fun DClassFile.DClassFieldType.toRawFieldValueType(index: DClassFileIndex? = null): FieldValue.Type {
    return when (this) {
        is DClassFile.DClassFieldType.Array -> FieldValue.Type.Array(type.toRawFieldValueType(index), range.isExactSize())
        DClassFile.DClassFieldType.Char -> FieldValue.Type.Char
        is DClassFile.DClassFieldType.Float -> FieldValue.Type.Float64
        is DClassFile.DClassFieldType.Int -> type.toFieldValueType()
        is DClassFile.DClassFieldType.Sized -> type.toFieldValueType()
        is DClassFile.DClassFieldType.User -> index?.userTypes?.get(type.name) ?: error("missing type for ${type.name}")
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

fun DClassFile.DClassField.toDistributedFieldSpec(
    index: DClassFileIndex,
    fieldId: FieldId,
    parentClass: String,
): DistributedFieldSpec {
    return DistributedFieldSpec(
        toRawFieldValueType(index, parentClass),
        name ?: "field${fieldId.id}",
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
        },
    )
}

fun DClassFile.DClassRawFieldType.toType(): String =
    when (this) {
        is DClassFile.DClassRawFieldType.UInt8 -> "uint8"
        is DClassFile.DClassRawFieldType.Int8 -> "int8"
        is DClassFile.DClassRawFieldType.UInt16 -> "uint16"
        is DClassFile.DClassRawFieldType.Int16 -> "int16"
        is DClassFile.DClassRawFieldType.UInt32 -> "uint32"
        is DClassFile.DClassRawFieldType.Int32 -> "int32"
        is DClassFile.DClassRawFieldType.UInt64 -> "uint64"
        is DClassFile.DClassRawFieldType.Int64 -> "int64"
        is DClassFile.DClassRawFieldType.Blob -> "blob"
        is DClassFile.DClassRawFieldType.String -> "string"
        is DClassFile.DClassRawFieldType.Char -> "char"
        is DClassFile.DClassRawFieldType.Float64 -> "float64"
        is DClassFile.DClassRawFieldType.UserType -> name
    }

fun DClassFile.DClassParameter.ArrayParameter.ArrayRange.toType(): String =
    when (this) {
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

fun DClassFile.DClassFieldType.Sized.SizeConstraint.toType(): String = if (maxSize != null) "($minSize - $maxSize)" else "($minSize)"

fun String.toDClassFieldType(): DClassFile.DClassRawFieldType {
    return when (this) {
        "uint8" -> DClassFile.DClassRawFieldType.UInt8
        "int8" -> DClassFile.DClassRawFieldType.Int8
        "uint16" -> DClassFile.DClassRawFieldType.UInt16
        "int16" -> DClassFile.DClassRawFieldType.Int16
        "uint32" -> DClassFile.DClassRawFieldType.UInt32
        "int32" -> DClassFile.DClassRawFieldType.Int32
        "uint64" -> DClassFile.DClassRawFieldType.UInt64
        "int64" -> DClassFile.DClassRawFieldType.Int64
        "float64" -> DClassFile.DClassRawFieldType.Float64
        "char" -> DClassFile.DClassRawFieldType.Char
        "string" -> DClassFile.DClassRawFieldType.String
        "blob" -> DClassFile.DClassRawFieldType.Blob
        else -> DClassFile.DClassRawFieldType.UserType(this)
    }
}
