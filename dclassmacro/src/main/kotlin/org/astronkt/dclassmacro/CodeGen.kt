package org.astronkt.dclassmacro

import org.astronkt.DistributedFieldModifiers
import org.astronkt.FieldValue
import org.astronkt.ProtocolMessageArgumentSpec.Dynamic.name
import org.astronkt.ProtocolMessageArgumentSpec.Dynamic.type

fun generateDClassHelper(dClassFile: DClassFile): String =
    buildString {
        val index = dClassFile.buildIndex()
        for (dClassName in index.dClasses) {
            val (dClassId, dClass) = index.byDClassName[dClassName]!!
            val id = dClassId.id
            append("@Suppress(\"unused\")\n")
            append("open class $dClassName (doId: DOId): DistributedObject(doId, ${id}U.toDClassId()) {\n")

            generatePreamble(index, dClassName, id, dClass)
            generateFieldSpecs(index, dClass)
            generateFields(index, dClassName, dClass)
            generateEventSetters(dClass)

            append("}\n\n")
        }

        generateClassSpec(index, dClassFile)
    }

fun StringBuilder.generatePreamble(
    index: DClassFileIndex,
    dClassName: String,
    id: UShort,
    dClass: DClassFile.TypeDecl.DClass,
) {
    append("\tcompanion object {\n")
    append("\t\tval dClassId = ${id}U.toDClassId()\n\n")
    append("\t\tobject Fields {\n")
    for ((localFieldIndex, field) in dClass.fields.withIndex()) {
        val fieldId = index.getFieldId(dClassName, localFieldIndex.toUShort()).id
        val name = field.name ?: "field$fieldId"
        append("\t\t\tval ${field.name}: FieldId = ${fieldId}U.toFieldId()\n")
    }
    append("\t\t}\n")
    append("\t}\n")
}

fun StringBuilder.generateFieldSpecs(
    index: DClassFileIndex,
    dClass: DClassFile.TypeDecl.DClass,
) {
    append("\toverride val objectFields: Map<FieldId, DistributedField> = mapOf(\n")
    for ((localIndex, field) in dClass.fields.withIndex()) {
        val fieldId = index.getFieldId(dClass.name, localIndex.toUShort()).id
        val fieldName = field.name ?: "field$fieldId"
        val spec = field.toDistributedFieldSpec()
        val isField = field is DClassFile.DClassField.ParameterField
        append("\t\t${fieldId}U.toFieldId() to DistributedField(\n")
        append("\t\t\tDistributedFieldSpec(\n")
        append("\t\t\t\t${spec.type.toTypeCode()},\n")
        append("\t\t\t\tmodifiers = DistributedFieldModifiers(\n")

        val printModifier = { name: String -> append("\t\t\t\t\t$name = true,\n") }
        if (spec.modifiers.ram) printModifier("ram")
        if (spec.modifiers.required) printModifier("required")
        if (spec.modifiers.db) printModifier("db")
        if (spec.modifiers.airecv) printModifier("airecv")
        if (spec.modifiers.ownrecv) printModifier("ownrecv")
        if (spec.modifiers.clrecv) printModifier("clrecv")
        if (spec.modifiers.broadcast) printModifier("broadcast")
        if (spec.modifiers.ownsend) printModifier("ownsend")
        if (spec.modifiers.clsend) printModifier("clsend")

        append("\t\t\t\t)\n")
        append("\t\t\t),\n")

        append("\t\t\tonChange = { it, sender ->\n")
        append(
            "${
                field
                    .toOnSetCode(
                        "on${if (isField) "Set" else ""}${fieldName.replaceFirstChar { it.uppercase() }}",
                        "it",
                        "\t\t\t\t",
                    )
            }\n",
        )
        append("\t\t\t}\n")
        append("\t\t),\n")
    }
    append("\t)\n\n")
}

fun StringBuilder.generateFields(
    index: DClassFileIndex,
    dClassName: String,
    dClass: DClassFile.TypeDecl.DClass,
) {
    for ((localIndex, field) in dClass.fields.withIndex()) {
        val id = index.getFieldId(dClassName, localIndex.toUShort()).id
        when (field) {
            is DClassFile.DClassField.ParameterField -> {
                val rawType = field.parameter.type.toRawFieldValueType().toKotlinType()
                val userType = field.parameter.type.toUserKotlinType()
                append("\tvar ${field.name}: $userType\n")
                append(
                    "\t\tget() = getField(${id}U.toFieldId())?.${field.parameter.type.toRawFieldValueType().toDestructureCodePrimitive()}",
                )
                if (field.parameter.type.needsTransform()) {
                    append(".transform {\n")
                    append(field.parameter.type.generateTransformBody("\t\t\t"))
                    append("\n\t\t}\n")

                    append(
                        "\t\tset(value) = setField(${id}U.toFieldId(), value.unTransform(${field.parameter.type.toRawFieldValueType().toTypeCode()}) {\n",
                    )
                    append(field.parameter.type.generateTransformBody("\t\t\t"))
                    append("\n\t\t})\n")
                } else {
                    append("\n\t\tset(value) = setField(${id}U.toFieldId(), value.toFieldValue())\n")
                }
            }

            is DClassFile.DClassField.AtomicField -> {
                data class ParameterInfo(
                    val name: String,
                    val type: DClassFile.DClassFieldType,
                    val rawType: FieldValue.Type,
                    val userType: String,
                )

                val types =
                    field.parameters.mapIndexed { localIndex, it ->
                        ParameterInfo(it.name ?: "arg$localIndex", it.type, it.type.toRawFieldValueType(), it.type.toUserKotlinType())
                    }
                append("\tfun ${field.name}(")
                for ((name, type, rawType, userType) in types) {
                    append("$name: $userType, ")
                }
                append(") {\n")
                append(
                    "\t\tsetField(${id}U.toFieldId(), ",
                )
                when (types.size) {
                    0 -> append("FieldValue.EmptyValue)\n")
                    1 -> {
                        val (name, type, rawType, userType) = types[0]
                        if (type.needsTransform()) {
                            append("$name.unTransform(${type.toRawFieldValueType().toTypeCode()}) {\n")
                            append(type.generateTransformBody("\t\t\t"))
                            append("\n\t\t})\n")
                        } else {
                            append("$name.toFieldValue())\n")
                        }
                    }
                    else -> {
                        append("FieldValue.TupleValue(\n")
                        for ((name, type, rawType, userType) in types) {
                            if (type.needsTransform()) {
                                append("\t\t\t$name.unTransform(${type.toRawFieldValueType().toTypeCode()}) {\n")
                                append(type.generateTransformBody("\t\t\t\t"))
                                append("\n\t\t\t},\n")
                            } else {
                                append("\t\t\t$name.toFieldValue(),\n")
                            }
                        }
                        append("\t\t)\n")
                    }
                }
                append("\t}\n")
            }

            is DClassFile.DClassField.MolecularField -> ""
        }
        append("\n")
    }
}

private fun StringBuilder.generateEventSetters(dClass: DClassFile.TypeDecl.DClass) {
//    for (field in dClass.fields) {
//        val type = field.toFieldValueType()
//        if (type is FieldValue.Type.Tuple) {
//            append("\topen fun on${field.name.replaceFirstChar { it.titlecase() }}(")
//            val method = field as DClassFile.DClassField.AtomicField
//            for ((name, fieldType) in method.type) {
//                append("$name: ${fieldType.toFieldValueType().toKotlinType()}, ")
//            }
//            append("sender: ChannelId? = null) {}\n")
//        } else {
//            when (field) {
//                is DClassFile.DClassField.ParameterField -> {
//                    append("\topen fun onSet${field.name.replaceFirstChar { it.titlecase() }}(new: ${type.toKotlinType()}, sender: ChannelId? = null) {}\n")
//                }
//
//                is DClassFile.DClassField.AtomicField -> {
//                    append("\topen fun on${field.name.replaceFirstChar { it.titlecase() }}(${field.type[0].first}: ${type.toKotlinType()}, sender: ChannelId? = null) {}\n")
//                }
//            }
//        }
//    }
}

private fun StringBuilder.generateClassSpec(
    index: DClassFileIndex,
    file: DClassFile,
) {
    append("val classSpecRepository = ClassSpecRepository.build {\n")
    index.dClasses.forEach {
        val dClass = index.byDClassName[it]!!.second

        append("\tdclass {\n")
        for (field in dClass.fields) {
            append("\t\tfield(${field.toRawFieldValueType().toTypeCode()})")
            val spec = field.toDistributedFieldSpec()

            if (spec.modifiers == DistributedFieldModifiers()) {
                append("\n")
                continue
            }

            append(" {\n")

            val printModifier = { keyword: String -> append("\t\t\t$keyword()\n") }
            if (spec.modifiers.ram) printModifier("ram")
            if (spec.modifiers.required) printModifier("required")
            if (spec.modifiers.db) printModifier("db")
            if (spec.modifiers.airecv) printModifier("airecv")
            if (spec.modifiers.ownrecv) printModifier("ownrecv")
            if (spec.modifiers.clrecv) printModifier("clrecv")
            if (spec.modifiers.broadcast) printModifier("broadcast")
            if (spec.modifiers.ownsend) printModifier("ownsend")
            if (spec.modifiers.clsend) printModifier("clsend")

            append("\t\t}\n")
        }

        append("\t}\n")
    }
    append("}\n")
}

private fun FieldValue.Type.toTypeCode(): String =
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

private fun FieldValue.Type.toKotlinType(): String =
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

private fun DClassFile.DClassFieldType.toUserKotlinType(): String =
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

private fun FieldValue.Type.toDestructureCodePrimitive(): String =
    when (this) {
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
        is FieldValue.Type.Array -> "toArrayValue()!!"
        is FieldValue.Type.Tuple -> error("tuples are not a simple primitive")
        else -> "TODODestructureCodePrimitive"
    }

private fun DClassFile.DClassParameter.IntParameter.IntTransform.toTransformCodeBody(prependIndent: String): String {
    val transforms = mutableListOf<String>()
    var transform: DClassFile.DClassParameter.IntParameter.IntTransform? = this

    while (transform != null) {
        val float = transform.literal.value.toFloat()
        transforms +=
            when (transform.operator) {
                DClassFile.Operator.Divide -> "${prependIndent}divide($float)"
                DClassFile.Operator.Minus -> "${prependIndent}minus($float)"
                DClassFile.Operator.Modulo -> "${prependIndent}modulo($float)"
                DClassFile.Operator.Multiply -> "${prependIndent}multiply($float)"
                DClassFile.Operator.Plus -> "${prependIndent}plus($float)"
            }
        transform = transform.next
    }
    return transforms.asReversed().joinToString("\n")
}

private fun DClassFile.DClassParameter.FloatParameter.FloatTransform.toTransformCodeBody(prependIndent: String): String {
    val transforms = mutableListOf<String>()
    var transform: DClassFile.DClassParameter.FloatParameter.FloatTransform? = this

    while (transform != null) {
        val float = transform.literal.toDouble().toFloat()
        transforms +=
            when (transform.operator) {
                DClassFile.Operator.Divide -> "${prependIndent}divide($float)"
                DClassFile.Operator.Minus -> "${prependIndent}minus($float)"
                DClassFile.Operator.Modulo -> "${prependIndent}modulo($float)"
                DClassFile.Operator.Multiply -> "${prependIndent}multiply($float)"
                DClassFile.Operator.Plus -> "${prependIndent}plus($float)"
            }
        transform = transform.next
    }
    return transforms.asReversed().joinToString("\n")
}

private fun DClassFile.DClassFieldType.generateTransformBody(prependIndent: String): String {
    return buildString {
        when (this@generateTransformBody) {
            is DClassFile.DClassFieldType.Float -> {
                append(transform?.toTransformCodeBody(prependIndent))
            }
            is DClassFile.DClassFieldType.Int -> {
                append(transform?.toTransformCodeBody(prependIndent))
            }
            else -> error("should be unreachable, cant make transform body $this")
        }
    }
}

private fun DClassFile.DClassFieldType.needsTransform(): Boolean =
    when (this) {
        is DClassFile.DClassFieldType.Float -> {
            transform != null || range != null
        }
        is DClassFile.DClassFieldType.Int -> {
            transform != null || range != null
        }
        else -> false
    }

private fun DClassFile.DClassField.toOnSetCode(
    method: String,
    variable: String,
    linePrepend: String,
): String = ""
// when (this) {
//    is FieldValue.Type.Tuple -> {
//        buildString {
//            append("${linePrepend}it.toTuple()!!.let { values ->\n")
//            for ((index, type) in types.withIndex()) {
//                append("${linePrepend}\tval t$index = values[$index].${type.toDestructureCodePrimitive()}\n")
//            }
//            append("${linePrepend}\t$method(")
//            for (i in types.indices) {
//                append("t$i, ")
//            }
//            append("sender)\n")
//            append("$linePrepend}")
//        }
//    }
//    FieldValue.Type.Empty -> "$linePrepend$method(sender)"

//    else -> "${linePrepend}$method($variable.${toDestructureCodePrimitive()}, sender)"
// }
