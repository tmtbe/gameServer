package com.tmtbe.frame.gameserver.framework.mqtt.sub

import com.tmtbe.frame.gameserver.framework.mqtt.sub.SubscribeTopic.MqttSubscribeMessage
import com.tmtbe.frame.gameserver.framework.scene.ResourceManager
import com.tmtbe.frame.gameserver.framework.stereotype.GameSubscribeTopic
import com.tmtbe.frame.gameserver.framework.utils.toJsonObject

@GameSubscribeTopic
class SystemClientDisconnectedSub(val resourceManager: ResourceManager) : SubscribeTopic {
    override fun subTopics(): List<String> {
        return arrayListOf("\$SYS/brokers/+/clients/+/disconnected")
    }

    override fun interrupt(): Boolean = true

    override suspend fun handle(mqttSubscribeMessage: MqttSubscribeMessage) {
        val clientDisConnectedMsg = mqttSubscribeMessage.payload.toJsonObject(ClientDisconnectedMsg::class.java)
        resourceManager.onPlayerDisconnected(clientDisConnectedMsg.username)
    }

    data class ClientDisconnectedMsg(
            val clientid: String,
            val username: String,
            val reason: String,
            val ts: Long
    )
}