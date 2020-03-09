package com.tmtbe.frame.gameserver.base.service

import com.alibaba.fastjson.JSON
import com.tmtbe.frame.gameserver.base.actor.RoomActor
import com.tmtbe.frame.gameserver.base.mqtt.TopicTemplate
import com.tmtbe.frame.gameserver.base.scene.ResourceManager
import com.tmtbe.frame.gameserver.base.scene.ResourceManager.Companion.PLAYER_ON_SERVER_ROOM
import com.tmtbe.frame.gameserver.base.scene.ResourceManager.Companion.ROOM_ON_GAME_SERVER
import com.tmtbe.frame.gameserver.base.scene.ResourceManager.Companion.ROOM_ON_SCENE_
import com.tmtbe.frame.gameserver.base.utils.RedisUtils
import kotlinx.coroutines.InternalCoroutinesApi
import org.springframework.stereotype.Service

@Service
@InternalCoroutinesApi
class RoomService(
        val resourceManager: ResourceManager,
        val redisUtils: RedisUtils,
        val emqService: EMQService,
        val topicTemplate: TopicTemplate,
        val roomQueue: RoomQueue
) {
    data class PlayerServerRoom(
            val playerName: String,
            val serverName: String,
            val sceneName: String,
            val roomName: String
    )

    suspend fun getPlayerRoom(playerName: String): PlayerServerRoom? {
        val hGet = redisUtils.hGet(PLAYER_ON_SERVER_ROOM, playerName)
        if (hGet != null) {
            val playerServerRoom = JSON.parseObject(hGet, PlayerServerRoom::class.java)
            if (playerServerRoom.serverName == resourceManager.serverName) {
                val scene = resourceManager.getScene(playerServerRoom.sceneName)
                val hasRoom = scene?.hasRoom(playerServerRoom.roomName)
                if (hasRoom != true) {
                    cleanRoomAndPlayer(playerServerRoom)
                    return null
                }
            } else {
                if (resourceManager.getServerRoomCount(playerServerRoom.serverName) == 0L) {
                    cleanRoomAndPlayer(playerServerRoom)
                    return null
                }
            }
            return playerServerRoom
        } else {
            return null
        }
    }

    private suspend fun cleanRoomAndPlayer(playerServerRoom: PlayerServerRoom) {
        redisUtils.sDel("$ROOM_ON_SCENE_${playerServerRoom.sceneName}", playerServerRoom.roomName)
        redisUtils.hDel(ROOM_ON_GAME_SERVER, "${playerServerRoom.sceneName}/${playerServerRoom.roomName}")
        redisUtils.hDel(PLAYER_ON_SERVER_ROOM, playerServerRoom.playerName)
    }

    suspend fun playerInterRoom(playerName: String, sceneName: String, roomName: String) {
        val scene = resourceManager.getScene(sceneName)!!
        val roomActor = scene.getRoomActor("$sceneName/$roomName") ?: error("room 不存在")
        val playerActor = roomActor.getPlayerActor("$sceneName/$roomName/$playerName")
        val createTopic = topicTemplate.createTopic(
                TopicTemplate.RoomChannel(roomName), sceneName, resourceManager.serverName
        )
        emqService.subscribe(playerName, createTopic)
        if (playerActor == null) {
            scene.createPlayer(roomName, playerName)
        }
        redisUtils.hSet(PLAYER_ON_SERVER_ROOM, playerName,
                JSON.toJSONString(PlayerServerRoom(playerName, resourceManager.serverName, sceneName, roomName)))
    }

    suspend fun playerOuterRoom(playerName: String, sceneName: String, roomName: String) {
        resourceManager.getActor("$sceneName/$roomName/$playerName")?.destroy()
        val createTopic = topicTemplate.createTopic(
                TopicTemplate.RoomChannel(roomName), sceneName, resourceManager.serverName
        )
        emqService.unSubscribe(playerName, createTopic)
        redisUtils.hDel(PLAYER_ON_SERVER_ROOM, playerName)
    }

    suspend fun closeRoom(roomActor: RoomActor) {
        val roomName = roomActor.roomName
        val sceneName = roomActor.sceneName
        roomActor.getPlayerActorList().map { it.playerName }.forEach { playerName ->
            redisUtils.hDel(PLAYER_ON_SERVER_ROOM, playerName)
        }
        roomActor.destroy()
        redisUtils.sDel("$ROOM_ON_SCENE_$sceneName", roomActor.roomName)
        redisUtils.hDel(ROOM_ON_GAME_SERVER, "$sceneName/$roomName")
    }

    suspend fun createRoom(sceneName: String, roomName: String) {
        val scene = resourceManager.getScene(sceneName) ?: error("不存在的Scene")
        if (hasRoom(sceneName, roomName)) error("已存在相同名称的room:$roomName")
        val createRoom = scene.createRoom(roomName)
        redisUtils.sSet("$ROOM_ON_SCENE_$sceneName", roomName)
        redisUtils.hSet(ROOM_ON_GAME_SERVER, "$sceneName/$roomName", resourceManager.serverName)
        roomQueue.sendRoomInfo(RoomQueue.RoomInfo(sceneName, roomName), createRoom.provideRoomConfiguration().maxKeepAliveTime)
    }

    suspend fun hasRoom(sceneName: String, roomName: String): Boolean {
        return redisUtils.sHasKey("$ROOM_ON_SCENE_$sceneName", roomName)
    }
}