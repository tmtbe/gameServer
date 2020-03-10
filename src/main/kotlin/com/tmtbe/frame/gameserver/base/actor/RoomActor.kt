package com.tmtbe.frame.gameserver.base.actor

import com.tmtbe.frame.gameserver.base.mqtt.MqttMessage
import com.tmtbe.frame.gameserver.base.mqtt.TopicTemplate
import com.tmtbe.frame.gameserver.base.mqtt.getMqttMsgType
import com.tmtbe.frame.gameserver.base.mqtt.serverError
import com.tmtbe.frame.gameserver.base.scene.Scene
import com.tmtbe.frame.gameserver.base.service.RoomService
import com.tmtbe.frame.gameserver.base.utils.SpringUtils
import java.time.Duration
import java.util.UUID

abstract class RoomActor(
        name: String,
        scene: Scene
) : Actor(name, scene) {
    var sceneName: String
    var roomName: String
    val roomConfiguration: RoomConfiguration

    init {
        val (_sceneName, _roomName) = name.split("/")
        this.sceneName = _sceneName
        this.roomName = _roomName
        this.roomConfiguration = provideRoomConfiguration()
    }

    abstract fun provideRoomConfiguration(): RoomConfiguration

    fun getPlayerActorList(): List<PlayerActor> {
        return children.values.filterIsInstance<PlayerActor>()
    }

    fun sendMqttToRoom(body: Any) {
        val mqttMessage = MqttMessage(UUID.randomUUID().toString(),
                body.getMqttMsgType(), body,
                TopicTemplate.TopicParse(TopicTemplate.RoomChannel(roomName),
                        sceneName, resourceManager.serverName)
        )
        sendMqttMessage(mqttMessage)
    }

    fun getPlayerActor(playerActorName: String): PlayerActor? {
        return children[playerActorName] as PlayerActor?
    }

    fun sendMqttToPlayer(playerActorName: String, body: Any) {
        val playerActor = getPlayerActor(playerActorName)
        if (playerActor != null) {
            val mqttMessage = MqttMessage(UUID.randomUUID().toString(),
                    body.getMqttMsgType(), body,
                    TopicTemplate.TopicParse(TopicTemplate.ResponseChannel(playerActor.playerName),
                            sceneName, resourceManager.serverName)
            )
            sendMqttMessage(mqttMessage)
        }
    }

    protected abstract suspend fun onAddedPlayer(playerActor: PlayerActor)
    protected abstract suspend fun onRemovingPlayer(playerActor: PlayerActor)

    protected override suspend fun onAddedChild(child: Actor) {
        if (child is PlayerActor) {
            onAddedPlayer(child)
        }
    }

    protected override suspend fun onRemovingChild(child: Actor) {
        if (child is PlayerActor) {
            onRemovingPlayer(child)
        }
        if (getPlayerActorList().size == 1) {
            destroy()
        }
    }

    protected override suspend fun onRemoving(parent: Actor) {

    }

    protected override suspend fun onAdded(parent: Actor) {

    }

    override fun getMaxKeepAliveTime(): Duration {
        return roomConfiguration.maxKeepAliveTime
    }

    fun isFull(): Boolean {
        return this.getPlayerActorList().size >= this.roomConfiguration.maxPlayerNumber
    }

    override suspend fun addChild(child: Actor) {
        if (isFull()) {
            child.destroy()
            serverError("房间已满")
        }
        super.addChild(child)
    }

    override suspend fun destroy() {
        getPlayerActorList().forEach { playerActor ->
            sendMqttToPlayer(playerActor.name, CloseRoomMsg(this.roomName))
        }
        super.destroy()
    }

    data class CloseRoomMsg(val roomName: String)
}
