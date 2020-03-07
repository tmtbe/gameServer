package com.tmtbe.frame.gameserver

import kotlinx.coroutines.InternalCoroutinesApi
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class GameserverApplication

@InternalCoroutinesApi
fun main(args: Array<String>) {
    runApplication<GameserverApplication>(*args)
}