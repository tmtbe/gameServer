package com.tmtbe.frame.gameserver.framework.annotation

import org.springframework.stereotype.Component

@Target(AnnotationTarget.ANNOTATION_CLASS, AnnotationTarget.CLASS)
@kotlin.annotation.Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
@Component
annotation class GameScene(val value: String = "")