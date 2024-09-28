package org.astronkt.client

import kotlinx.coroutines.*
import org.astronkt.*
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.reflect.KClass
import kotlin.reflect.full.primaryConstructor

data class AstronClientRepositoryConfig(
    val serverAddress: String,
)

class AstronClientRepository(
    internal val objectsCoroutineScope: CoroutineScope,
    private val classSpecRepository: ClassSpecRepository,
    val astronClientNetwork: AstronClientNetwork,
    val config: AstronClientRepositoryConfig
) {
    private val objects: MutableMap<DOId, MutableList<DistributedObjectBase>> = mutableMapOf()
    private val objectsDClass: MutableMap<DOId, DClassId> = mutableMapOf()
    val classRepository = ClassRepository()

    internal val activeInterests = mutableSetOf<InterestId>()

    suspend fun launch() {
        astronClientNetwork.connect(config.serverAddress)
        astronClientNetwork.sendMessage(
            AstronClientMessage(
                1U,
                listOf(0xDEADBEEFU.toFieldValue(), "dev".toFieldValue()).toBytes()
            )
        )

        classRepository.uberDogClients().forEach { (id, clazz) ->
            val doInstance = clazz.primaryConstructor!!.call(id) as DistributedObjectBase
            addDO(id, doInstance.dclassId, doInstance)
        }

        // client heartbeat coroutine
        objectsCoroutineScope.launch {
            while (isActive) {
                delay(1000L)
                runCatching {
                    astronClientNetwork.sendMessage(AstronClientMessage(5U, ByteArray(0)))
                }
            }
        }

        (MainScope() + CoroutineName("AstronRepositoryNetwork")).launch {
            astronClientNetwork.networkMessages.collect {
                recieveMessage(it)
            }
        }
    }

    private fun recieveMessage(message: AstronClientMessage) {
        val buf = ByteBuffer.wrap(message.msgData).order(ByteOrder.LITTLE_ENDIAN)
        when (message.msgType) {
            120U.toUShort() -> {
                //set field
                val doId = FieldValue.Type.UInt32.read(buf).toDOId()
                val fieldId = FieldValue.Type.UInt16.read(buf).toFieldId()
                val fieldSpec = classSpecRepository.byFieldId[fieldId]
                    ?: throw IllegalStateException("don't have a field id spec for $fieldId")
                val value = fieldSpec.type.readValue(buf)

                receiveFieldUpdate(doId, fieldId, value)
            }

            142U.toUShort() -> {
                //enter object
                val doId = FieldValue.Type.UInt32.read(buf).toDOId()
                val parentId = FieldValue.Type.UInt32.read(buf)
                val zoneId = FieldValue.Type.UInt32.read(buf).toZoneId()
                val dclassId = FieldValue.Type.UInt16.read(buf).toDClassId()
                val fields = classSpecRepository.getRequiredFieldIds(dclassId).map {
                    it to classSpecRepository.byFieldId[it]!!.type.readValue(buf)
                }

                val dClassName by lazy { classSpecRepository.byDClassId[dclassId] }
                addDO(
                    doId,
                    dclassId,
                    *classRepository.classesForDClass(dclassId).map {
                        val doObject: DistributedObjectBase = it.primaryConstructor?.call(doId) as DistributedObjectBase
                        for ((fieldId, value) in fields) {
                            println("(${doId.id}) setting field: ${classSpecRepository.byFieldId[fieldId]!!.name} to $value")
                            doObject.setField(fieldId, value, fromNetwork = true)
                        }
                        doObject
                    }.toTypedArray()
                )
            }

            173U.toUShort() -> {
                //enter object other w/ownership
                val doId = FieldValue.Type.UInt32.read(buf).toDOId()
                val parentId = FieldValue.Type.UInt32.read(buf)
                val zoneId = FieldValue.Type.UInt32.read(buf).toZoneId()
                val dclassId = FieldValue.Type.UInt16.read(buf).toDClassId()
                val fields = classSpecRepository.getRequiredFieldIds(dclassId).map {
                    it to classSpecRepository.byFieldId[it]!!.type.readValue(buf).also { value ->
                        Unit
                    }
                }

                val otherFields = mutableListOf<Pair<FieldId, FieldValue>>()
                while (buf.hasRemaining()) {
                    val fieldId = FieldValue.Type.UInt16.read(buf).toFieldId()
                    val fieldValue = classSpecRepository.byFieldId[fieldId]!!.type.readValue(buf)
                    otherFields += fieldId to fieldValue
                }

                val dClassName by lazy { classSpecRepository.byDClassId[dclassId] }
                addDO(
                    doId,
                    dclassId,
                    *classRepository.classesForDClass(dclassId).map {
                        val doObject: DistributedObjectBase = it.primaryConstructor?.call(doId) as DistributedObjectBase
                        for ((fieldId, value) in fields + otherFields) {
                            println("(${doId.id}) setting field: ${classSpecRepository.byFieldId[fieldId]!!.name} to $value")
                            doObject.setField(fieldId, value, fromNetwork = true)
                        }
                        doObject
                    }.toTypedArray()
                )
            }

            204U.toUShort() -> {
                //add interest resp
                FieldValue.Type.UInt32.read(buf)
                activeInterests.add(FieldValue.Type.UInt16.read(buf).toInterestId())
            }
        }
    }

    private fun addDO(doId: DOId, dClassId: DClassId, vararg doObject: DistributedObjectBase) {
        if (doObject.isEmpty()) println("No classes registered for (${dClassId.id}) ${classSpecRepository.byDClassId[dClassId]}")

        objects.getOrPut(doId) { mutableListOf() } += doObject
        objectsDClass[doId] = dClassId
    }

    fun registerClass(dclassId: DClassId, vararg clazz: KClass<*>) = classRepository.registerClass(dclassId, *clazz)

    fun sendFieldUpdate(doId: DOId, fieldId: FieldId, value: FieldValue) {
        astronClientNetwork.sendMessage(
            AstronClientMessage(
                120U,
                listOf(doId.toFieldValue(), fieldId.toFieldValue(), value).toBytes()
            )
        )
    }

    fun receiveFieldUpdate(doId: DOId, fieldId: FieldId, value: FieldValue) {
        val classes = objects[doId] ?: return

        println("(${doId.id}) setting field: ${classSpecRepository.byFieldId[fieldId]!!.name} to $value")

        classes.forEach {
            it.setField(fieldId, value, true)
        }
    }
}

fun AstronClientRepository.sendInterestRequest(interestId: InterestId, parentId: DOId, zoneId: ZoneId) {
    astronClientNetwork.sendMessage(
        AstronClientMessage(
            200U,
            listOf(
                0U.toFieldValue(),
                interestId.id.toFieldValue(),
                parentId.toFieldValue(),
                zoneId.toFieldValue()
            ).toBytes()
        )
    )
}