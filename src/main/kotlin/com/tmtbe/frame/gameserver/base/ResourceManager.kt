package com.tmtbe.frame.gameserver.base

import com.hivemq.client.mqtt.datatypes.MqttQos
import com.hivemq.client.mqtt.mqtt3.Mqtt3BlockingClient
import com.tmtbe.frame.gameserver.base.utils.log
import com.tmtbe.frame.gameserver.config.MqttGateWay
import kotlinx.coroutines.InternalCoroutinesApi
import org.springframework.stereotype.Component
import java.nio.charset.Charset
import java.util.concurrent.ConcurrentHashMap

@InternalCoroutinesApi
@Component
class ResourceManager(
        val client: Mqtt3BlockingClient,
        val mqttGateWays: List<MqttGateWay>
) {
    private val log = this.log()
    private val actorMap: ConcurrentHashMap<String, Actor> = ConcurrentHashMap()
    private val sceneMap: ConcurrentHashMap<String, Scene> = ConcurrentHashMap()

    fun registerScene(scene: Scene) {
        sceneMap[scene.name] = scene
        log.info("新增一个场景：${scene.name}")
    }

    fun getScene(name: String) = sceneMap[name]

    fun addActor(actor: Actor) {
        actor.mqttGateways.addAll(mqttGateWays)
        actorMap[actor.name] = actor
    }

    fun getActor(name: String) = actorMap[name]

    fun sendMsgToActor(name: String, topic: String) {
        actorMap[name]?.send(MqttMsg(topic))
    }

    init {
        subscribe()
    }

    private fun subscribe() {
        client.toAsync().subscribeWith()
                .topicFilter("SERVER/#")
                .qos(MqttQos.AT_LEAST_ONCE)
                .callback {
                    val topic: String = it.topic.toString()
                    val buffer = it.payload.get()
                    sendMsgToActor(topic.substring(7), Charset.defaultCharset().decode(buffer).toString())
                }
                .send();
    }

    fun removeActor(name: String) {
        val actor = actorMap[name]
        if (actor != null) {
            actorMap.remove(actor.name)
            actor.scene.removeActor(actor.name)
            actor.destroy()
        }
    }

    fun removeActor(actor: Actor) {
        removeActor(actor.name)
    }

    fun removeAllActor() {
        val keys = actorMap.keys.toList()
        for (element in keys) {
            actorMap[element]?.destroy()
        }
    }

    fun getAllActorName() = actorMap.keys.toList()

    fun getAllPlayerActorName() = actorMap.filter { it.value is PlayerActor }.keys.toList()

    fun getAllRoomActorName() = actorMap.filter { it.value is RoomActor }.keys.toList()
}