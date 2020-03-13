package com.tmtbe.frame.gameserver.framework.actor

import com.tmtbe.frame.gameserver.framework.scene.Scene

abstract class PlayerActor(
        name: String,
        scene: Scene
) : Actor(name, scene) {

    @Volatile
    private var isOnline: Boolean = true
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

    fun sendMqttToPlayer(body: Any) {
        getRoomActor().sendMqttToPlayer(name, body)
    }

    fun sendMqttToRoom(body: Any) {
        getRoomActor().sendMqttToRoom(body)
    }

    protected override suspend fun onAddedChild(child: Actor) {

    }

    protected override suspend fun onRemovedChild(child: Actor) {

    }

    fun onConnected() {
        isOnline = true
    }

    fun onDisconnected() {
        isOnline = false
    }
}
