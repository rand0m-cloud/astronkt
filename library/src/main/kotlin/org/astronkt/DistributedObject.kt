package org.astronkt

import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.plus

abstract class DistributedObject(val doId: DOId, val dclassId: DClassId) {
    abstract val startingFieldId: UShort
    abstract val objectFields: List<DistributedField>
    val coroutineScope: CoroutineScope =
        (if (isClient) clientRepository.objectsCoroutineScope else internalRepository.objectsCoroutineScope) + CoroutineName(
            "DO-$doId"
        )
    var isAlive: Boolean = true
        private set

    open fun onDelete() {
        isAlive = false
    }

    fun getField(fieldId: FieldId): FieldValue? = objectFields[(fieldId.id - startingFieldId).toInt()].value

    fun setField(fieldId: FieldId, fieldValue: FieldValue, fromNetwork: Boolean = false, sender: ChannelId? = null) {
        objectFields[(fieldId.id - startingFieldId).toInt()].apply {
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
) {
}

data class DistributedObjectId(val id: UInt)
typealias DOId = DistributedObjectId

fun Int.toDOId() = DOId(this.toUInt())
fun UInt.toDOId() = DOId(this.toUInt())
fun DOId.toChannelId() = id.toULong().toChannelId()
fun DOId.toFieldValue() = id.toFieldValue()
