@file:Suppress("unused")

package org.astronkt.dclassmacro

import org.astronkt.DistributedFieldModifiers
import org.astronkt.FieldId
import org.astronkt.FieldValue
import java.io.File

fun generateDClassHelper(
    dClassFile: DClassFile,
    directory: File,
) {
    assert(directory.isDirectory)

    val index = dClassFile.buildIndex()

    val classSpec = File(directory.path + "/ClassSpec.kt")
    classSpec.writeText(
        buildString {
            append("@file:Suppress(\"unused\", \"NestedLambdaShadowedImplicitParameter\", \"TrailingComma\")\n\n")

            append("package GameSpec\n\n")

            append("import org.astronkt.*\n")

            generateClassSpec(index, dClassFile)
        },
    )

    for (decl in dClassFile.decls) {
        val name =
            when (decl) {
                is DClassFile.TypeDecl.DClass -> decl.name
                is DClassFile.TypeDecl.Struct -> decl.name
                is DClassFile.TypeDecl.TypeDef -> decl.newTypeName
            }

        val classFile = File(directory.path + "/$name.kt")

        classFile.writeText(
            buildString {
                append("@file:Suppress(\"unused\", \"NestedLambdaShadowedImplicitParameter\", \"TrailingComma\")\n\n")

                append("package GameSpec\n\n")

                append("import org.astronkt.*\n")
                // append("import GameSpec.*\n\n")

                when (decl) {
                    is DClassFile.TypeDecl.Struct -> generateStruct(index, decl)
                    is DClassFile.TypeDecl.TypeDef -> generateTypeDef(index, decl)
                    is DClassFile.TypeDecl.DClass -> {
                        val dClassName = decl.name
                        val (dClassId, dClass) = index.byDClassName[dClassName]!!
                        val id = dClassId.id
                        append("open class $dClassName(doId: DOId): DistributedObjectBase(doId, ${id}U.toDClassId()) {\n")

                        generatePreamble(index, dClassName, id)
                        generateFieldSpecs(index, dClass)
                        generateFields(index, dClassName)
                        generateEventSetters(index, dClass)

                        append("}\n\n")
                    }
                }
                append("\n")
            },
        )
    }
}

fun StringBuilder.generatePreamble(
    index: DClassFileIndex,
    dClassName: String,
    id: UShort,
) {
    append("\tcompanion object {\n")
    append("\t\tval dClassId = ${id}U.toDClassId()\n\n")
    append("\t\tobject Fields {\n")
    for ((fieldId, field) in index.getDClassFields(dClassName)) {
        val name = field.name ?: "field${fieldId.id}"
        append("\t\t\tval $name: FieldId = ${fieldId.id}U.toFieldId()\n")
    }
    append("\t\t}\n")
    append("\t}\n")
}

fun StringBuilder.generateFieldSpecs(
    index: DClassFileIndex,
    dClass: DClassFile.TypeDecl.DClass,
) {
    append("\toverride val objectFields: Map<FieldId, DistributedField> = mapOf(\n")
    for (tuple in index.getDClassFields(dClass.name)) {
        val (fieldId, field) = tuple
        val fieldName = field.name ?: "field${fieldId.id}"
        val spec = field.toDistributedFieldSpec(index, fieldId, dClass.name)
        field is DClassFile.DClassField.ParameterField
        append("\t\t${fieldId.id}U.toFieldId() to DistributedField(\n")
        append("\t\t\tDistributedFieldSpec(\n")
        append("\t\t\t\t${spec.type.toTypeCode()},\n")
        append("\t\t\t\t\"$fieldName\",\n")
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

        append("\t\t\t\t),\n")

        if (spec.molecular != null) {
            append("\t\t\t\tmolecular = listOf(")
            for (id in spec.molecular!!) {
                append("${id.id}U.toFieldId(), ")
            }
            append(")\n")
        }

        append("\t\t\t),\n")

        append("\t\t\tonChange = { it, sender ->\n")
        append(
            "${
                field
                    .toOnSetCode(
                        index,
                        dClass.name,
                        fieldId,
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
) {
    for ((fieldId, field) in index.getDClassFields(dClassName)) {
        when (field) {
            is DClassFile.DClassField.ParameterField -> {
                val type = field.parameter.type
                type.toRawFieldValueType(index)
                val userType = type.toUserKotlinType()
                type.userDefinedType()
                append("\tvar ${field.name}: $userType\n\t\tget() = ")

                append(type.destructure(index, "getField(${fieldId.id}U.toFieldId())!!", "\t\t"))
                append("\n")
                append("\t\tset(value) { ")
                append("setField(${fieldId.id}U.toFieldId(), ")
                append(type.restructure(index, "value", "\t\t"))
                append(") }\n")
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
                            it.type.toUserKotlinType(),
                        )
                    }
                append("\tfun ${field.name}(")
                for ((name, _, _, userType) in types) {
                    append("$name: $userType, ")
                }
                append(") {\n")
                append(
                    "\t\tsetField(${fieldId.id}U.toFieldId(), ",
                )
                when (types.size) {
                    0 -> append("FieldValue.EmptyValue)")
                    1 -> {
                        append(types[0].type.restructure(index, types[0].name, "\t\t"))
                        append(")")
                    }

                    else -> {
                        append("FieldValue.TupleValue(\n\t\t\t")
                        for ((name, type, _, _) in types) {
                            append(type.restructure(index, name, "\t\t"))
                            append(", \n\t\t\t")
                        }
                        append("\t))")
                    }
                }
                append("\n\t}\n")
            }

            is DClassFile.DClassField.MolecularField -> {
                assert(field.fields.isNotEmpty()) { "expected molecular to have more than zero fields" }

                val name = field.name
                val fieldMap = index.getDClassFields(dClassName)
                val atoms =
                    field.fields.map { atomName -> fieldMap.find { it.second.name == atomName }!!.second }
                        .flatMap { atom ->
                            with(index) { atom.parameters(dClassName, fieldId) }
                        }

                append("\tfun $name(")
                for ((localIndex, atom) in atoms.withIndex()) {
                    append("${atom.name ?: "arg$localIndex"}: ${atom.type.toUserKotlinType()}, ")
                }
                append(") {\n")

                for ((localIndex, atom) in atoms.withIndex()) {
                    append("\t\tval ${atom.name ?: "arg${localIndex}Value"} = ")
                    append(atom.type.restructure(index, "arg$localIndex", "\t\t"))
                    append("\n")
                }

                append("\n")

                if (atoms.size == 1) {
                    append("\t\tsetField(${fieldId.id}U.toFieldId(), arg0Value)\n")
                } else {
                    append("\t\tsetField(${fieldId.id}U.toFieldId(), FieldValue.TupleValue(")

                    for ((localIndex, _) in atoms.withIndex()) {
                        append("arg${localIndex}Value, ")
                    }

                    append("))\n")
                }

                append("\t}\n")
            }
        }
        append("\n")
    }
}

private fun StringBuilder.generateEventSetters(
    index: DClassFileIndex,
    dClass: DClassFile.TypeDecl.DClass,
) {
    for ((fieldId, field) in index.getDClassFields(dClass.name)) {
        val fieldName = (field.name ?: "field${fieldId.id}").replaceFirstChar { it.uppercase() }
        val parameters = with(index) { field.parameters(dClass.name, fieldId) }

        when (field) {
            is DClassFile.DClassField.ParameterField -> {
                append("\topen fun onSet$fieldName(")
            }

            is DClassFile.DClassField.AtomicField -> {
                append("\topen fun on$fieldName(")
            }

            is DClassFile.DClassField.MolecularField -> {
                append("\topen fun on$fieldName(")
            }
        }

        for ((localIndex, parameter) in parameters.withIndex()) {
            val paramName = parameter.name ?: "arg$localIndex"
            append("$paramName: ${parameter.type.toUserKotlinType()}, ")
        }
        append("sender: ChannelId? = null) {}\n")
    }
}

private fun StringBuilder.generateClassSpec(
    index: DClassFileIndex,
    file: DClassFile,
) {
    append("val classSpecRepository = ClassSpecRepository.build {\n")
    for (decl in file.decls) {
        when (decl) {
            is DClassFile.TypeDecl.DClass -> {

                append("\tdclass(\"${decl.name}\", listOf(${decl.parents.joinToString(",") { "\"$it\"" }})) {\n")
                for ((localIndex, field) in decl.fields.withIndex()) {
                    val fieldId = index.getFieldId(decl.name, localIndex.toUShort())
                    val fieldName = field.name ?: "field${fieldId.id}"
                    append("\t\tfield(\"$fieldName\", ${field.toRawFieldValueType(index, decl.name).toTypeCode()})")
                    val spec = field.toDistributedFieldSpec(index, fieldId, decl.name)

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

            is DClassFile.TypeDecl.Struct -> {
                append("\tstruct(\"${decl.name}\") {\n")

                for ((localIndex, field) in decl.parameters.withIndex()) {
                    val fieldName = field.name ?: "field$localIndex"
                    append("\t\tfield(\"$fieldName\",${field.type.toRawFieldValueType(index).toTypeCode()})\n")
                }

                append("\t}\n")
            }

            is DClassFile.TypeDecl.TypeDef -> {}
        }
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
    index: DClassFileIndex,
    dClassName: String,
    fieldId: FieldId,
    linePrepend: String,
): String {
    val fieldName = (name ?: "field${fieldId.id}").replaceFirstChar { it.uppercase() }
    val methodName =
        when (this) {
            is DClassFile.DClassField.ParameterField -> "onSet$fieldName"
            else -> "on$fieldName"
        }

    val parameters = with(index) { parameters(dClassName, fieldId) }
    return when (parameters.size) {
        0 -> {
            buildString {
                append("${linePrepend}$methodName(sender)")
            }
        }

        1 -> {
            buildString {
                append("${linePrepend}$methodName(")
                append(parameters[0].type.destructure(index, "it", "${linePrepend}\t"))
                append(", sender)")
            }
        }

        else -> {
            buildString {
                append("${linePrepend}it.toTuple()!!.let { values ->\n")
                for ((localIndex, parameter) in parameters.withIndex()) {
                    append("${linePrepend}val t$localIndex = ")
                    append(parameter.type.destructure(index, "values[$localIndex]", "${linePrepend}\t"))
                    append("\n")
                }

                append("${linePrepend}$methodName(")
                for ((localIndex, _) in parameters.withIndex()) {
                    append("t$localIndex, ")
                }
                append("sender)\n")
                append("$linePrepend}")
            }
        }
    }
}

fun StringBuilder.generateTypeDef(
    index: DClassFileIndex,
    typeDef: DClassFile.TypeDecl.TypeDef,
) {
    val name = typeDef.newTypeName
    val type = typeDef.type
    val rawType = type.toRawFieldValueType(index)

    append("data class $name(val inner: ${type.toUserKotlinType()}) {\n")
    append("\tcompanion object: ToFieldValue<$name> {\n")
    append("\t\toverride fun fromFieldValue(value: FieldValue): $name {\n")

    append("\t\t\tval inner = ")
    append(type.destructure(index, "value", "\t\t\t"))

    append("\n")

    append("\t\t\treturn $name(inner)\n")
    append("\t\t}\n\n")
    append("\t\toverride val type: FieldValue.Type = ${rawType.toTypeCode()}\n\n")

    append("\t\toverride fun $name.toFieldValue(): FieldValue = ")
    append(type.restructure(index, "inner", "\t\t"))
    append("\n")

    append("\t}\n")
    append("}\n")
}

fun StringBuilder.generateStruct(
    index: DClassFileIndex,
    struct: DClassFile.TypeDecl.Struct,
) {
    val name = struct.name
    val structType = FieldValue.Type.Tuple(*struct.parameters.map { it.type.toRawFieldValueType(index) }.toTypedArray())
    append("data class $name(")
    for (param in struct.parameters) {
        val paramName = param.name
        val type = param.type.toUserKotlinType()
        append("val $paramName: $type, ")
    }
    append(") {\n")
    append("\tcompanion object: ToFieldValue<$name> {\n")
    append("\t\toverride val type: FieldValue.Type = ${structType.toTypeCode()}\n\n")
    append("\t\toverride fun fromFieldValue(value: FieldValue): $name {\n")
    append("\t\t\tval tuple = value.toTuple()!!\n")

    for ((localIndex, parameter) in struct.parameters.withIndex()) {
        val paramName = parameter.name ?: "arg$localIndex"

        append("\t\t\tval $paramName = ")

        append(parameter.type.destructure(index, "tuple[$localIndex]", "\t\t\t"))

        append("\n")
    }
    append("\t\t\treturn $name(${struct.parameters.map { it.name }.joinToString(", ")})\n")
    append("\t\t}\n\n")
    append("\t\toverride fun $name.toFieldValue(): FieldValue =\n")
    append("\t\t\tFieldValue.TupleValue(")
    append(
        struct.parameters.mapIndexed { localIndex, it ->
            it.type.restructure(index, it.name ?: "arg$localIndex", "\t\t\t")
        }
            .joinToString(", "),
    )
    append(")\n")
    append("\t}\n")
    append("}\n")
}

fun DClassFile.DClassFieldType.destructure(
    index: DClassFileIndex,
    expr: String,
    prependIndent: String,
): String {
    val arrayType = arrayType()
    val userDefined = userDefinedType()
    val destructure = toDestructureCodePrimitive(index)

    return buildString {
        if (userDefined != null) {
            append("with($userDefined) { ")
        }

        if (arrayType != null) {
            append(expr)
            if (destructure != null) append(".$destructure")

            var subArrayType = arrayType
            var neededBraces = 0
            while (true) {
                append(".map { ")
                neededBraces += 1

                val next = subArrayType!!.arrayType() ?: break

                append("it.toList()!!")
                subArrayType = next
            }

            if (userDefined != null) {
                append("fromFieldValue(it)")
            } else {
                append(subArrayType!!.destructure(index, "it", prependIndent))
            }

            for (i in 0..<neededBraces) {
                append(" }")
            }
        } else if (userDefined != null) {
            append("fromFieldValue(it)")
        } else {
            append(expr)
            if (destructure != null) append(".$destructure")

            if (needsTransform()) {
                append(".transform {\n")
                append(generateTransformBody("$prependIndent\t"))
                append("\n$prependIndent}")
            }
        }

        if (userDefined != null) {
            append("}")
        }
    }
}

fun DClassFile.DClassFieldType.restructure(
    index: DClassFileIndex,
    expr: String,
    prependIndent: String,
): String {
    val arrayType = arrayType()
    val userDefined = userDefinedType()

    return buildString {
        if (userDefined != null) {
            append("with($userDefined) { ")
        }

        if (arrayType != null) {
            append("FieldValue.ArrayValue(${arrayType.toRawFieldValueType(index).toTypeCode()}, $expr")

            var subArrayType: DClassFile.DClassFieldType = arrayType
            var neededBraces = 0
            while (true) {
                append(".map { it")
                neededBraces += 1

                val next = subArrayType.arrayType() ?: break

                subArrayType = next
            }

            if (needsTransform()) {
                append(".unTransform(${subArrayType.toRawFieldValueType(index).toTypeCode()}) {\n")
                append(generateTransformBody("$prependIndent\t"))
                append("\n$prependIndent}")
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

fun FieldValue.Type.innerType(): FieldValue.Type? =
    when (this) {
        is FieldValue.Type.Array -> type
        else -> null
    }
