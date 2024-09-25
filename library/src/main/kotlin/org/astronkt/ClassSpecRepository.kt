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
    abstract val fields: List<DistributedFieldSpec>
    abstract val isDClass: Boolean

    data class DistributedClassSpec(
        override val fields: List<DistributedFieldSpec>,
        val extends: List<DistributedClassSpec>?,
    ) : DistributedSpec() {
        override val isDClass: Boolean = true
    }

    data class DistributedStructSpec(override val fields: List<DistributedFieldSpec>) : DistributedSpec() {
        override val isDClass: Boolean = false
    }
}

class ClassSpecRepository(val classes: List<DistributedSpec>) {
    val byFieldId: Map<FieldId, DistributedFieldSpec> =
        classes.flatMap { it.fields }.withIndex().associate { it.index.toUShort().toFieldId() to it.value }

    val byDClassId: Map<DClassId, List<FieldId>> =
        mutableMapOf<DClassId, List<FieldId>>().also {
            var dClassId = 0U.toUShort()
            var fieldId = 0U.toUShort()
            for (clazz in classes) {
                val fields =
                    clazz.fields.map {
                        fieldId.toFieldId().also {
                            fieldId++
                        }
                    }

                if (!clazz.isDClass) continue

                it.put(
                    dClassId.toDClassId(),
                    fields,
                )

                dClassId++
            }
        }

    companion object {
        class Builder(val classes: MutableList<DistributedSpec> = mutableListOf()) {
            class DClassDsl(internal val fields: MutableList<DistributedFieldSpec> = mutableListOf()) {
                fun field(
                    type: FieldValue.Type,
                    block: DFieldDsl.() -> Unit = {},
                ) {
                    fields.add(
                        DistributedFieldSpec(type, modifiers = DFieldDsl().apply(block).modifiers),
                    )
                }

                fun molecular(vararg atoms: FieldValue.Type) {
                    fields.add(
                        DistributedFieldSpec(FieldValue.Type.Tuple(*atoms)),
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
                extends: List<DistributedSpec.DistributedClassSpec>? = null,
                block: DClassDsl.() -> Unit,
            ) {
                classes.add(
                    DistributedSpec.DistributedClassSpec(
                        DClassDsl().apply(block).fields,
                        extends,
                    ),
                )
            }

            class StructDsl(internal val fields: MutableList<DistributedFieldSpec> = mutableListOf()) {
                fun field(type: FieldValue.Type) {
                    fields.add(DistributedFieldSpec(type))
                }
            }

            fun struct(block: StructDsl.() -> Unit) {
                classes.add(DistributedSpec.DistributedStructSpec(StructDsl().apply(block).fields))
            }
        }

        fun build(block: Builder.() -> Unit): ClassSpecRepository = ClassSpecRepository(Builder().apply(block).classes)
    }

    fun getRequiredFieldIds(dClass: DClassId): List<FieldId> =
        byDClassId[dClass]!!
            .filter {
                byFieldId[it]!!.modifiers.required
            }
}
