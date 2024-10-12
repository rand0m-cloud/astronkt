package org.astronkt.sample

import GameSpec.Authenticator
import GameSpec.Cube
import GameSpec.GameRoot
import GameSpec.classSpecRepository
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.astronkt.*
import org.astronkt.client.AstronClientRepositoryConfig
import org.astronkt.client.awaitFieldSet
import org.astronkt.client.sendInterestRequest
import org.astronkt.internal.AstronInternalRepositoryConfig
import org.astronkt.internal.ClientCAState
import org.astronkt.internal.sendFieldUpdateToClient
import org.astronkt.internal.setClientCAState

@DClassClient
class CubeClient(doId: DOId) : Cube(doId) {
    override fun onSetColor(color: UByte, sender: ChannelId?) {
        println("set color: $color")
    }

    override fun onSetX(x: Double, sender: ChannelId?) {
        println("set x: $x")
    }

    override fun onSetY(y: Double, sender: ChannelId?) {
        println("set y: $y")
    }

    override fun onSetZ(z: Double, sender: ChannelId?) {
        println("set z: $z")
    }
}

@DClassAI
class CubeAI(doId: DOId) : Cube(doId) {
    override fun afterInit() {
        coroutineScope.launch {
            while (isAlive) {
                color = ((color + 1U) % 7U).toUByte()
                delay(500L)
            }
        }
    }
}

@DClassUberDOG(5000U)
class AuthenticatorUD(doId: DOId) : Authenticator(doId) {
    override fun onRequestLogin(sender: ChannelId?) {
        internalRepository.setClientCAState(sender!!, ClientCAState.Established)
        internalRepository.sendFieldUpdateToClient(
            sender,
            doId,
            Companion.Fields.requestLoginAck,
            FieldValue.EmptyValue
        )
    }
}

@DClassClientUberDOG(5000U)
class AuthenticatorClient(doId: DOId) : Authenticator(doId) {
    override fun afterInit() {
        requestLogin()
    }
}

suspend fun clientMain() {
    setupAstronClientRepository(
        classSpecRepository,
        AstronClientRepositoryConfig("localhost:7198", "v1.0.0", 0xdeadbeefU)
    )

    clientRepository.registerClass(Authenticator.dClassId, AuthenticatorClient::class)
    clientRepository.registerClass(Cube.dClassId, CubeClient::class)

    clientRepository.launch()

    clientRepository.awaitFieldSet(5000U.toDOId(), Authenticator.Companion.Fields.requestLoginAck)

    clientRepository.sendInterestRequest(1U.toInterestId(), 6000U.toDOId(), 0U.toZoneId())
    clientRepository.awaitRepositoryClose()
}

suspend fun serverMain() {
    setupAstronInternalRepository(
        classSpecRepository,
        AstronInternalRepositoryConfig("localhost:7199", 10000U.toChannelId(), 4002U.toChannelId())
    )

    internalRepository.registerClass(Authenticator.dClassId, AuthenticatorUD::class)
    internalRepository.registerClass(Cube.dClassId, CubeAI::class)

    internalRepository.launch()

    internalRepository.createDO(6000U.toDOId(), 0U, 0U.toZoneId(), GameRoot.dClassId)

    internalRepository.createDO(
        6001U.toDOId(),
        6000U,
        0U.toZoneId(),
        Cube.dClassId,
        listOf(
            FieldValue.UInt8Value(0U),
            FieldValue.UInt32Value(0U),
            FieldValue.UInt32Value(1U),
            FieldValue.UInt32Value(1U)
        )
    )

    internalRepository.awaitRepositoryClose()

}

suspend fun main() {
    coroutineScope {
        val server = launch {
            serverMain()
        }

        val client = launch {
            clientMain()
        }

        delay(5000L)

        internalRepository.closeRepository()
        clientRepository.closeRepository()

        client.cancelAndJoin()
        server.cancelAndJoin()
    }
}