package org.astronkt

import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.plus
import org.astronkt.client.AstronClientNetwork
import org.astronkt.client.AstronClientRepository
import org.astronkt.client.AstronClientRepositoryConfig
import org.astronkt.internal.AstronInternalNetwork
import org.astronkt.internal.AstronInternalRepository
import org.astronkt.internal.AstronInternalRepositoryConfig


var isClient: Boolean = false
    private set

val isServer: Boolean
    get() = !isClient

lateinit var internalRepository: AstronInternalRepository
    private set

lateinit var clientRepository: AstronClientRepository
    private set

fun setupAstronInternalRepository(
    classSpecRepository: ClassSpecRepository,
    config: AstronInternalRepositoryConfig,
) {
    isClient = false
    internalRepository = AstronInternalRepository(
        MainScope() + CoroutineName("ObjectsCoroutineScope"),
        classSpecRepository,
        AstronInternalNetwork(
            MainScope() + CoroutineName("AstronInternalNetwork")
        ),
        config,
    )
}

fun setupAstronClientRepository(
    classSpecRepository: ClassSpecRepository,
    config: AstronClientRepositoryConfig,
) {
    isClient = true
    clientRepository = AstronClientRepository(
        MainScope() + CoroutineName("ObjectsCoroutineScope"),
        classSpecRepository,
        AstronClientNetwork(MainScope() + CoroutineName("AstronClientNetwork")),
        config,
    )
}

