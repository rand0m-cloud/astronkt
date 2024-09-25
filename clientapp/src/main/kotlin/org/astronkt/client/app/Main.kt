package org.astronkt.client.app

import AstronLoginManager
import classSpecRepository
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.astronkt.*
import org.astronkt.client.AstronClientRepositoryConfig

@DClassClientUberDOG(4670U)
class AstronLoginManagerClient(doId: DOId) : AstronLoginManager(doId) {
    init {
        this.requestLogin("dev")
    }

    override fun onLoginResponse(
        arg0: ByteArray,
        sender: ChannelId?,
    ) {
        println("arg0: ${arg0.decodeToString()}, sender: $sender")
        this.requestPlayAvatar(1000000001U)
    }
}

suspend fun main() {
    setupAstronClientRepository(
        classSpecRepository,
        AstronClientRepositoryConfig(
            "localhost:7198",
        ),
    )

    clientRepository.registerClass(AstronLoginManager.dClassId, AstronLoginManagerClient::class)

    MainScope().launch {
        clientRepository.astronClientNetwork.networkMessages.collect {
            println(it)
        }
    }

    clientRepository.launch()

    println("astron launched")
}
