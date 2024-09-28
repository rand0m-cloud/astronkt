package org.astronkt

data class DistributedFieldModifiers(
    val ram: Boolean = false,
    val required: Boolean = false,
    val db: Boolean = false,
    val airecv: Boolean = false,
    val ownrecv: Boolean = false,
    val clrecv: Boolean = false,
    val broadcast: Boolean = false,
    val ownsend: Boolean = false,
    val clsend: Boolean = false,
)

data class DistributedFieldSpec(
    val type: FieldValue.Type,
    val name: String,
    val modifiers: DistributedFieldModifiers = DistributedFieldModifiers(),
    val default: FieldValue? = null,
)

data class DClassId(val id: UShort)

data class FieldId(val id: UShort)

fun UShort.toDClassId() = DClassId(this)

fun UInt.toDClassId() = toUShort().toDClassId()

fun DClassId.toFieldValue() = id.toFieldValue()

fun UShort.toFieldId() = FieldId(this)

fun UInt.toFieldId() = toUShort().toFieldId()

fun FieldId.toFieldValue() = id.toFieldValue()

sealed class DistributedSpec {
    abstract val name: String
    abstract val fields: List<DistributedFieldSpec>
    abstract val isDClass: Boolean

    data class DistributedClassSpec(
        override val fields: List<DistributedFieldSpec>,
        val extends: List<String>?,
        override val name: String,
    ) : DistributedSpec() {
        override val isDClass: Boolean = true
    }

    data class DistributedStructSpec(
        override val fields: List<DistributedFieldSpec>,
        override val name: String,
    ) : DistributedSpec() {
        override val isDClass: Boolean = false
    }
}

class ClassSpecRepository(val classes: List<DistributedSpec>) {
    val byFieldId: Map<FieldId, DistributedFieldSpec> =
        classes.flatMap { it.fields }.withIndex().associate { it.index.toUShort().toFieldId() to it.value }

    val byDClassId: Map<DClassId, Pair<DistributedSpec.DistributedClassSpec, List<FieldId>>> =
        mutableMapOf<DClassId, Pair<DistributedSpec.DistributedClassSpec, List<FieldId>>>().also {
            var dClassId = 0U.toUShort()
            var fieldId = 0U.toUShort()

            for (clazz in classes) {
                val name = clazz.name
                val fields =
                    clazz.fields.map {
                        fieldId.toFieldId().also {
                            fieldId++
                        }
                    }

                when (clazz) {
                    is DistributedSpec.DistributedClassSpec -> it[dClassId.toDClassId()] = clazz to fields
                    else -> {}
                }

                dClassId++

            }
        }

    val byDClassName: Map<String, DClassId> = byDClassId.entries.associate { (dClass, value) ->
        value.first.name to dClass
    }

    companion object {
        class Builder(val classes: MutableList<DistributedSpec> = mutableListOf()) {
            class DClassDsl(internal val fields: MutableList<DistributedFieldSpec> = mutableListOf()) {
                fun field(
                    name: String,
                    type: FieldValue.Type,
                    block: DFieldDsl.() -> Unit = {},
                ) {
                    fields.add(
                        DistributedFieldSpec(type, name, modifiers = DFieldDsl().apply(block).modifiers),
                    )
                }

                fun molecular(name: String, vararg atoms: FieldValue.Type) {
                    fields.add(
                        DistributedFieldSpec(FieldValue.Type.Tuple(*atoms), name),
                    )
                }
            }

            class DFieldDsl(internal var modifiers: DistributedFieldModifiers = DistributedFieldModifiers()) {
                fun required() {
                    modifiers = modifiers.copy(required = true)
                }

                fun ram() {
                    modifiers = modifiers.copy(ram = true)
                }

                fun db() {
                    modifiers = modifiers.copy(db = true)
                }

                fun broadcast() {
                    modifiers = modifiers.copy(broadcast = true)
                }

                fun ownsend() {
                    modifiers = modifiers.copy(ownsend = true)
                }

                fun ownrecv() {
                    modifiers = modifiers.copy(ownrecv = true)
                }

                fun clsend() {
                    modifiers = modifiers.copy(clsend = true)
                }

                fun clrecv() {
                    modifiers = modifiers.copy(clrecv = true)
                }

                fun airecv() {
                    modifiers = modifiers.copy(airecv = true)
                }
            }

            fun dclass(
                name: String,
                extends: List<String>? = null,
                block: DClassDsl.() -> Unit,
            ) {
                classes.add(
                    DistributedSpec.DistributedClassSpec(
                        DClassDsl().apply(block).fields,
                        extends,
                        name
                    ),
                )
            }

            class StructDsl(internal val fields: MutableList<DistributedFieldSpec> = mutableListOf()) {
                fun field(name: String, type: FieldValue.Type) {
                    fields.add(DistributedFieldSpec(type, name))
                }
            }

            fun struct(name: String, block: StructDsl.() -> Unit) {
                classes.add(DistributedSpec.DistributedStructSpec(StructDsl().apply(block).fields, name))
            }
        }

        fun build(block: Builder.() -> Unit): ClassSpecRepository = ClassSpecRepository(Builder().apply(block).classes)
    }

    fun getRequiredFieldIds(dClass: DClassId): List<FieldId> {
        val selfFields = byDClassId[dClass]!!
            .second.filter {
                byFieldId[it]!!.modifiers.required
            }
        val parents = byDClassId[dClass]!!.first.extends ?: listOf()
        val fields = parents.flatMap {
            getRequiredFieldIds(byDClassName[it]!!)
        } + selfFields
        return fields.sortedBy { it.id }
    }
}
