package org.astronkt

import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.plus
import org.astronkt.client.AstronClientNetwork
import org.astronkt.client.AstronClientRepository
import org.astronkt.client.AstronClientRepositoryConfig
import org.astronkt.internal.AstronInternalNetwork
import org.astronkt.internal.AstronInternalRepository
import org.astronkt.internal.AstronInternalRepositoryConfig


var isClient: Boolean = false
    private set

var isServer: Boolean = false
    private set

val isClientAndServer: Boolean get() = isClient && isServer

lateinit var internalRepository: AstronInternalRepository
    private set

lateinit var clientRepository: AstronClientRepository
    private set

fun setupAstronInternalRepository(
    classSpecRepository: ClassSpecRepository,
    config: AstronInternalRepositoryConfig,
) {
    isServer = true
    internalRepository = AstronInternalRepository(
        config.repositoryCoroutineScope + CoroutineName("ObjectsCoroutineScope"),
        classSpecRepository,
        AstronInternalNetwork(
            config.repositoryCoroutineScope + CoroutineName("AstronInternalNetwork")
        ),
        config,
    )
}

@Suppress("unused")
fun setupAstronClientRepository(
    classSpecRepository: ClassSpecRepository,
    config: AstronClientRepositoryConfig,
) {
    isClient = true
    clientRepository = AstronClientRepository(
        config.repositoryCoroutineScope + CoroutineName("ObjectsCoroutineScope"),
        classSpecRepository,
        AstronClientNetwork(config.repositoryCoroutineScope + CoroutineName("AstronClientNetwork")),
        config,
    )
}
