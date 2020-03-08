package com.tmtbe.frame.gameserver.base.actor

import com.tmtbe.frame.gameserver.base.scene.Scene
import com.tmtbe.frame.gameserver.base.service.RoomService
import com.tmtbe.frame.gameserver.base.utils.SpringUtils
import kotlinx.coroutines.InternalCoroutinesApi

@InternalCoroutinesApi
abstract class PlayerActor(
        name: String,
        scene: Scene
) : Actor(name, scene) {
    @Volatile
    private var isStartDestroy: Boolean = false
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
        getRoomActor().sendMqttToPlayer(playerName, data)
    }

    fun sendMqttToRoom(data: String) {
        getRoomActor().sendMqttToRoom(data)
    }

    protected override suspend fun onAddedChild(child: Actor) {

    }

    protected override suspend fun onRemovingChild(child: Actor) {

    }

    abstract fun onConnected()

    abstract fun onDisconnected()

    override suspend fun destroy() {
        if (isStartDestroy) return
        isStartDestroy = true
        SpringUtils.getBean(RoomService::class.java).playerOuterRoom(playerName, sceneName, roomName)
        super.destroy()
    }
}
