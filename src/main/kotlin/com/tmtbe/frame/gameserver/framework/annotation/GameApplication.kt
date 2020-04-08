package com.tmtbe.frame.gameserver.framework.annotation

import com.tmtbe.frame.gameserver.framework.message.GameRegistrar
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.Import
import reactivefeign.spring.config.EnableReactiveFeignClients
import kotlin.reflect.KClass

@kotlin.annotation.Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.ANNOTATION_CLASS, AnnotationTarget.CLASS)
@MustBeDocumented
@SpringBootApplication
@Import(GameRegistrar::class)
@EnableReactiveFeignClients
annotation class GameApplication(
        vararg val value: String = [],
        val basePackages: Array<String> = [],
        val basePackageClasses: Array<KClass<*>> = []
)