package com.tmtbe.frame.gameserver

import com.tmtbe.frame.gameserver.framework.annotation.GameApplication
import org.springframework.boot.WebApplicationType
import org.springframework.boot.runApplication

@GameApplication
class GameserverApplication

fun main(args: Array<String>) {
    runApplication<GameserverApplication>(*args) {
        this.webApplicationType = WebApplicationType.NONE
    }
}