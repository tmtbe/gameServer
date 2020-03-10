package com.tmtbe.frame.gameserver.base.mqtt.sub

import com.tmtbe.frame.gameserver.base.mqtt.sub.SubscribeTopic.MqttSubscribeMessage
import com.tmtbe.frame.gameserver.base.scene.ResourceManager
import com.tmtbe.frame.gameserver.base.utils.toJsonObject
import kotlinx.coroutines.InternalCoroutinesApi
import org.springframework.stereotype.Component

@Component
@InternalCoroutinesApi
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