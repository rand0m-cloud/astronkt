package org.astronkt.client.app

import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.astronkt.*
import org.astronkt.client.AstronClientRepositoryConfig
import org.astronkt.client.sendInterestRequest

@Suppress("unused")
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
                    ownsend = true,
                    broadcast = true,
                )
            ),
            onChange = { it, sender ->
                onSetColor(it.toUInt32()!!, sender)
            }
        ),
        DistributedField(
            DistributedFieldSpec(
                FieldValue.Type.UInt32,
                modifiers = DistributedFieldModifiers(
                    ownsend = true,
                    broadcast = true,
                )
            ),
            onChange = { it, sender ->
                onSetX(it.toUInt32()!!, sender)
            }
        ),
        DistributedField(
            DistributedFieldSpec(
                FieldValue.Type.UInt32,
                modifiers = DistributedFieldModifiers(
                    ownsend = true,
                    broadcast = true,
                )
            ),
            onChange = { it, sender ->
                onSetY(it.toUInt32()!!, sender)
            }
        ),
        DistributedField(
            DistributedFieldSpec(
                FieldValue.Type.UInt32,
                modifiers = DistributedFieldModifiers(
                    ownsend = true,
                    broadcast = true,
                )
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
        set(value) = setField(1U.toFieldId(), value.toFieldValue())

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

@Suppress("unused")
open class LoginManager(doId: DOId) : DistributedObject(doId, 1U.toDClassId()) {
    companion object {
        val dClassId = 1U.toDClassId()

        object Fields {
            val login: FieldId = 4U.toFieldId()
            val loginResponse: FieldId = 5U.toFieldId()
        }
    }

    override val startingFieldId: UShort = 4U
    override val objectFields: List<DistributedField> = listOf(
        DistributedField(
            DistributedFieldSpec(
                FieldValue.Type.Tuple(FieldValue.Type.String, FieldValue.Type.String),
                modifiers = DistributedFieldModifiers(
                    clsend = true,
                )
            ),
            onChange = { it, sender ->
                it.toTuple()!!.let { values ->
                    val t0 = values[0].toStringValue()!!
                    val t1 = values[1].toStringValue()!!
                    onLogin(t0, t1, sender)
                }
            }
        ),
        DistributedField(
            DistributedFieldSpec(
                FieldValue.Type.String,
                modifiers = DistributedFieldModifiers(
                )
            ),
            onChange = { it, sender ->
                onLoginResponse(it.toStringValue()!!, sender)
            }
        ),
    )

    fun login(username: String, password: String) {
        setField(4U.toFieldId(), FieldValue.TupleValue(username.toFieldValue(), password.toFieldValue()))
    }

    fun loginResponse(status: String) {
        setField(5U.toFieldId(), status.toFieldValue())
    }

    open fun onLogin(username: String, password: String, sender: ChannelId? = null) {}
    open fun onLoginResponse(status: String, sender: ChannelId? = null) {}
}

@Suppress("unused")
open class Empty(doId: DOId) : DistributedObject(doId, 2U.toDClassId()) {
    companion object {
        val dClassId = 2U.toDClassId()

        object Fields {
        }
    }

    override val startingFieldId: UShort = 0U
    override val objectFields: List<DistributedField> = listOf(
    )

}


@DClassClientUberDOG(4000U)
class LoginManagerClient(doId: DOId) : LoginManager(doId) {
    init {
        login("username", "password")
    }

    override fun onLoginResponse(status: String, sender: ChannelId?) {
        super.onLoginResponse(status, sender)
        println("got login response: $status")
        if (status == "success") {
            clientRepository.sendInterestRequest(1U.toInterestId(), 1000U.toDOId(), 1000U.toZoneId())
        }
    }
}

@DClassClient
class BlockClient(doId: DOId) : Block(doId) {
    override fun onSetColor(new: UInt, sender: ChannelId?) {
        super.onSetColor(new, sender)
        println("set color to $new")
    }
}

suspend fun main() {
    setupAstronClientRepository(
        ClassSpecRepository.build {
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
        },
        AstronClientRepositoryConfig(
            "localhost:7198"
        )
    )
    clientRepository.registerClass(LoginManager.dClassId, LoginManagerClient::class)
    clientRepository.registerClass(Block.dClassId, BlockClient::class)

    MainScope().launch {
        clientRepository.astronClientNetwork.networkMessages.collect {
            println(it)
        }
    }

    clientRepository.launch()

    println("astron launched")
}