package com.tmtbe.frame.gameserver.framework.scene

import com.tmtbe.frame.gameserver.framework.actor.AddActorMsg
import com.tmtbe.frame.gameserver.framework.actor.PlayerActor
import com.tmtbe.frame.gameserver.framework.actor.RoomActor
import com.tmtbe.frame.gameserver.framework.message.serverError
import com.tmtbe.frame.gameserver.framework.utils.log
import java.util.concurrent.ConcurrentHashMap
import java.util.regex.Matcher
import java.util.regex.Pattern

abstract class Scene(
        val name: String,
        private val roomActor: Class<out RoomActor>,
        private val playerActor: Class<out PlayerActor>,
        val configuration: SceneConfiguration
) {
    protected val log = log()
    var resourceManager: ResourceManager? = null

    private val roomActors: ConcurrentHashMap<String, RoomActor> = ConcurrentHashMap()
    private val playerActors: ConcurrentHashMap<String, PlayerActor> = ConcurrentHashMap()

    init {
        matchName(name)
    }

    fun getAllRoomActor() = roomActors.values.toList()
    fun getAllPlayerActor() = playerActors.values.toList()
    fun getPlayerActor(playerName: String) = playerActors[playerName]
    fun getRoomActor(roomName: String) = roomActors[roomName]
    suspend fun createRoom(roomName: String): RoomActor {
        matchName(roomName)
        val newRoomName = "$name/$roomName"
        val roomActor = roomActor.getConstructor(String::class.java, Scene::class.java)
                .newInstance(newRoomName, this) as RoomActor
        addRoomActor(roomActor)
        return roomActor
    }

    private suspend fun addRoomActor(roomActor: RoomActor) {
        onRoomCreate(roomActor)
        resourceManager!!.addActor(roomActor)
        roomActors[roomActor.roomName] = roomActor
        roomActor.addHookOnDestroy {
            onRoomDestroy(it as RoomActor)
            roomActors.remove(it.roomName)
            log.info("remove room ${it.roomName}")
        }
    }

    suspend fun createPlayer(roomName: String, playerName: String, playerActor: Class<out PlayerActor>): PlayerActor {
        matchName(playerName)
        matchName(roomName)
        if (playerActors.containsKey(playerName)) serverError("玩家同一个游戏只允许进入一个房间:$playerName")
        val newRoomName = "$name/$roomName"
        val newPlayerName = "$newRoomName/$playerName"
        val roomActor = resourceManager!!.getActor(newRoomName) ?: serverError("不存在的room")
        if ((roomActor as RoomActor).isFull()) serverError("房间已满人")
        val playerActor = playerActor.getConstructor(String::class.java, Scene::class.java)
                .newInstance(newPlayerName, this) as PlayerActor
        addPlayerActor(playerActor)
        val addActorMsg = AddActorMsg<Any>(playerActor)
        roomActor.send(addActorMsg)
        val addActorResult = addActorMsg.response.await()
        if (addActorResult.success) {
            return playerActor
        } else {
            serverError(addActorResult.error)
        }
    }

    suspend fun createPlayer(roomName: String, playerName: String): PlayerActor {
        return createPlayer(roomName, playerName, playerActor)
    }

    private fun addPlayerActor(playerActor: PlayerActor) {
        resourceManager!!.addActor(playerActor)
        playerActors[playerActor.playerName] = playerActor
        playerActor.addHookOnDestroy {
            log.info("remove player: ${(it as PlayerActor).playerName}")
            playerActors.remove(it.playerName)
        }
    }

    abstract suspend fun onRoomCreate(roomActor: RoomActor)
    abstract suspend fun onRoomDestroy(roomActor: RoomActor)

    companion object {
        private const val regEx = "[ _`~!@#$%^&*()+=|{}':;',\\[\\].<>/?~！@#￥%……&*（）——+|{}【】‘；：”“’。，、？]|\n|\r|\t"
        private val p: Pattern = Pattern.compile(regEx)
        private fun matchName(name: String) {
            val m: Matcher = p.matcher(name)
            if (m.find()) serverError("非法名称")
        }
    }

    fun onPlayerConnected(username: String) {
        playerActors[username]?.onConnected()
    }

    fun onPlayerDisconnected(username: String) {
        playerActors[username]?.onDisconnected()
    }

    fun hasRoom(roomName: String): Boolean {
        return roomActors.containsKey("$name/$roomName")
    }
}

data class SceneConfiguration(
        // 需要多少玩家才能开始游戏，主要用于匹配
        val matchedNeedPlayerNum: Int
)