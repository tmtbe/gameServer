package com.tmtbe.frame.gameserver.base.mqtt

import com.alibaba.fastjson.JSONObject
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

data class MqttMessage<T>(
        val requestId: String?,
        val type: String,
        val body: T?,
        val topicParse: TopicTemplate.TopicParse
)

abstract class MqttMessageBinding<T> {
    abstract fun getType(): String
    abstract fun getClassName(): Class<T>
    abstract suspend fun handleMessage(mqttMessage: MqttMessage<T>)
    fun buildMessage(topic: TopicTemplate.TopicParse, payload: JSONObject) {
        val type = payload.getString("type")
        val body = payload.getJSONObject("body")?.toJavaObject(getClassName())
        val mqttMessage = MqttMessage(payload.getString("requestId"), type, body, topic)
        GlobalScope.launch {
            handleMessage(mqttMessage)
        }
    }
}
