package com.tmtbe.frame.gameserver.game

import com.tmtbe.frame.gameserver.framework.scene.ResourceManager
import com.tmtbe.frame.gameserver.framework.service.EMQAcl
import com.tmtbe.frame.gameserver.framework.service.EMQService
import com.tmtbe.frame.gameserver.game.client.AccountClient
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactive.awaitFirst
import org.springframework.boot.CommandLineRunner
import org.springframework.stereotype.Component

@Component
class Runner(val resourceManager: ResourceManager,
             val emqService: EMQService,
             val accountClient: AccountClient
) : CommandLineRunner {
    override fun run(vararg args: String?) {
        val usernameList = arrayListOf("client", "client2", "client3", "client4")
        GlobalScope.launch {
            usernameList.forEach { username ->
                emqService.addUser(username, username)
                emqService.addAcl(username, "REQUEST/${username}/+/+", EMQAcl.PUBLISH)
                emqService.subscribe(username, "RESPONSE/${username}/+/+")
            }
            /* val scene = resourceManager.getScene("demoA")!!
             for (i in 1..20000) {
                 scene.createRoom("room$i")
                 for (j in 1..4) {
                     scene.createPlayer("room$i", "room$i-player$j")
                     delay(1)
                 }
                 delay(10)
             }*/
            val result = accountClient.getUserInfo("").awaitFirst()
            println(result)
        }
    }
}