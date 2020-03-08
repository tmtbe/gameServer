package com.tmtbe.frame.gameserver.base.actor

import com.tmtbe.frame.gameserver.base.scene.ResourceManager
import com.tmtbe.frame.gameserver.base.scene.Scene
import kotlinx.coroutines.InternalCoroutinesApi

@InternalCoroutinesApi
abstract class PlayerActor(
        name: String,
        scene: Scene,
        resourceManager: ResourceManager
) : Actor(name, scene, resourceManager) {
    var sceneName: String
    var roomName: String
    var playerName: String

    init {
        val (_sceneName, _roomName, _playerName) = name.split("/")
        this.sceneName = _sceneName
        this.roomName = _roomName
        this.playerName = _playerName
    }

    fun getRoomActor(): RoomActor {
        return parent as RoomActor
    }

    fun sendMqttToPlayer(data: String) {
        getMqttGatWay().sendToMqtt(data, name)
    }

    fun sendMqttToRoom(data: String) {
        getRoomActor().sendMqttToRoom(data)
    }

    protected override suspend fun onAddedChild(child: Actor) {

    }

    protected override suspend fun onRemovingChild(child: Actor) {

    }
}
