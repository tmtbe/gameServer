package com.tmtbe.frame.gameserver.base.mqtt.sub

import com.tmtbe.frame.gameserver.base.scene.ResourceManager
import org.springframework.stereotype.Component

@Component
class RequestSub(val resourceManager: ResourceManager) : SubscribeTopic {
    override fun subTopics(): List<String> {
        return resourceManager.getAllSceneName().map { sceneName ->
            arrayListOf("\$queue/REQUEST/+/${sceneName}/all", "REQUEST/+/${sceneName}/${resourceManager.serverName}")
        }.flatten()
    }
}