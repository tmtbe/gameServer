package com.tmtbe.frame.gameserver.base.mqtt.sub

import com.alibaba.fastjson.JSON
import com.tmtbe.frame.gameserver.base.mqtt.sub.SubscribeTopic.MqttSubscribeMessage
import com.tmtbe.frame.gameserver.base.scene.ResourceManager
import kotlinx.coroutines.InternalCoroutinesApi
import org.springframework.stereotype.Component

@Component
@InternalCoroutinesApi
class SystemClientDisconnectedSub(val resourceManager: ResourceManager) : SubscribeTopic {
    override fun subTopics(): List<String> {
        return arrayListOf("\$SYS/brokers/+/clients/+/disconnected")
    }

    override fun interrupt(): Boolean = true

    override fun handle(mqttSubscribeMessage: MqttSubscribeMessage) {
        val clientDisConnectedMsg = JSON.parseObject(mqttSubscribeMessage.payload, ClientDisconnectedMsg::class.java)
        resourceManager.onPlayerDisconnected(clientDisConnectedMsg.username)
    }

    data class ClientDisconnectedMsg(
            val clientid: String,
            val username: String,
            val reason: String,
            val ts: Long
    )
}