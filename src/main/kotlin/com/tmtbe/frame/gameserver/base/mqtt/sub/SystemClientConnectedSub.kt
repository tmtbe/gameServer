package com.tmtbe.frame.gameserver.base.mqtt.sub

import com.alibaba.fastjson.JSON
import com.tmtbe.frame.gameserver.base.mqtt.sub.SubscribeTopic.MqttSubscribeMessage
import com.tmtbe.frame.gameserver.base.scene.ResourceManager
import kotlinx.coroutines.InternalCoroutinesApi
import org.springframework.stereotype.Component

@Component
@InternalCoroutinesApi
class SystemClientConnectedSub(val resourceManager: ResourceManager) : SubscribeTopic {
    override fun subTopics(): List<String> {
        return arrayListOf("\$SYS/brokers/+/clients/+/connected")
    }

    override fun interrupt(): Boolean = true

    override fun handle(mqttSubscribeMessage: MqttSubscribeMessage) {
        val clientConnectedMsg = JSON.parseObject(mqttSubscribeMessage.payload, ClientConnectedMsg::class.java)
        resourceManager.onPlayerConnected(clientConnectedMsg.username)
    }

    data class ClientConnectedMsg(
            val clientid: String,
            val username: String,
            val ipaddress: String,
            val ts: Long
    )
}