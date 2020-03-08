package com.tmtbe.frame.gameserver

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cloud.openfeign.EnableFeignClients

@SpringBootApplication

class GameserverApplication

fun main(args: Array<String>) {
    runApplication<GameserverApplication>(*args)
}