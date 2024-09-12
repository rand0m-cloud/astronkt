package org.astronkt.dclassmacro

import org.astronkt.*

data class DClassFile(val decls: List<TypeDecl>) {
    sealed class TypeDecl {
        data class DClass(val name: String, val fields: List<DClassField>) : TypeDecl()
    }

    sealed class DClassField {
        abstract val name: String
        abstract val modifiers: List<DClassFieldModifier>

        data class Simple(
            override val name: String,
            val type: DClassFieldType,
            override val modifiers: List<DClassFieldModifier>
        ) : DClassField()

        data class Method(
            override val name: String,
            val type: List<Pair<String, DClassFieldType>>,
            override val modifiers: List<DClassFieldModifier>
        ) : DClassField()
    }

    sealed class DClassFieldModifier {
        data object Required : DClassFieldModifier()
        data object Broadcast : DClassFieldModifier()
        data object ClSend : DClassFieldModifier()
    }

    sealed class DClassFieldType {
        data object String : DClassFieldType()
        data object UInt32 : DClassFieldType()
    }
}

data class DClassFileIndex(
    val dClasses: List<String>,
    val byDClassName: Map<String, Pair<DClassId, DClassFile.TypeDecl.DClass>>,
    val byDClassFieldName: Map<Pair<String, String>, Pair<FieldId, DClassFile.DClassField>>
) {
    fun getFieldId(dClassName: String, fieldName: String): FieldId = byDClassFieldName[dClassName to fieldName]!!.first
    fun firstFieldId(dClassName: String): FieldId {
        val dClass = byDClassName[dClassName]!!.second
        return dClass.fields.getOrNull(0)?.name?.let {
            byDClassFieldName[dClassName to it]!!.first
        } ?: FieldId(0U)
    }
}

fun DClassFile.buildIndex(): DClassFileIndex {
    var dClassId = 0U
    var fieldId = 0U

    val dClasses = mutableListOf<String>()
    val byDClassName = mutableMapOf<String, Pair<DClassId, DClassFile.TypeDecl.DClass>>()
    val byDClassFieldName = mutableMapOf<Pair<String, String>, Pair<FieldId, DClassFile.DClassField>>()

    for (decl in decls) {
        when (decl) {
            is DClassFile.TypeDecl.DClass -> {
                dClasses.add(decl.name)
                byDClassName[decl.name] = dClassId.toDClassId() to decl

                for (field in decl.fields) {
                    byDClassFieldName[decl.name to field.name] = fieldId.toFieldId() to field
                    fieldId += 1U
                }

                dClassId += 1U
            }
        }
    }

    return DClassFileIndex(dClasses, byDClassName, byDClassFieldName)
}

fun DClassFile.DClassField.toFieldValueType(): FieldValue.Type {
    return when (this) {
        is DClassFile.DClassField.Simple -> {
            type.toFieldValueType()
        }

        is DClassFile.DClassField.Method -> {
            val types = type.map { it.second.toFieldValueType() }
            if (types.size == 1) types[0] else FieldValue.Type.Tuple(*types.toTypedArray())
        }
    }
}

fun DClassFile.DClassFieldType.toFieldValueType(): FieldValue.Type {
    return when (this) {
        is DClassFile.DClassFieldType.UInt32 -> FieldValue.Type.UInt32
        is DClassFile.DClassFieldType.String -> FieldValue.Type.String
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
            }
        }
    )
}