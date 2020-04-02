package com.tmtbe.frame.gameserver.framework.message.sub

import com.tmtbe.frame.gameserver.framework.scene.ResourceManager
import com.tmtbe.frame.gameserver.framework.annotation.GameSubscribeTopic

@GameSubscribeTopic
class RequestSub(val resourceManager: ResourceManager) : SubscribeTopic {
    override fun subTopics(): List<String> {
        return resourceManager.getAllSceneName().map { sceneName ->
            arrayListOf("\$queue/REQUEST/+/${sceneName}/all",
                    "\$queue/SERVER/+/${sceneName}/all",
                    "REQUEST/+/${sceneName}/${resourceManager.serverName}")
        }.flatten()
    }
}