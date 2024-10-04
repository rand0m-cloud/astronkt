package org.astronkt.client.app

// import GameSpec.AstronLoginManager
// import GameSpec.ToontownDistrict
// import GameSpec.classSpecRepository
import GameSpec.*
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.astronkt.*
import org.astronkt.client.AstronClientRepositoryConfig
import org.astronkt.client.sendInterestRequest
import org.astronkt.client.setLocation

@DClassClientUberDOG(4670U)
class AstronLoginManagerClient(doId: DOId) : AstronLoginManager(doId) {
    init {
        this.requestLogin("dev2")
    }

    override fun onLoginResponse(
        arg0: ByteArray,
        sender: ChannelId?,
    ) {
        println("arg0: ${arg0.decodeToString()}, sender: $sender")
        clientRepository.sendInterestRequest(1U.toInterestId(), 4618U.toDOId(), 2U.toZoneId())
        clientRepository.sendInterestRequest(2U.toInterestId(), 4618U.toDOId(), 3U.toZoneId())
        clientRepository.sendInterestRequest(3U.toInterestId(), 4618U.toDOId(), 4U.toZoneId())

        requestAvatarList()
    }

    override fun onAvatarListResponse(
        arg0: List<PotentialAvatar>,
        sender: ChannelId?,
    ) {
        println(arg0)
        requestPlayAvatar(arg0[0].avNum)
    }
}

@DClassClient
class ToontownDistrictClient(doId: DOId) : ToontownDistrict(doId) {
    override fun onSetName(
        arg0: String,
        sender: ChannelId?,
    ) {
        println("district named: $arg0")
    }
}

var localToon: DistributedToonClient? = null

@DClassClient
class DistributedToonClient(doId: DOId) : DistributedToon(doId) {
    lateinit var defaultShard: DOId
    lateinit var lastHood: ZoneId

    override fun onSetDefaultShard(
        arg0: UInt,
        sender: ChannelId?,
    ) {
        defaultShard = arg0.toDOId()
    }

    override fun onSetLastHood(
        arg0: UInt,
        sender: ChannelId?,
    ) {
        lastHood = arg0.toZoneId()
    }

    override fun afterInit() {
        if (localToon != null) return

        println("lastHood was $lastHood")
        localToon = this
        clientRepository.sendInterestRequest(4U.toInterestId(), defaultShard, 2U.toZoneId())
        clientRepository.setLocation(localToon!!.doId, localToon!!.defaultShard, 1U.toZoneId())
        clientRepository.setLocation(localToon!!.doId, localToon!!.defaultShard, localToon!!.lastHood)
        clientRepository.sendInterestRequest(5U.toInterestId(), localToon!!.defaultShard, localToon!!.lastHood)
        println("finish afterInit")
    }
}

@DClassClient
class SafeZoneManagerClient(doId: DOId) : SafeZoneManager(doId) {
    override fun afterInit() {
        println("entering safe zone")
        val toon = localToon!!
        coroutineScope.launch {
            toon.clearSmoothing(0)
            toon.setSmPosHprL(0U, -7.93, 8.75, -5.0, 24.47, 0.0, 0.0, 21621)
            toon.setAnimState("TeleportIn".toList(), 59.395, 29200)
            enterSafeZone()
        }
    }
}

suspend fun main() {
    setupAstronClientRepository(
        classSpecRepository,
        AstronClientRepositoryConfig(
            "localhost:7198",
            "sv1.0.47.38",
            0x209cece0U,
        ),
    )

    clientRepository.registerClass(AstronLoginManager.dClassId, AstronLoginManagerClient::class)
    clientRepository.registerClass(ToontownDistrict.dClassId, ToontownDistrictClient::class)
    clientRepository.registerClass(DistributedToon.dClassId, DistributedToonClient::class)
    clientRepository.registerClass(SafeZoneManager.dClassId, SafeZoneManagerClient::class)

    MainScope().launch {
        clientRepository.astronClientNetwork.networkMessages.collect {
            // println(it)
        }
    }

    clientRepository.launch()

    println("astron launched")
}
