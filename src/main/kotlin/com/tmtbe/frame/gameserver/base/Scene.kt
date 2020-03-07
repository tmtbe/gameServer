package com.tmtbe.frame.gameserver.base

import kotlinx.coroutines.InternalCoroutinesApi
import java.util.concurrent.ConcurrentHashMap
import java.util.regex.Matcher
import java.util.regex.Pattern


@InternalCoroutinesApi
abstract class Scene(
        val name: String,
        private val roomActor: Class<*>,
        private val playerActor: Class<*>,
        private var resourceManager: ResourceManager
) {
    private val roomActors: ConcurrentHashMap<String, RoomActor> = ConcurrentHashMap()

    init {
        matchName(name)
        resourceManager.registerScene(this)
    }

    fun getAllRoomActorName() = roomActors.keys.toList()
    fun getAllPlayerActorName() = resourceManager.getAllPlayerActorName().filter { it.startsWith("$name/") }
    fun getRoomActor(roomName: String) = roomActors[roomName]
    fun createRoom(roomName: String) {
        matchName(roomName)
        val newRoomName = "$name/$roomName"
        val roomActor = roomActor.getConstructor(String::class.java,
                        Scene::class.java,
                        ResourceManager::class.java)
                .newInstance(newRoomName, this, resourceManager) as RoomActor
        resourceManager.addActor(roomActor)
        roomActors[newRoomName] = roomActor
        onRoomCreate(roomActor)
    }

    fun createPlayer(roomName: String, playerName: String) {
        matchName(playerName)
        matchName(roomName)
        val newRoomName = "$name/$roomName"
        val newPlayerName = "$newRoomName/$playerName"
        val roomActor = resourceManager.getActor(newRoomName) ?: error("不存在的room")
        val playerActor = playerActor.getConstructor(String::class.java,
                        Scene::class.java,
                        ResourceManager::class.java)
                .newInstance(newPlayerName, this, resourceManager) as PlayerActor
        roomActor.addChild(playerActor)
        resourceManager.addActor(playerActor)
    }

    fun removeActor(name: String) {
        val actor = resourceManager.getActor(name)
        roomActors.remove(name)
        if (actor != null) {
            resourceManager.removeActor(actor.name)
            actor.destroy()
        }
    }

    abstract fun onRoomCreate(roomActor: RoomActor)
    abstract fun onRoomDestroy(roomActor: RoomActor)

    companion object {
        private const val regEx = "[ _`~!@#$%^&*()+=|{}':;',\\[\\].<>/?~！@#￥%……&*（）——+|{}【】‘；：”“’。，、？]|\n|\r|\t"
        private val p: Pattern = Pattern.compile(regEx)
        private fun matchName(name: String) {
            val m: Matcher = p.matcher(name)
            if (m.find()) error("非法名称")
        }
    }
}