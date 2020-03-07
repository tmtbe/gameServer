package com.tmtbe.frame.gameserver.config

import com.hivemq.client.mqtt.datatypes.MqttQos
import com.hivemq.client.mqtt.mqtt3.Mqtt3BlockingClient
import com.hivemq.client.mqtt.mqtt3.Mqtt3Client
import com.hivemq.client.mqtt.mqtt3.message.auth.Mqtt3SimpleAuth
import com.hivemq.client.mqtt.mqtt3.message.connect.Mqtt3Connect
import com.tmtbe.frame.gameserver.base.utils.log
import kotlinx.coroutines.InternalCoroutinesApi
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Component


@Configuration
class MqttReceiveConfig {
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

    @InternalCoroutinesApi
    @Bean
    fun mqtt3Client(): Mqtt3BlockingClient {
        val client = Mqtt3Client.builder()
                .identifier(clientId)
                .serverHost(host)
                .serverPort(port.toInt())
                .buildBlocking();
        client.connect(Mqtt3Connect.builder().keepAlive(60)
                .simpleAuth(Mqtt3SimpleAuth.builder()
                        .username(username).password(password.toByteArray())
                        .build()).build())
        return client
    }
}


@Component
class MqttPublic(val mqtt3BlockingClient: Mqtt3BlockingClient) : MqttGateWay {
    var log = log()
    override fun sendToMqtt(data: String, topic: String, qos: MqttQos) {
        log.info(data)
        mqtt3BlockingClient.toAsync().publishWith().topic(topic).qos(qos).payload(data.toByteArray()).send()
    }
}

interface MqttGateWay {
    fun sendToMqtt(data: String, topic: String, qos: MqttQos = MqttQos.AT_LEAST_ONCE)
}