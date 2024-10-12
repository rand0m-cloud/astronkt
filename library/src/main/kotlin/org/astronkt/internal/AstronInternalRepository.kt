@file:Suppress("unused", "MemberVisibilityCanBePrivate")

package org.astronkt.internal

import kotlinx.coroutines.*
import org.astronkt.*
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.reflect.KClass
import kotlin.reflect.full.primaryConstructor

data class AstronInternalRepositoryConfig(
    val serverAddress: String,
    val repoControlId: ChannelId,
    val stateServerControl: ChannelId,
    val repositoryCoroutineScope: CoroutineScope = MainScope()
)

class AstronInternalRepository(
    internal val repositoryCoroutineScope: CoroutineScope,
    private val classSpecRepository: ClassSpecRepository,
    val astronInternalNetwork: AstronInternalNetwork,
    val config: AstronInternalRepositoryConfig,
) {
    val objects: MutableMap<DOId, MutableList<DistributedObjectBase>> = mutableMapOf()
    val classRepository = ClassRepository(isServer = true)

    fun sendFieldUpdate(doId: DOId, fieldId: FieldId, value: FieldValue) {
        astronInternalNetwork.sendMessage(
            AstronInternalMessage(
                listOf(doId.toChannelId()),
                config.repoControlId,
                2020U,
                listOf(doId.toFieldValue(), fieldId.toFieldValue(), value).toBytes()
            )
        )
    }

    fun receiveFieldUpdate(doId: DOId, fieldId: FieldId, value: FieldValue, sender: ChannelId) {
        val classes = objects[doId] ?: return
        classes.forEach {
            it.setField(fieldId, value, true, sender = sender)
        }
    }

    fun launch() {
        astronInternalNetwork.connect(config.serverAddress)
        astronInternalNetwork.sendMessage(
            AstronInternalMessage.controlMessage(
                9000U,
                config.repoControlId.toFieldValue().toBytes()
            )
        )
        classRepository.uberDogs().forEach { (id, clazz) ->
            astronInternalNetwork.sendMessage(
                AstronInternalMessage.controlMessage(
                    9000U,
                    id.toChannelId().toFieldValue().toBytes()
                )
            )
            val doInstance = clazz.primaryConstructor!!.call(id) as DistributedObjectBase
            addDO(id, doInstance)
        }

        (repositoryCoroutineScope + CoroutineName("AstronRepositoryNetwork")).launch {
            astronInternalNetwork.networkMessages.collect {
                recieveMessage(it)
            }
        }
    }

    suspend fun awaitRepositoryClose(): Unit = repositoryCoroutineScope.coroutineContext.job.join()

    fun closeRepository() {
        objects.values.flatten().forEach {
            it.onDelete()
        }
        repositoryCoroutineScope.coroutineContext.job.cancel()
    }

    private fun recieveMessage(message: AstronInternalMessage) {
        val buf = ByteBuffer.wrap(message.msgData).order(ByteOrder.LITTLE_ENDIAN)
        if (message.msgType == 2020U.toUShort()) {
            val doId = FieldValue.Type.UInt32.read(buf).toDOId()
            val fieldId = FieldValue.Type.UInt16.read(buf).toFieldId()
            val value = classSpecRepository.byFieldId[fieldId]!!.type.readValue(buf)

            receiveFieldUpdate(doId, fieldId, value, message.sender)
        }
    }

    fun createDO(
        doId: DOId,
        parentId: UInt,
        zoneId: ZoneId,
        dclassId: DClassId,
        required: List<FieldValue>? = null,
        other: List<FieldValue>? = null
    ) {
        val requiredFieldId = classSpecRepository.getRequiredFieldIds(dclassId)
        assert(
            requiredFieldId.size == (required?.size ?: 0)
        ) { "dclass $dclassId expected ${requiredFieldId.size} but got ${required?.size} required fields" }

        val newDOs = classRepository.classesForDClass(dclassId).map {
            (it.primaryConstructor!!.call(doId) as DistributedObjectBase)
        }

        addDO(doId, *newDOs.toTypedArray())

        newDOs.forEach {
            if (required != null) {
                requiredFieldId.zip(required).forEach { (id, value) ->
                    it.setField(id, value)
                }
            }
            // TODO: handle other
            it.afterInit()
        }

        astronInternalNetwork.sendMessage(
            AstronInternalMessage(
                listOf(config.stateServerControl),
                config.repoControlId,
                2000U,
                mutableListOf(
                    doId.id.toFieldValue(),
                    parentId.toFieldValue(),
                    zoneId.toFieldValue(),
                    dclassId.id.toFieldValue(),
                ).apply {
                    required?.let { addAll(it) }
                    other?.let { addAll(it) }
                }.toBytes()
            )
        )
    }

    fun registerClass(dclassId: DClassId, vararg clazz: KClass<*>) = classRepository.registerClass(dclassId, *clazz)

    private fun addDO(doId: DOId, vararg doObject: DistributedObjectBase) {
        objects.getOrPut(doId) { mutableListOf() } += doObject
    }
}

fun AstronInternalRepository.sendFieldUpdateToClient(
    recipient: ChannelId,
    doId: DOId,
    fieldId: FieldId,
    fieldValue: FieldValue
) = astronInternalNetwork.sendMessage(
    AstronInternalMessage(
        listOf(recipient),
        config.repoControlId,
        2020U,
        listOf(doId.toFieldValue(), fieldId.toFieldValue(), fieldValue).toBytes()
    )
)

enum class ClientCAState(val value: UShort) {
    New(0U),
    Anonymous(1U),
    Established(2U)
}

fun AstronInternalRepository.setClientCAState(client: ChannelId, state: ClientCAState) {
    astronInternalNetwork.sendMessage(
        AstronInternalMessage(
            listOf(client),
            config.repoControlId,
            1000U,
            state.value.toFieldValue().toBytes()
        )
    )
}