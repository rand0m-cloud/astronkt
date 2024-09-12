package org.astronkt.dclassmacro

import org.astronkt.DistributedFieldModifiers
import org.astronkt.FieldValue

fun generateDClassHelper(dClassFile: DClassFile): String = buildString {
    val index = dClassFile.buildIndex()
    for (dClassName in index.dClasses) {
        val (dClassId, dClass) = index.byDClassName[dClassName]!!
        val id = dClassId.id
        append("@Suppress(\"unused\")\n")
        append("open class $dClassName (doId: DOId): DistributedObject(doId, ${id}U.toDClassId()) {\n")

        generatePreamble(index, dClassName, id, dClass)
        generateFieldSpecs(dClass)
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
    dClass: DClassFile.TypeDecl.DClass
) {
    append("\tcompanion object {\n")
    append("\t\tval dClassId = ${id}U.toDClassId()\n\n")
    append("\t\tobject Fields {\n")
    for (field in dClass.fields) {
        append("\t\t\tval ${field.name}: FieldId = ${index.getFieldId(dClassName, field.name).id}U.toFieldId()\n")
    }
    append("\t\t}\n")
    append("\t}\n")
    append("\toverride val startingFieldId: UShort = ${index.firstFieldId(dClassName).id}U\n")
}

fun StringBuilder.generateFieldSpecs(dClass: DClassFile.TypeDecl.DClass) {
    append("\toverride val objectFields: List<DistributedField> = listOf(\n")
    for (field in dClass.fields) {
        val spec = field.toDistributedFieldSpec()
        val isField = field is DClassFile.DClassField.Simple
        append("\t\tDistributedField(\n")
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
                field.toFieldValueType()
                    .toOnSetCode(
                        "on${if (isField) "Set" else ""}${field.name.replaceFirstChar { it.titlecase() }}",
                        "it",
                        "\t\t\t\t"
                    )
            }\n"
        )
        append("\t\t\t}\n")
        append("\t\t),\n")
    }
    append("\t)\n\n")

}

fun StringBuilder.generateFields(
    index: DClassFileIndex,
    dClassName: String,
    dClass: DClassFile.TypeDecl.DClass
) {
    for (field in dClass.fields) {
        val type = field.toFieldValueType()
        val id = index.getFieldId(dClassName, field.name).id
        when (field) {
            is DClassFile.DClassField.Simple -> {
                append("\tvar ${field.name}: ${type.toKotlinType()}\n")
                append(
                    "\t\tget() = getField(${id}U.toFieldId())?.${type.toDestructureCodePrimitive()}\n"
                )
                append("\t\tset(value) = setField(${id}U.toFieldId(), value.toFieldValue())\n")
            }

            is DClassFile.DClassField.Method -> {
                append("\tfun ${field.name}(")
                for ((name, fieldType) in field.type) {
                    append("$name: ${fieldType.toFieldValueType().toKotlinType()},")
                }
                append(") {\n")
                append(
                    "\t\tsetField(${
                        index.getFieldId(
                            dClassName,
                            field.name
                        ).id
                    }U.toFieldId(), "
                )
                if (field.type.size == 1) {
                    append(
                        "${field.type[0].first}.toFieldValue()"
                    )
                } else {
                    append("FieldValue.TupleValue(")
                    for ((name, _) in field.type) {
                        append("$name.toFieldValue(), ")
                    }
                    append(")")
                }
                append(")\n")
                append("\t}\n")
            }
        }
        append("\n")
    }
}

private fun StringBuilder.generateEventSetters(dClass: DClassFile.TypeDecl.DClass) {
    for (field in dClass.fields) {
        val type = field.toFieldValueType()
        if (type is FieldValue.Type.Tuple) {
            append("\topen fun on${field.name.replaceFirstChar { it.titlecase() }}(")
            val method = field as DClassFile.DClassField.Method
            for ((name, fieldType) in method.type) {
                append("$name: ${fieldType.toFieldValueType().toKotlinType()}, ")
            }
            append("sender: ChannelId? = null) {}\n")
        } else {
            when (field) {
                is DClassFile.DClassField.Simple -> {
                    append("\topen fun onSet${field.name.replaceFirstChar { it.titlecase() }}(new: ${type.toKotlinType()}, sender: ChannelId? = null) {}\n")
                }

                is DClassFile.DClassField.Method -> {
                    append("\topen fun on${field.name.replaceFirstChar { it.titlecase() }}(${field.type[0].first}: ${type.toKotlinType()}, sender: ChannelId? = null) {}\n")
                }
            }
        }
    }
}

private fun StringBuilder.generateClassSpec(index: DClassFileIndex, file: DClassFile) {
    append("val classSpecRepository = ClassSpecRepository.build {\n")
    index.dClasses.forEach {
        val dClass = index.byDClassName[it]!!.second

        append("\tdclass {\n")
        for (field in dClass.fields) {
            append("\t\tfield(${field.toFieldValueType().toTypeCode()})")
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

private fun FieldValue.Type.toTypeCode(): String = when (this) {
    is FieldValue.Type.UInt32 -> "FieldValue.Type.UInt32"
    is FieldValue.Type.String -> "FieldValue.Type.String"
    is FieldValue.Type.Tuple -> {
        StringBuilder().apply {
            append("FieldValue.Type.Tuple(")
            for (type in types) {
                append("${type.toTypeCode()}, ")
            }
            append(")")
        }.toString()
    }

    else -> error("todo $this")
}

private fun FieldValue.Type.toKotlinType(): String = when (this) {
    is FieldValue.Type.UInt32 -> "UInt"
    is FieldValue.Type.String -> "String"
    is FieldValue.Type.Tuple -> error("tuples are not a simple primitive")
    else -> error("todo $this")
}

private fun FieldValue.Type.toDestructureCodePrimitive(): String = when (this) {
    is FieldValue.Type.UInt32 -> "toUInt32()!!"
    is FieldValue.Type.String -> "toStringValue()!!"
    is FieldValue.Type.Tuple -> error("tuples are not a simple primitive")
    else -> error("todo $this")
}

private fun FieldValue.Type.toOnSetCode(method: String, variable: String, linePrepend: String): String =
    when (this) {
        is FieldValue.Type.Tuple -> {
            buildString {
                append("${linePrepend}it.toTuple()!!.let { values ->\n")
                for ((index, type) in types.withIndex()) {
                    append("${linePrepend}\tval t${index} = values[$index].${type.toDestructureCodePrimitive()}\n")
                }
                append("${linePrepend}\t$method(")
                for (i in types.indices) {
                    append("t$i, ")
                }
                append("sender)\n")
                append("${linePrepend}}")
            }
        }

        else -> "${linePrepend}$method($variable.${toDestructureCodePrimitive()}, sender)"
    }
