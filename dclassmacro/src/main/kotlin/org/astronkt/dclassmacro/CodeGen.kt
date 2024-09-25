package org.astronkt.dclassmacro

import org.astronkt.DistributedFieldModifiers
import org.astronkt.FieldValue


fun generateDClassHelper(dClassFile: DClassFile): String =
    buildString {
        val index = dClassFile.buildIndex()

        append("@file:Suppress(\"unused\", \"NestedLambdaShadowedImplicitParameter\", \"TrailingComma\")\n\n")

        append("import org.astronkt.*\n\n")

        for (decl in dClassFile.decls) {
            when (decl) {
                is DClassFile.TypeDecl.Struct -> generateStruct(index, decl)
                is DClassFile.TypeDecl.TypeDef -> generateTypeDef(index, decl)
                else -> {}
            }
        }
        for (dClassName in index.dClasses) {
            val (dClassId, dClass) = index.byDClassName[dClassName]!!
            val id = dClassId.id
            append("open class $dClassName(doId: DOId): DistributedObjectBase(doId, ${id}U.toDClassId()) {\n")

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
        val spec = field.toDistributedFieldSpec(index)
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
                val type = field.parameter.type
                val rawType = type.toRawFieldValueType(index)
                val userType = type.toUserKotlinType()
                val userDefined = type.userDefinedType()
                append("\tvar ${field.name}: $userType\n\t\tget() = ")

                append(type.destructure(index, "getField(${id}U.toFieldId())!!"))
                append("\n")
                append("\t\tset(value) { ")
                append(type.restructure(index, "value"))
                append("}\n")

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
                        ParameterInfo(
                            it.name ?: "arg$localIndex",
                            it.type,
                            it.type.toRawFieldValueType(index),
                            it.type.toUserKotlinType()
                        )
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
                    0 -> append("FieldValue.EmptyValue)")
                    1 -> {
                        append(types[0].type.restructure(index, types[0].name))
                        append(")")
                    }

                    else -> {
                        append("FieldValue.TupleValue(\n\t\t\t")
                        for ((name, type, rawType, userType) in types) {
                            append(type.restructure(index, name))
                            append(", \n\t\t\t")
                        }
                        append("\t))")
                    }
                }
                append("\n\t}\n")
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
            append("\t\tfield(${field.toRawFieldValueType(index).toTypeCode()})")
            val spec = field.toDistributedFieldSpec(index)

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

fun StringBuilder.generateTypeDef(index: DClassFileIndex, typeDef: DClassFile.TypeDecl.TypeDef) {
    val name = typeDef.newTypeName
    val type = typeDef.type
    val rawType = type.toRawFieldValueType(index)
    val userDefined = type.userDefinedType()
    val arrayType = type.arrayType()

    append("data class $name(val inner: ${type.toUserKotlinType()}) {\n")
    append("\tcompanion object: ToFieldValue<$name> {\n")
    append("\t\toverride fun fromFieldValue(value: FieldValue): $name {\n")

    append("\t\t\tval inner = ")
    append(type.destructure(index, "value"))

    append("\n")

    append("\t\t\treturn $name(inner)\n")
    append("\t\t}\n\n")
    append("\t\toverride val type: FieldValue.Type = ${rawType.toTypeCode()}\n\n")

    append("\t\toverride fun $name.toFieldValue(): FieldValue = ")
    append(type.restructure(index, "inner"))
    append("\n")

    append("\t}\n")
    append("}\n")
}

fun StringBuilder.generateStruct(index: DClassFileIndex, struct: DClassFile.TypeDecl.Struct) {
    val name = struct.name
    val structType = FieldValue.Type.Tuple(*struct.parameters.map { it.type.toRawFieldValueType(index) }.toTypedArray())
    append("data class $name(")
    for (param in struct.parameters) {
        val name = param.name
        val type = param.type.toUserKotlinType()
        append("val $name: $type, ")
    }
    append(") {\n")
    append("\tcompanion object: ToFieldValue<$name> {\n")
    append("\t\toverride val type: FieldValue.Type = ${structType.toTypeCode()}\n\n")
    append("\t\toverride fun fromFieldValue(value: FieldValue): $name {\n")
    append("\t\t\tval tuple = value.toTuple()!!\n")

    // TODO: add transform logic, should be okay for now when type checked
    for ((localIndex, parameter) in struct.parameters.withIndex()) {
        val paramName = parameter.name ?: "arg$localIndex"
        val destructure = parameter.type.toDestructureCodePrimitive(index)
        val userDefined = parameter.type.userDefinedType()
        val arrayType = parameter.type.arrayType()

        append("\t\t\tval $paramName = ")

        append(parameter.type.destructure(index, "tuple[$localIndex]"))

        append("\n")
    }
    append("\t\t\treturn $name(${struct.parameters.map { it.name }.joinToString(", ")})\n")
    append("\t\t}\n\n")
    append("\t\toverride fun $name.toFieldValue(): FieldValue =\n")
    append("\t\t\tFieldValue.TupleValue(")
    append(
        struct.parameters.mapIndexed { localIndex, it ->
            it.type.restructure(index, it.name ?: "arg$localIndex")
        }
            .joinToString(", ")
    )
    append(")\n")
    append("\t}\n")
    append("}\n")
}


fun DClassFile.DClassFieldType.destructure(index: DClassFileIndex, expr: String): String {
    val arrayType = arrayType()
    val userDefined = userDefinedType()
    val destructure = toDestructureCodePrimitive(index)

    return buildString {
        if (userDefined != null) {
            append("with($userDefined) { ")
        }

        append("$expr.$destructure")
        if (arrayType != null) {
            var subArrayType = arrayType
            var neededBraces = 0
            while (true) {
                append(".map { ")
                neededBraces += 1

                val next = subArrayType!!.arrayType()
                if (next == null) break

                append("it.toList()!!")
                subArrayType = next
            }

            if (userDefined != null) {
                append("fromFieldValue(it)")
            } else {
                append("it.${subArrayType!!.toDestructureCodePrimitive(index)}")
            }

            for (i in 0..<neededBraces) {
                append(" }")
            }
        }

        if (needsTransform()) {
            append(".transform {\n")
            append(generateTransformBody("\t\t\t"))
            append("\n\t\t}")
        }

        if (userDefined != null) {
            append("}")
        }

    }
}

fun DClassFile.DClassFieldType.restructure(index: DClassFileIndex, expr: String): String {
    val arrayType = arrayType()
    val userDefined = userDefinedType()

    return buildString {
        if (userDefined != null) {
            append("with($userDefined) { ")
        }

        if (arrayType != null) {
            append("FieldValue.ArrayValue(${arrayType.toRawFieldValueType(index).toTypeCode()}, $expr")

            var subArrayType = arrayType
            var neededBraces = 0
            while (true) {
                append(".map { it")
                neededBraces += 1

                val next = subArrayType!!.arrayType()
                if (next == null) break

                subArrayType = next
            }

            if (needsTransform()) {
                append(".unTransform(${subArrayType!!.toRawFieldValueType(index).toTypeCode()}) {\n")
                append(generateTransformBody("\t\t\t"))
                append("\n\t\t}")
            }
            for (i in 0..<neededBraces) {
                append(".toFieldValue() }")
            }
            append(")")
        } else {
            append(expr)
            if (needsTransform()) {
                append(".unTransform(${toRawFieldValueType(index).toTypeCode()}) {\n")
                append(generateTransformBody("\t\t\t"))
                append("\n\t\t}")
            }
        }
        append(".toFieldValue()")


        if (userDefined != null) {
            append("}")

        }
    }

}

fun FieldValue.Type.innerType(): FieldValue.Type? = when (this) {
    is FieldValue.Type.Array -> type
    else -> null
}