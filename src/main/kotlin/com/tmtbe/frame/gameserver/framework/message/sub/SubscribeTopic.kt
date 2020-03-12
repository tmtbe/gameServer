package com.tmtbe.frame.gameserver.framework.message.sub

interface SubscribeTopic {
    fun subTopics(): List<String>
    fun interrupt(): Boolean = false
    suspend fun handle(mqttSubscribeMessage: MqttSubscribeMessage) {

    }

    data class MqttSubscribeMessage(
            val topic: String,
            val payload: String
    )
}

