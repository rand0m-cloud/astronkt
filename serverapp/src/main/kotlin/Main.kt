package org.astronkt.server.app

import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.astronkt.*
import org.astronkt.internal.AstronInternalRepositoryConfig
import org.astronkt.internal.ClientCAState
import org.astronkt.internal.sendFieldUpdateToClient
import org.astronkt.internal.setClientCAState

open class Block(doId: DOId) : DistributedObject(doId, 0U.toDClassId()) {
    companion object {
        val dClassId = 0U.toDClassId()

        object Fields {
            val color: FieldId = 0U.toFieldId()
            val x: FieldId = 1U.toFieldId()
            val y: FieldId = 2U.toFieldId()
            val z: FieldId = 3U.toFieldId()
        }
    }

    override val startingFieldId: UShort = 0U

    override val objectFields: List<DistributedField> = listOf(
        DistributedField(
            DistributedFieldSpec(
                FieldValue.Type.UInt32,
                modifiers = DistributedFieldModifiers(
                    required = true,
                    broadcast = true
                ),
            ),
            onChange = { it, sender ->
                onSetColor(it.toUInt32()!!, sender)
            }
        ),
        DistributedField(
            DistributedFieldSpec(
                FieldValue.Type.UInt32,
                modifiers = DistributedFieldModifiers(
                    required = true,
                    broadcast = true
                ),
            ),
            onChange = { it, sender ->
                onSetX(it.toUInt32()!!, sender)
            }
        ),
        DistributedField(
            DistributedFieldSpec(
                FieldValue.Type.UInt32,
                modifiers = DistributedFieldModifiers(
                    required = true,
                    broadcast = true
                ),
            ),
            onChange = { it, sender ->
                onSetY(it.toUInt32()!!, sender)
            }
        ),
        DistributedField(
            DistributedFieldSpec(
                FieldValue.Type.UInt32,
                modifiers = DistributedFieldModifiers(
                    required = true,
                    broadcast = true
                ),
            ),
            onChange = { it, sender ->
                onSetZ(it.toUInt32()!!, sender)
            }
        ),
    )


    var color: UInt
        get() = getField(0U.toFieldId())?.toUInt32()!!
        set(value) = setField(0U.toFieldId(), value.toFieldValue())

    var x: UInt
        get() = getField(1U.toFieldId())?.toUInt32()!!
        private set(value) = setField(1U.toFieldId(), value.toFieldValue())

    var y: UInt
        get() = getField(2U.toFieldId())?.toUInt32()!!
        set(value) = setField(2U.toFieldId(), value.toFieldValue())

    var z: UInt
        get() = getField(3U.toFieldId())?.toUInt32()!!
        set(value) = setField(3U.toFieldId(), value.toFieldValue())

    open fun onSetColor(new: UInt, sender: ChannelId? = null) {}
    open fun onSetX(new: UInt, sender: ChannelId? = null) {}
    open fun onSetY(new: UInt, sender: ChannelId? = null) {}
    open fun onSetZ(new: UInt, sender: ChannelId? = null) {}
}

open class LoginManager(doId: DOId) : DistributedObject(doId, 1U.toDClassId()) {
    companion object {
        val dClassId = 1U.toDClassId()
    }

    object Fields {
        val login: FieldId = 4U.toFieldId()
        val loginResponse: FieldId = 5U.toFieldId()
    }

    override val startingFieldId: UShort = 4U

    override val objectFields: List<DistributedField> = listOf(
        DistributedField(
            DistributedFieldSpec(
                FieldValue.Type.Tuple(FieldValue.Type.String, FieldValue.Type.String),
                modifiers = DistributedFieldModifiers(clsend = true)
            ),
            onChange = { it, sender ->
                it.toTuple()!!.let { values ->
                    val username = values[0].toStringValue()!!
                    val password = values[1].toStringValue()!!
                    onLogin(username, password, sender)
                }
            }),
        DistributedField(DistributedFieldSpec(FieldValue.Type.String), onChange = { it, sender ->
            onLoginResponse(it.toStringValue()!!, sender)
        })
    )

    open fun login(username: String, password: String) {
        setField(4U.toFieldId(), FieldValue.TupleValue(username.toFieldValue(), password.toFieldValue()))
    }

    open fun onLogin(username: String, password: String, sender: ChannelId? = null) {}

    open fun loginResponse(status: String) {
        setField(5U.toFieldId(), status.toFieldValue())
    }

    open fun onLoginResponse(status: String, sender: ChannelId? = null) {}

}

@DClassClient
class BlockClient(doId: DOId) : Block(doId) {
    override fun onSetColor(new: UInt, sender: ChannelId?) {
        println("changed color to $new")
    }
}

@DClassAI
class BlockAI(doId: DOId) : Block(doId) {
    init {
        coroutineScope.launch {
            while (isAlive) {
                delay(500L)
                color += 1.toUInt()
            }
        }
    }
}

@DClassUberDOG(4000U)
class LoginManagerUD(doId: DOId) : LoginManager(doId) {
    override fun onLogin(username: String, password: String, sender: ChannelId?) {
        super.onLogin(username, password, sender)

        println("login received for $username, $password")

        val success = username == "username"
        val resp = if (success) {
            "success"
        } else {
            "failure"
        }

        internalRepository.sendFieldUpdateToClient(
            sender!!,
            doId,
            LoginManager.Fields.loginResponse,
            resp.toFieldValue()
        )

        if (success) {
            internalRepository.setClientCAState(sender!!, ClientCAState.Established)
        }
    }
}

@DClassClient
class LoginManagerClient(doId: DOId) : LoginManager(doId) {
    override fun onLoginResponse(status: String, sender: ChannelId?) {
        super.onLoginResponse(status, sender)
        println("login resp: $status")
    }
}

suspend fun main() {
    val classSpecRepository = ClassSpecRepository.build {
        dclass {
            field(FieldValue.Type.UInt32) {
                required()
                broadcast()
            }
            field(FieldValue.Type.UInt32) {
                required()
                broadcast()
            }
            field(FieldValue.Type.UInt32) {
                required()
                broadcast()
            }
            field(FieldValue.Type.UInt32) {
                required()
                broadcast()
            }
        }

        dclass {
            field(FieldValue.Type.Tuple(FieldValue.Type.String, FieldValue.Type.String)) {
                clsend()
            }
            field(FieldValue.Type.String)
        }

        dclass {}
    }

    setupAstronInternalRepository(
        classSpecRepository,
        AstronInternalRepositoryConfig(
            "127.0.0.1:7199",
            10000U.toChannelId(),
            4002U.toChannelId()
        ),
    )

    MainScope().launch {
        internalRepository.astronInternalNetwork.networkMessages.collect {
            println(it)
        }
    }

    internalRepository.registerClass(Block.dClassId, BlockAI::class, BlockClient::class)
    internalRepository.registerClass(
        LoginManager.dClassId,
        LoginManagerUD::class,
        LoginManagerClient::class,
    )

    internalRepository.launch()

    internalRepository.createDO(
        1000U.toDOId(),
        0U,
        1000U.toZoneId(),
        2U.toDClassId(),
        listOf(0U.toFieldValue(), 0U.toFieldValue(), 0U.toFieldValue())
    )

    internalRepository.createDO(
        5000U.toDOId(),
        1000U,
        1000U.toZoneId(),
        Block.dClassId,
        listOf(0U.toFieldValue(), 0U.toFieldValue(), 0U.toFieldValue(), 0U.toFieldValue())
    )


    println("astron launched")
}