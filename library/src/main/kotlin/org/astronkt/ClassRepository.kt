package org.astronkt

import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.hasAnnotation

class ClassRepository(
    private val classes: MutableMap<DClassId, MutableList<KClass<*>>> = mutableMapOf(),
    private val isServer: Boolean
) {
    fun classesForDClass(dclassId: DClassId): List<KClass<*>> =
        classes.getOrElse(dclassId) { listOf() }.filter { clazz ->
            if (this.isServer) {
                clazz.annotations.any { it.annotationClass == DClassAI::class || it.annotationClass == DClassUberDOG::class }
            } else {
                clazz.annotations.any { it.annotationClass == DClassClient::class || it.annotationClass == DClassClientUberDOG::class }
            }
        }

    fun registerClass(dclassId: DClassId, vararg clazz: KClass<*>) {
        classes.getOrPut(dclassId) { mutableListOf() }
            .addAll(clazz)
    }

    fun uberDogs(): List<Pair<DOId, KClass<*>>> =
        classes.values.flatten().filter { it.hasAnnotation<DClassUberDOG>() }.map {
            val dogDOId = it.findAnnotation<DClassUberDOG>()!!.id
            dogDOId.toDOId() to it
        }

    fun uberDogClients(): List<Pair<DOId, KClass<*>>> =
        classes.values.flatten().filter { it.hasAnnotation<DClassClientUberDOG>() }.map {
            val dogDOId = it.findAnnotation<DClassClientUberDOG>()!!.id
            dogDOId.toDOId() to it
        }

}