package com.tmtbe.frame.gameserver.framework.mqtt.sub

import com.tmtbe.frame.gameserver.framework.scene.ResourceManager
import com.tmtbe.frame.gameserver.framework.stereotype.GameSubscribeTopic

@GameSubscribeTopic
class RequestSub(val resourceManager: ResourceManager) : SubscribeTopic {
    override fun subTopics(): List<String> {
        return resourceManager.getAllSceneName().map { sceneName ->
            arrayListOf("\$queue/REQUEST/+/${sceneName}/all", "REQUEST/+/${sceneName}/${resourceManager.serverName}")
        }.flatten()
    }
}