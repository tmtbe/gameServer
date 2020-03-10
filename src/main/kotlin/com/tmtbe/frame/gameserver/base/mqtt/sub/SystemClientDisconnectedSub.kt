package com.tmtbe.frame.gameserver.base.mqtt.sub

import com.tmtbe.frame.gameserver.base.mqtt.sub.SubscribeTopic.MqttSubscribeMessage
import com.tmtbe.frame.gameserver.base.scene.ResourceManager
import com.tmtbe.frame.gameserver.base.utils.toJsonObject
import org.springframework.stereotype.Component

@Component
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