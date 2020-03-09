package com.tmtbe.frame.gameserver.base.service

import com.alibaba.fastjson.JSON
import com.tmtbe.frame.gameserver.base.actor.RoomActor
import com.tmtbe.frame.gameserver.base.mqtt.TopicTemplate
import com.tmtbe.frame.gameserver.base.mqtt.serverError
import com.tmtbe.frame.gameserver.base.scene.ResourceManager
import com.tmtbe.frame.gameserver.base.scene.ResourceManager.Companion.PLAYER_ON_SERVER_SCENE_ROOM
import com.tmtbe.frame.gameserver.base.scene.ResourceManager.Companion.ROOM_ON_GAME_SERVER
import com.tmtbe.frame.gameserver.base.scene.ResourceManager.Companion.SCENE_HAS_ROOM_
import com.tmtbe.frame.gameserver.base.scene.ResourceManager.Companion.SCENE_ROOM_HAS_PLAYER_SUB_
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

    data class PlayerRoomTopic(
            val playerName: String,
            val topic: String
    )

    suspend fun getPlayerRoom(playerName: String): PlayerServerRoom? {
        val hGet = redisUtils.hGet(PLAYER_ON_SERVER_SCENE_ROOM, playerName)
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
        redisUtils.sDel("$SCENE_HAS_ROOM_${playerServerRoom.sceneName}", playerServerRoom.roomName)
        redisUtils.hDel(ROOM_ON_GAME_SERVER, "${playerServerRoom.sceneName}/${playerServerRoom.roomName}")
        redisUtils.hDel(PLAYER_ON_SERVER_SCENE_ROOM, playerServerRoom.playerName)
    }

    suspend fun playerInterRoom(playerName: String, sceneName: String, roomName: String) {
        val scene = resourceManager.getScene(sceneName)!!
        val roomActor = scene.getRoomActor("$sceneName/$roomName") ?: serverError("room 不存在")
        val playerActor = roomActor.getPlayerActor("$sceneName/$roomName/$playerName")
        if (playerActor == null) {
            scene.createPlayer(roomName, playerName)
        }
        val createTopic = topicTemplate.createTopic(
                TopicTemplate.RoomChannel(roomName), sceneName, resourceManager.serverName
        )
        emqService.subscribe(playerName, createTopic)
        redisUtils.hSet(PLAYER_ON_SERVER_SCENE_ROOM, playerName,
                JSON.toJSONString(PlayerServerRoom(playerName, resourceManager.serverName, sceneName, roomName)))
        redisUtils.sSet("$SCENE_ROOM_HAS_PLAYER_SUB_$sceneName/$roomName",
                PlayerRoomTopic(playerName, createTopic).toJson())
    }

    /**
     * 清理用户就行
     */
    suspend fun playerOuterRoom(playerName: String, sceneName: String, roomName: String) {
        resourceManager.getActor("$sceneName/$roomName/$playerName")?.destroy()
        val createTopic = topicTemplate.createTopic(
                TopicTemplate.RoomChannel(roomName), sceneName, resourceManager.serverName
        )
        emqService.unsubscribe(playerName, createTopic)
        redisUtils.hDel(PLAYER_ON_SERVER_SCENE_ROOM, playerName)
        redisUtils.sDel("$SCENE_ROOM_HAS_PLAYER_SUB_$sceneName/$roomName",
                PlayerRoomTopic(playerName, createTopic).toJson())
    }

    /**
     * 清理房间就行
     */
    suspend fun closeRoom(roomActor: RoomActor) {
        val roomName = roomActor.roomName
        val sceneName = roomActor.sceneName
        roomActor.destroy()
        redisUtils.sDel("$SCENE_HAS_ROOM_$sceneName", roomActor.roomName)
        redisUtils.hDel(ROOM_ON_GAME_SERVER, "$sceneName/$roomName")
        redisUtils.del("$SCENE_ROOM_HAS_PLAYER_SUB_$sceneName/$roomName")
    }

    suspend fun createRoom(sceneName: String, roomName: String) {
        val scene = resourceManager.getScene(sceneName) ?: serverError("不存在的Scene")
        if (hasRoom(sceneName, roomName)) serverError("已存在相同名称的room:$roomName")
        val createRoom = scene.createRoom(roomName)
        redisUtils.sSet("$SCENE_HAS_ROOM_$sceneName", roomName)
        redisUtils.hSet(ROOM_ON_GAME_SERVER, "$sceneName/$roomName", resourceManager.serverName)
        roomQueue.sendRoomInfo(RoomQueue.RoomInfo(sceneName, roomName), createRoom.provideRoomConfiguration().maxKeepAliveTime)
    }

    suspend fun hasRoom(sceneName: String, roomName: String): Boolean {
        return redisUtils.sHasKey("$SCENE_HAS_ROOM_$sceneName", roomName)
    }
}