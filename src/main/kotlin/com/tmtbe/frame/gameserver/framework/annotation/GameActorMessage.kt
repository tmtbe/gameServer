package com.tmtbe.frame.gameserver.framework.annotation

@Target(AnnotationTarget.ANNOTATION_CLASS, AnnotationTarget.CLASS)
@kotlin.annotation.Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class GameActorMessage(val value: String = "")