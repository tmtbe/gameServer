package com.tmtbe.frame.gameserver.test

import com.tmtbe.frame.gameserver.base.scene.ResourceManager
import com.tmtbe.frame.gameserver.base.service.EMQAcl
import com.tmtbe.frame.gameserver.base.service.EMQService
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.launch
import org.springframework.boot.CommandLineRunner
import org.springframework.stereotype.Component


@InternalCoroutinesApi
@Component
class Runner(val resourceManager: ResourceManager, val emqService: EMQService) : CommandLineRunner {
    override fun run(vararg args: String?) {
        val usernameList = arrayListOf("client", "client2")
        GlobalScope.launch {
            usernameList.forEach { username ->
                emqService.addUser(username, username)
                emqService.addAcl(username, "REQUEST/${username}/+/+", EMQAcl.PUBLISH)
                emqService.subscribe(username, "RESPONSE/${username}/+/+")
            }
        }
    }
}