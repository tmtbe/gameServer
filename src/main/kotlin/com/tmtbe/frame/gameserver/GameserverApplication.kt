package com.tmtbe.frame.gameserver

import org.springframework.boot.WebApplicationType
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication()

class GameserverApplication

fun main(args: Array<String>) {
    runApplication<GameserverApplication>(*args) {
        this.webApplicationType = WebApplicationType.NONE
    }
}