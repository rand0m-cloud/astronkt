package org.astronkt.sample

import GameSpec.Cube
import GameSpec.classSpecRepository
import org.astronkt.ChannelId
import org.astronkt.DOId
import org.astronkt.client.AstronClientRepositoryConfig
import org.astronkt.clientRepository
import org.astronkt.setupAstronClientRepository

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

suspend fun main() {
    setupAstronClientRepository(
        classSpecRepository,
        AstronClientRepositoryConfig("localhost:7198", "v1.0.0", 0xdeadbeefU)
    )

    clientRepository.registerClass(Cube.dClassId, CubeClient::class)

    clientRepository.launch()
}