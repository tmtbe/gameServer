package com.tmtbe.frame.gameserver.base.scene

import com.tmtbe.frame.gameserver.base.actor.Actor
import com.tmtbe.frame.gameserver.base.actor.MqttMsg
import com.tmtbe.frame.gameserver.base.actor.PlayerActor
import com.tmtbe.frame.gameserver.base.actor.RoomActor
import com.tmtbe.frame.gameserver.base.mqtt.MqttGateWay
import com.tmtbe.frame.gameserver.base.utils.log
import kotlinx.coroutines.InternalCoroutinesApi
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap

@Component
@InternalCoroutinesApi
class ResourceManager(sceneList: List<Scene>) {

    @Value("\${spring.application.name}")
    lateinit var serverName: String

    private val mqttGateWays: ArrayList<MqttGateWay> = ArrayList()
    private val log = this.log()
    private val actorMap: ConcurrentHashMap<String, Actor> = ConcurrentHashMap()
    private val sceneMap: ConcurrentHashMap<String, Scene> = ConcurrentHashMap()

    init {
        sceneList.forEach {
            registerScene(it)
            it.resourceManager = this
        }
    }

    fun registerScene(scene: Scene) {
        sceneMap[scene.name] = scene
        log.info("新增一个场景：${scene.name}")
    }

    fun getScene(name: String) = sceneMap[name]

    fun getAllSceneName() = sceneMap.keys().toList()

    fun addActor(actor: Actor) {
        if (actorMap.contains(actor.name)) error("重复名称的Actor：${actor.name}")
        actor.mqttGateways.addAll(mqttGateWays)
        actorMap[actor.name] = actor
    }

    fun getActor(name: String) = actorMap[name]

    fun sendMsgToActor(name: String, topic: String) {
        actorMap[name]?.send(MqttMsg(topic))
    }

    suspend fun removeActor(name: String) {
        val actor = actorMap[name]
        if (actor != null) {
            actorMap.remove(actor.name)
            actor.scene.removeActor(actor.name)
            actor.destroy()
        }
    }

    suspend fun removeActor(actor: Actor) {
        removeActor(actor.name)
    }

    suspend fun removeAllActor() {
        val keys = actorMap.keys.toList()
        for (element in keys) {
            actorMap[element]?.destroy()
        }
    }

    fun getAllActorName() = actorMap.keys.toList()

    fun getAllPlayerActorName() = actorMap.filter { it.value is PlayerActor }.keys.toList()

    fun getAllRoomActorName() = actorMap.filter { it.value is RoomActor }.keys.toList()

    fun registerMqttGateWay(mqttGateWay: MqttGateWay) {
        this.mqttGateWays.add(mqttGateWay)
    }

    fun onPlayerConnected(username: String) {
        sceneMap.forEach {
            it.value.onPlayerConnected(username)
        }
    }

    fun onPlayerDisconnected(username: String) {
        sceneMap.forEach {
            it.value.onPlayerDisconnected(username)
        }
    }
}