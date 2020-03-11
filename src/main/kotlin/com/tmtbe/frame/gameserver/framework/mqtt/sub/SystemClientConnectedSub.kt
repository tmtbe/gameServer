package com.tmtbe.frame.gameserver.framework.mqtt.sub

import com.tmtbe.frame.gameserver.framework.mqtt.sub.SubscribeTopic.MqttSubscribeMessage
import com.tmtbe.frame.gameserver.framework.scene.ResourceManager
import com.tmtbe.frame.gameserver.framework.stereotype.GameSubscribeTopic
import com.tmtbe.frame.gameserver.framework.utils.toJsonObject

@GameSubscribeTopic
class SystemClientConnectedSub(val resourceManager: ResourceManager) : SubscribeTopic {
    override fun subTopics(): List<String> {
        return arrayListOf("\$SYS/brokers/+/clients/+/connected")
    }

    override fun interrupt(): Boolean = true

    override suspend fun handle(mqttSubscribeMessage: MqttSubscribeMessage) {
        val clientConnectedMsg = mqttSubscribeMessage.payload.toJsonObject(ClientConnectedMsg::class.java)
        resourceManager.onPlayerConnected(clientConnectedMsg.username)
    }

    data class ClientConnectedMsg(
            val clientid: String,
            val username: String,
            val ipaddress: String,
            val ts: Long
    )
}