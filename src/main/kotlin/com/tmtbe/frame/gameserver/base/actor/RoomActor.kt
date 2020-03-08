package com.tmtbe.frame.gameserver.base.actor

import com.tmtbe.frame.gameserver.base.mqtt.TopicTemplate
import com.tmtbe.frame.gameserver.base.scene.Scene
import com.tmtbe.frame.gameserver.base.service.RoomService
import com.tmtbe.frame.gameserver.base.utils.SpringUtils
import kotlinx.coroutines.InternalCoroutinesApi

@InternalCoroutinesApi
abstract class RoomActor(
        name: String,
        scene: Scene
) : Actor(name, scene) {
    var sceneName: String
    var roomName: String

    @Volatile
    private var isStartDestroy: Boolean = false

    init {
        val (_sceneName, _roomName) = name.split("/")
        this.sceneName = _sceneName
        this.roomName = _roomName
    }

    fun getPlayerActorList(): List<PlayerActor> {
        return children.values.filterIsInstance<PlayerActor>()
    }

    fun sendMqttToRoom(data: String) {
        getMqttGatWay().sendToMqtt(data,
                topicTemplate.createTopic(TopicTemplate.RoomChannel(roomName),
                        sceneName, resourceManager.serverName))
    }

    fun getPlayerActor(playerName: String): PlayerActor? {
        return children[playerName] as PlayerActor?
    }

    fun sendMqttToPlayer(playerName: String, data: String) {
        val playerActor = getPlayerActor(playerName)
        if (playerActor != null) {
            getMqttGatWay().sendToMqtt(data,
                    topicTemplate.createTopic(TopicTemplate.ResponseChannel(playerName),
                            sceneName, resourceManager.serverName))
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
        if (getPlayerActorList().size == 1) this.destroy()
    }

    protected override suspend fun onRemoving(parent: Actor) {

    }

    protected override suspend fun onAdded(parent: Actor) {

    }

    override suspend fun destroy() {
        if (isStartDestroy) return
        isStartDestroy = true
        scene.removeActor(this.name)
        scene.onRoomDestroy(this)
        SpringUtils.getBean(RoomService::class.java).closeRoom(this)
        super.destroy()
    }
}
