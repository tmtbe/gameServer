package com.tmtbe.frame.gameserver.framework.scene

import com.tmtbe.frame.gameserver.framework.actor.Actor
import com.tmtbe.frame.gameserver.framework.actor.MqttMsg
import com.tmtbe.frame.gameserver.framework.actor.PlayerActor
import com.tmtbe.frame.gameserver.framework.actor.RoomActor
import com.tmtbe.frame.gameserver.framework.mqtt.MqttGateWay
import com.tmtbe.frame.gameserver.framework.mqtt.MqttMessage
import com.tmtbe.frame.gameserver.framework.mqtt.serverError
import com.tmtbe.frame.gameserver.framework.utils.RedisUtils
import com.tmtbe.frame.gameserver.framework.utils.log
import com.tmtbe.frame.gameserver.framework.utils.toJson
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap
import javax.annotation.PostConstruct

@Component
@EnableScheduling
class ResourceManager(sceneList: List<Scene>) {
    @Value("\${spring.application.name}")
    lateinit var serverName: String

    @Autowired
    lateinit var redisUtils: RedisUtils
    private var mqttIndex: Int = 0
    private val mqttGateWays: ArrayList<MqttGateWay> = ArrayList()
    private val log = this.log()
    private val actorMap: ConcurrentHashMap<String, Actor> = ConcurrentHashMap()
    private val sceneMap: ConcurrentHashMap<String, Scene> = ConcurrentHashMap()

    companion object {
        //有哪些服务器存在
        const val GAME_SERVER_ = "GAME_SERVER_"

        //room在哪个服务器
        const val ROOM_ON_GAME_SERVER = "ROOM_ON_GAME_SERVER"

        //场景里有多少room
        const val SCENE_HAS_ROOM_ = "SCENE_HAS_ROOM_"

        //玩家在哪个room
        const val PLAYER_ON_SERVER_SCENE_ROOM = "PLAYER_ON_SERVER_SCENE_ROOM"

        //room里有多少玩家的订阅信息
        const val SCENE_ROOM_HAS_PLAYER_SUB_ = "SCENE_ROOM_HAS_PLAYER_SUB_"
    }

    init {
        sceneList.forEach {
            registerScene(it)
            it.resourceManager = this
        }
    }

    /**
     * 定时注册下服务器状态
     */
    @PostConstruct
    @Scheduled(cron = "0/10 * *  * * ? ")
    private fun registerServer() {
        GlobalScope.launch {
            redisUtils.set("$GAME_SERVER_${serverName}",
                    getAllRoomActorName().size.toString(),
                    Duration.ofSeconds(15))
        }
    }

    suspend fun getServerRoomCount(serverName: String): Long {
        return redisUtils.get(GAME_SERVER_ + serverName).run {
            return this?.toLong() ?: 0
        }
    }

    fun registerScene(scene: Scene) {
        if(sceneMap.containsKey(scene.name)) error("已存在同名的Scene")
        sceneMap[scene.name] = scene
        log.info("新增一个场景：${scene.name}")
    }

    fun getScene(name: String) = sceneMap[name]

    fun getAllSceneName() = sceneMap.keys().toList()

    fun addActor(actor: Actor) {
        if (actorMap.contains(actor.name)) serverError("重复名称的Actor：${actor.name}")
        actorMap[actor.name] = actor
        actor.addHookOnDestroy {
            log.info("remove resource: ${it.name}")
            actorMap.remove(it.name)
        }
    }

    fun getActor(name: String) = actorMap[name]

    fun forwardMqttMsgToActor(name: String, mqttMessage: MqttMessage<*>) {
        actorMap[name]?.send(MqttMsg(mqttMessage))
    }

    fun forwardMqttMsgToActor(actor: Actor, mqttMessage: MqttMessage<*>) {
        actor.send(MqttMsg(mqttMessage))
    }

    suspend fun removeActor(name: String) {
        actorMap[name]?.destroy()
    }

    suspend fun removeActor(actor: Actor) {
        actorMap[actor.name]?.destroy()
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

    fun getMqttGatWay(): MqttGateWay {
        mqttIndex++
        return mqttGateWays[mqttIndex % mqttGateWays.size]
    }

    fun sendMqttMessage(mqttMessage: MqttMessage<*>) {
        val topicParse = mqttMessage.topicParse
        getMqttGatWay().sendToMqtt(mqttMessage.getSendMap().toJson(), topicParse.toTopicString())
    }
}