package org.astronkt.client.app

//import GameSpec.AstronLoginManager
//import GameSpec.ToontownDistrict
//import GameSpec.classSpecRepository
import GameSpec.AstronLoginManager
import GameSpec.PotentialAvatar
import GameSpec.ToontownDistrict
import GameSpec.classSpecRepository
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.astronkt.*
import org.astronkt.client.AstronClientRepositoryConfig
import org.astronkt.client.sendInterestRequest

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
        clientRepository.sendInterestRequest(1U.toInterestId(), 4618U.toDOId(), 2U.toZoneId())
        clientRepository.sendInterestRequest(2U.toInterestId(), 4618U.toDOId(), 3U.toZoneId())
        clientRepository.sendInterestRequest(3U.toInterestId(), 4618U.toDOId(), 4U.toZoneId())
        clientRepository.sendInterestRequest(4U.toInterestId(), 4618U.toDOId(), 5U.toZoneId())

        requestAvatarList()
    }

    override fun onAvatarListResponse(arg0: List<PotentialAvatar>, sender: ChannelId?) {
        println(arg0)
        requestPlayAvatar(arg0[2].avNum)
    }
}

@DClassClient
class ToontownDistrictClient(doId: DOId) : ToontownDistrict(doId) {
    override fun onSetName(arg0: String, sender: ChannelId?) {
        println("district named: $arg0")
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
    clientRepository.registerClass(ToontownDistrict.dClassId, ToontownDistrictClient::class)

    MainScope().launch {
        clientRepository.astronClientNetwork.networkMessages.collect {
            println(it)
        }
    }

    clientRepository.launch()

    println("astron launched")
}
