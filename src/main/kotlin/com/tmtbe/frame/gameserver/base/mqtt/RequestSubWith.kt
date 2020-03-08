package com.tmtbe.frame.gameserver.base.mqtt

import com.tmtbe.frame.gameserver.base.scene.ResourceManager
import kotlinx.coroutines.InternalCoroutinesApi
import org.springframework.stereotype.Component

@Component
@InternalCoroutinesApi
class RequestSubWith(val resourceManager: ResourceManager) : SubscribeWith {
    override fun subWith(): List<String> {
        return resourceManager.getAllSceneName().map { sceneName ->
            arrayListOf("\$queue/REQUEST/+/${sceneName}/all", "REQUEST/+/${sceneName}/${resourceManager.serverName}")
        }.flatten()
    }
}