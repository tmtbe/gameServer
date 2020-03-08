package com.tmtbe.frame.gameserver.base.mqtt

import com.alibaba.fastjson.JSON.parseObject
import com.hivemq.client.mqtt.MqttGlobalPublishFilter
import com.hivemq.client.mqtt.datatypes.MqttQos
import com.hivemq.client.mqtt.mqtt3.Mqtt3BlockingClient
import com.hivemq.client.mqtt.mqtt3.Mqtt3Client
import com.hivemq.client.mqtt.mqtt3.message.auth.Mqtt3SimpleAuth
import com.hivemq.client.mqtt.mqtt3.message.connect.Mqtt3Connect
import com.tmtbe.frame.gameserver.base.scene.ResourceManager
import com.tmtbe.frame.gameserver.base.utils.log
import kotlinx.coroutines.InternalCoroutinesApi
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Component
import java.nio.charset.Charset
import javax.annotation.PostConstruct


@Configuration
class MqttConfig {
    @Value("\${spring.mqtt.username}")
    private lateinit var username: String

    @Value("\${spring.mqtt.password}")
    private lateinit var password: String

    @Value("\${spring.mqtt.host}")
    private lateinit var host: String

    @Value("\${spring.mqtt.port}")
    private lateinit var port: String

    @Value("\${spring.mqtt.client.id}")
    private lateinit var clientId: String

    private var log = this.log()

    @InternalCoroutinesApi
    @Bean
    fun mqtt3Client(
            subscribeWithList: List<SubscribeWith>,
            mqttMessageBindingList: List<MqttMessageBinding<*>>,
            topicTemplate: TopicTemplate
    ): Mqtt3BlockingClient {
        val client = Mqtt3Client.builder()
                .identifier(clientId)
                .serverHost(host)
                .serverPort(port.toInt())
                .buildBlocking();
        client.connect(Mqtt3Connect.builder().keepAlive(60)
                .simpleAuth(Mqtt3SimpleAuth.builder()
                        .username(username).password(password.toByteArray())
                        .build()).build())
        client.toAsync().publishes(MqttGlobalPublishFilter.ALL) { pub ->
            val topic: String = pub.topic.toString()
            val buffer = pub.payload.get()
            val payload = Charset.defaultCharset().decode(buffer).toString()
            val parseTopic = topicTemplate.parseTopic(topic)
            val parseObject = parseObject(payload)
            val type = parseObject.getString("type")
            val binding = mqttMessageBindingList.firstOrNull() { binding ->
                binding.getType() == type
            }
            binding?.buildMessage(parseTopic, parseObject)
            if (binding == null) log.warn("丢弃一个无效信息：$payload")
        }
        subscribeWithList.map { it.subWith() }.flatten().forEach { subWith ->
            log.info("添加订阅：$subWith")
            client.toAsync().subscribeWith()
                    .topicFilter(subWith)
                    .qos(MqttQos.AT_LEAST_ONCE)
                    .send();
        }
        return client
    }
}


@InternalCoroutinesApi
@Component
class MqttPublic(val mqtt3BlockingClient: Mqtt3BlockingClient, resourceManager: ResourceManager) : MqttGateWay(resourceManager) {
    override fun sendToMqtt(data: String, topic: String, qos: MqttQos) {
        mqtt3BlockingClient.toAsync().publishWith().topic(topic).qos(qos).payload(data.toByteArray()).send()
    }
}

@InternalCoroutinesApi
abstract class MqttGateWay(val resourceManager: ResourceManager) {
    @PostConstruct
    fun registerMqttGateWay() {
        resourceManager.registerMqttGateWay(this)
    }

    abstract fun sendToMqtt(data: String, topic: String, qos: MqttQos = MqttQos.AT_LEAST_ONCE)
}