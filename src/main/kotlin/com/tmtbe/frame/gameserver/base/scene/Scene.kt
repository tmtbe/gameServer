package com.tmtbe.frame.gameserver.base.scene

import com.tmtbe.frame.gameserver.base.actor.PlayerActor
import com.tmtbe.frame.gameserver.base.actor.RoomActor
import kotlinx.coroutines.InternalCoroutinesApi
import java.util.concurrent.ConcurrentHashMap
import java.util.regex.Matcher
import java.util.regex.Pattern

@InternalCoroutinesApi
abstract class Scene(
        val name: String,
        private val roomActor: Class<*>,
        private val playerActor: Class<*>
) {

    var resourceManager: ResourceManager? = null

    private val roomActors: ConcurrentHashMap<String, RoomActor> = ConcurrentHashMap()
    private val playerActors: ConcurrentHashMap<String, PlayerActor> = ConcurrentHashMap()

    init {
        matchName(name)
    }

    fun getAllRoomActorName() = roomActors.keys.toList()
    fun getAllPlayerActorName() = resourceManager!!.getAllPlayerActorName().filter { it.startsWith("$name/") }
    fun getRoomActor(roomName: String) = roomActors[roomName]
    suspend fun createRoom(roomName: String) {
        matchName(roomName)
        val newRoomName = "$name/$roomName"
        val roomActor = roomActor.getConstructor(String::class.java, Scene::class.java)
                .newInstance(newRoomName, this) as RoomActor
        resourceManager!!.addActor(roomActor)
        roomActors[newRoomName] = roomActor
        onRoomCreate(roomActor)
    }

    suspend fun createPlayer(roomName: String, playerName: String): PlayerActor {
        matchName(playerName)
        matchName(roomName)
        if (playerActors.containsKey(playerName)) error("玩家同一个游戏只允许进入一个房间:$playerName")
        val newRoomName = "$name/$roomName"
        val newPlayerName = "$newRoomName/$playerName"
        val roomActor = resourceManager!!.getActor(newRoomName) ?: error("不存在的room")
        val playerActor = playerActor.getConstructor(String::class.java, Scene::class.java)
                .newInstance(newPlayerName, this) as PlayerActor
        playerActors[playerName] = playerActor
        resourceManager!!.addActor(playerActor)
        roomActor.addChild(playerActor)
        return playerActor
    }

    suspend fun removeActor(name: String) {
        val actor = resourceManager!!.getActor(name)
        when (actor) {
            is RoomActor -> roomActors.remove(name)
            is PlayerActor -> playerActors.remove(actor.playerName)
        }
        if (actor != null) {
            resourceManager!!.removeActor(actor.name)
            actor.destroy()
        }
    }

    abstract suspend fun onRoomCreate(roomActor: RoomActor)
    abstract suspend fun onRoomDestroy(roomActor: RoomActor)

    companion object {
        private const val regEx = "[ _`~!@#$%^&*()+=|{}':;',\\[\\].<>/?~！@#￥%……&*（）——+|{}【】‘；：”“’。，、？]|\n|\r|\t"
        private val p: Pattern = Pattern.compile(regEx)
        private fun matchName(name: String) {
            val m: Matcher = p.matcher(name)
            if (m.find()) error("非法名称")
        }
    }

    fun onPlayerConnected(username: String) {
        playerActors[username]?.onConnected()
    }

    fun onPlayerDisconnected(username: String) {
        playerActors[username]?.onDisconnected()
    }
}