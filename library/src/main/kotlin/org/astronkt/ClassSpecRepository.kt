@file:Suppress("unused", "MemberVisibilityCanBePrivate")

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
    val molecular: List<FieldId>? = null,
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

    val byDClassName: Map<String, DClassId> =
        byDClassId.entries.associate { (dClass, value) ->
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

                fun molecular(
                    name: String,
                    atomFields: List<FieldId>,
                    vararg atoms: FieldValue.Type,
                ) {
                    fields.add(
                        DistributedFieldSpec(FieldValue.Type.Tuple(*atoms), name, molecular = atomFields),
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
                        name,
                    ),
                )
            }

            class StructDsl(internal val fields: MutableList<DistributedFieldSpec> = mutableListOf()) {
                fun field(
                    name: String,
                    type: FieldValue.Type,
                ) {
                    fields.add(DistributedFieldSpec(type, name))
                }
            }

            fun struct(
                name: String,
                block: StructDsl.() -> Unit,
            ) {
                classes.add(DistributedSpec.DistributedStructSpec(StructDsl().apply(block).fields, name))
            }
        }

        fun build(block: Builder.() -> Unit): ClassSpecRepository = ClassSpecRepository(Builder().apply(block).classes)
    }

    fun getFieldIds(dClassName: String): List<FieldId> = getFieldIds(byDClassName[dClassName]!!)

    fun getFieldIds(dClass: DClassId): List<FieldId> = byDClassId[dClass]!!.second

    private fun FieldId.isRequired(): Boolean = byFieldId[this]!!.modifiers.required

    /* TODO:
     */
    fun getRequiredFieldIds(
        dClass: DClassId,
        toClient: Boolean = false,
        isOwner: Boolean = false,
    ): List<FieldId> {
        val selfFields =
            getFieldIds(dClass)
        val parents = byDClassId[dClass]!!.first.extends.orEmpty()
        val fields =
            (
                    parents.flatMap { name ->
                        val parentDClassId = byDClassName[name]!!
                        val parentSelf = getFieldIds(parentDClassId)
                        parentSelf + byDClassId[parentDClassId]!!.first.extends.orEmpty().flatMap { getFieldIds(it) }
                    } + selfFields
                    ).sortedBy { it.id }.toMutableList()

        val duplicates = mutableSetOf<FieldId>()
        val fieldName = mutableMapOf<String, FieldId>()
        for (id in fields) {
            val name = byFieldId[id]!!.name

            val old = fieldName.put(name, id)
            if (old != null) {
                duplicates.add(old)
            }
        }

        fields.removeIf {
            if (duplicates.contains(it)) return@removeIf true

            val field = byFieldId[it]!!
            val isRequired = field.modifiers.required
            val isNotMolecular = field.molecular == null
            val isClientOnlyAllowed =
                !toClient ||
                        field.modifiers.broadcast ||
                        field.modifiers.clrecv ||
                        (isOwner && field.modifiers.ownrecv)

            !(isRequired && isNotMolecular && isClientOnlyAllowed)
        }

        return fields
    }
}
