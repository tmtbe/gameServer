package com.tmtbe.frame.gameserver.base.mqtt

import com.alibaba.fastjson.JSONObject
import com.tmtbe.frame.gameserver.base.scene.ResourceManager
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.launch
import org.springframework.beans.factory.annotation.Autowired

data class MqttMessage<T>(
        val requestId: String?,
        val type: String,
        val body: T?,
        val topicParse: TopicTemplate.TopicParse
) {
    fun getSendMap(): HashMap<String, Any?> {
        val hashMap: HashMap<String, Any?> = HashMap()
        hashMap["requestId"] = requestId
        hashMap["type"] = type
        hashMap["body"] = body
        return hashMap
    }
}

abstract class MqttMessageBinding<T> {
    private val mqttIndex: Int = 0

    @InternalCoroutinesApi
    @Autowired
    protected lateinit var resourceManager: ResourceManager

    @Autowired
    protected lateinit var topicTemplate: TopicTemplate

    abstract fun getClassName(): Class<T>
    abstract suspend fun handleMessage(mqttMessage: MqttMessage<T>)

    @InternalCoroutinesApi
    fun buildMessage(topic: TopicTemplate.TopicParse, payload: JSONObject) {
        val type = payload.getString("type")
        val body = payload.getJSONObject("body")?.toJavaObject(getClassName())
        val mqttMessage = MqttMessage(payload.getString("requestId"), type, body, topic)
        GlobalScope.launch {
            try {
                handleMessage(mqttMessage)
            } catch (e: ErrorMessageException) {
                responseError(mqttMessage, e)
            } catch (ignore: BreakException) {

            }
        }
    }

    @InternalCoroutinesApi
    fun responseMessage(fromMqttMessage: MqttMessage<*>, responseBody: Any) {
        val topicChannel = fromMqttMessage.topicParse.topicChannel
        if (topicChannel is TopicTemplate.RequestChannel) {
            val createTopic = TopicTemplate.TopicParse(
                    TopicTemplate.ResponseChannel(topicChannel.getName()),
                    fromMqttMessage.topicParse.scene,
                    resourceManager.serverName)
            resourceManager.sendMqttMessage(
                    MqttMessage(fromMqttMessage.requestId, responseBody.getMqttMsgType(), responseBody, createTopic))
        }
    }

    @InternalCoroutinesApi
    fun responseError(fromMqttMessage: MqttMessage<*>, e: Throwable) {
        val errorMessageBody = ErrorMessage(e.message)
        responseMessage(fromMqttMessage, errorMessageBody)
    }

    @InternalCoroutinesApi
    fun responseError(fromMqttMessage: MqttMessage<*>, errorMessage: String, body: Any) {
        val errorMessageBody = ErrorMessage(errorMessage, body)
        responseMessage(fromMqttMessage, errorMessageBody)
        throw BreakException()
    }
}

class BreakException : Exception()

class ErrorMessageException(error: String) : Exception(error)

data class ErrorMessage(
        val error: String?,
        val body: Any? = null
)

fun Any.getMqttMsgType(): String = this.javaClass.simpleName