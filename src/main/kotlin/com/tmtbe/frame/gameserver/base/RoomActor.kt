package com.tmtbe.frame.gameserver.base

import kotlinx.coroutines.InternalCoroutinesApi

@InternalCoroutinesApi
abstract class RoomActor(
        name: String,
        scene: Scene,
        resourceManager: ResourceManager
) : Actor(name, scene, resourceManager) {
    fun getPlayerActorList(): List<Actor> {
        return children.values.filterIsInstance<PlayerActor>()
    }

    fun sendMqttToRoom(data: String) {
        getMqttGatWay().sendToMqtt(data, name)
    }

    fun getPlayerActor(playerName: String): PlayerActor? {
        return children[playerName] as PlayerActor?
    }

    fun sendMqttToPlayer(playerName: String, data: String) {
        val playerActor = getPlayerActor(playerName)
        if (playerActor != null) {
            getMqttGatWay().sendToMqtt(data, playerActor.name)
        }
    }

    protected abstract fun onAddedPlayer(playerActor: PlayerActor)
    protected abstract fun onRemovingPlayer(playerActor: PlayerActor)

    protected override fun onAddedChild(child: Actor) {
        if (child is PlayerActor) {
            onAddedPlayer(child)
        }
    }

    protected override fun onRemovingChild(child: Actor) {
        if (child is PlayerActor) {
            onRemovingPlayer(child)
        }
    }

    protected override fun onRemoving(parent: Actor) {

    }

    protected override fun onAdded(parent: Actor) {

    }

    override fun destroy() {
        scene.removeActor(this.name)
        if (!isStartDestroy) {
            scene.onRoomDestroy(this)
        }
        super.destroy()
    }
}
