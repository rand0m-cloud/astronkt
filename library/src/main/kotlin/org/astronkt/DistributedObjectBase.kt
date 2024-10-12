@file:Suppress("unused", "MemberVisibilityCanBePrivate")

package org.astronkt

import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.plus

abstract class DistributedObjectBase(val doId: DOId, val dclassId: DClassId) {
    abstract val objectFields: Map<FieldId, DistributedField>
    val coroutineScope: CoroutineScope =
        (if (isClient) clientRepository.repositoryCoroutineScope else internalRepository.repositoryCoroutineScope) +
                CoroutineName(
                    "DO-$doId",
                )
    var isAlive: Boolean = true
        private set

    open fun onDelete() {
        isAlive = false
    }

    open fun afterInit() {}

    fun getField(fieldId: FieldId): FieldValue? = objectFields[fieldId]!!.value

    fun setField(
        fieldId: FieldId,
        fieldValue: FieldValue,
        fromNetwork: Boolean = false,
        sender: ChannelId? = null,
    ) {
        objectFields[fieldId]!!.apply {
            value = fieldValue
            onChange.invoke(fieldValue, sender)
        }

        val atoms = objectFields[fieldId]!!.spec.molecular
        if (atoms != null) {
            if (atoms.size == 1) {
                setField(atoms[0], fieldValue, true, sender)
            } else {
                for ((value, id) in fieldValue.toTuple()!!.zip(atoms)) {
                    setField(id, value, true, sender)
                }
            }
        }

        if (fromNetwork) return

        if (isClientAndServer) {
            val doesClient = clientRepository.objects.values.flatten().find { it === this } != null
            val doesServer = internalRepository.objects.values.flatten().find { it === this } != null

            if (!doesClient.xor(doesServer)) {
                error("searching for repository to send update and client is $doesClient and server is $doesServer")
            }

            if (doesClient) {
                clientRepository.sendFieldUpdate(doId, fieldId, fieldValue)
            } else {
                internalRepository.sendFieldUpdate(doId, fieldId, fieldValue)
            }
        } else if (isClient) {
            clientRepository.sendFieldUpdate(doId, fieldId, fieldValue)
        } else {
            internalRepository.sendFieldUpdate(doId, fieldId, fieldValue)
        }
    }
}

open class DistributedField(
    val spec: DistributedFieldSpec,
    var onChange: ((FieldValue, ChannelId?) -> Unit),
    var value: FieldValue? = null,
)

data class DistributedObjectId(val id: UInt)
typealias DOId = DistributedObjectId

fun Int.toDOId() = DOId(this.toUInt())

fun UInt.toDOId() = DOId(this)

fun DOId.toChannelId() = id.toULong().toChannelId()

fun DOId.toFieldValue() = id.toFieldValue()
