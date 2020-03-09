package com.tmtbe.frame.gameserver.base.mqtt.sub

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

