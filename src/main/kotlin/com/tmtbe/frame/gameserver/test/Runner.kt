package com.tmtbe.frame.gameserver.test

import com.tmtbe.frame.gameserver.base.scene.ResourceManager
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.springframework.boot.CommandLineRunner
import org.springframework.stereotype.Component


@InternalCoroutinesApi
@Component
class Runner(val resourceManager: ResourceManager) : CommandLineRunner {
    override fun run(vararg args: String?) {
        GlobalScope.launch {
            val scene = resourceManager.getScene("test")!!
            for (i in 1..500) {
                scene.createRoom("room$i")
                for (j in 1..8) {
                    scene.createPlayer("room$i", "player$j")
                    delay(1)
                }
            }
        }
    }
}