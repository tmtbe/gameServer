package com.tmtbe.frame.gameserver.base

import kotlinx.coroutines.InternalCoroutinesApi

@InternalCoroutinesApi
abstract class PlayerActor(
        name: String,
        scene: Scene,
        resourceManager: ResourceManager
) : Actor(name, scene, resourceManager) {
    fun getRoomActor(): RoomActor {
        return parent as RoomActor
    }

    fun sendToPlayer(data: String) {
        getMqttGatWay().sendToMqtt(data, name)
    }

    fun sendToRoom(data: String) {
        getRoomActor().sendToRoom(data)
    }

    protected override fun onAddedChild(child: Actor) {

    }

    protected override fun onRemovingChild(child: Actor) {

    }
}
