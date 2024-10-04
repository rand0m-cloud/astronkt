package org.astronkt

import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.plus

abstract class DistributedObjectBase(val doId: DOId, val dclassId: DClassId) {
    abstract val objectFields: Map<FieldId, DistributedField>
    val coroutineScope: CoroutineScope =
        (if (isClient) clientRepository.objectsCoroutineScope else internalRepository.objectsCoroutineScope) +
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

        if (fromNetwork) return

        if (isClient) {
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

fun UInt.toDOId() = DOId(this.toUInt())

fun DOId.toChannelId() = id.toULong().toChannelId()

fun DOId.toFieldValue() = id.toFieldValue()
