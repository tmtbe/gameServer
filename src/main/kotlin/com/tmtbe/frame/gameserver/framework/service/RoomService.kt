package com.tmtbe.frame.gameserver.framework.service

import com.tmtbe.frame.gameserver.framework.actor.RoomActor
import com.tmtbe.frame.gameserver.framework.message.TopicTemplate
import com.tmtbe.frame.gameserver.framework.message.serverError
import com.tmtbe.frame.gameserver.framework.scene.ResourceManager
import com.tmtbe.frame.gameserver.framework.scene.ResourceManager.Companion.PLAYER_ON_SERVER_SCENE_ROOM
import com.tmtbe.frame.gameserver.framework.scene.ResourceManager.Companion.ROOM_ON_GAME_SERVER
import com.tmtbe.frame.gameserver.framework.scene.ResourceManager.Companion.SCENE_HAS_ROOM_
import com.tmtbe.frame.gameserver.framework.scene.ResourceManager.Companion.SCENE_ROOM_HAS_PLAYER_SUB_
import com.tmtbe.frame.gameserver.framework.utils.RedisUtils
import com.tmtbe.frame.gameserver.framework.utils.log
import com.tmtbe.frame.gameserver.framework.utils.toJson
import com.tmtbe.frame.gameserver.framework.utils.toJsonObject
import org.springframework.stereotype.Service

@Service
class RoomService(
        val resourceManager: ResourceManager,
        val redisUtils: RedisUtils,
        val emqService: EMQService,
        val topicTemplate: TopicTemplate,
        val roomQueue: RoomQueue
) {
    protected val log = log()

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
            val playerServerRoom = hGet.toJsonObject(PlayerServerRoom::class.java)
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
        val roomActor = scene.getRoomActor(roomName) ?: serverError("room 不存在")
        var playerActor = roomActor.getPlayerActor("$sceneName/$roomName/$playerName")
        val createTopic = topicTemplate.createTopic(
                TopicTemplate.RoomChannel(roomName), sceneName, resourceManager.serverName
        )
        // 优先订阅否则刚加进去的收不到消息
        emqService.subscribe(playerName, createTopic)
        try {
            if (playerActor == null) {
                playerActor = scene.createPlayer(roomName, playerName)
            }
        } finally {
            emqService.unsubscribe(playerName, createTopic)
        }
        redisUtils.hSet(PLAYER_ON_SERVER_SCENE_ROOM, playerName,
                PlayerServerRoom(playerName, resourceManager.serverName, sceneName, roomName).toJson())
        redisUtils.sSet("$SCENE_ROOM_HAS_PLAYER_SUB_$sceneName/$roomName",
                PlayerRoomTopic(playerName, createTopic).toJson())
        playerActor!!.addHookOnDestroy {
            log.info("remove player sub/redis info:$playerName")
            emqService.unsubscribe(playerName, createTopic)
            redisUtils.hDel(PLAYER_ON_SERVER_SCENE_ROOM, playerName)
            redisUtils.sDel("$SCENE_ROOM_HAS_PLAYER_SUB_$sceneName/$roomName",
                    PlayerRoomTopic(playerName, createTopic).toJson())
        }
    }

    suspend fun playerOuterRoom(playerName: String, sceneName: String, roomName: String) {
        val actor = resourceManager.getActor("$sceneName/$roomName/$playerName") ?: serverError("用户不在这个房间")
        actor.destroy()
    }

    suspend fun closeRoom(roomActor: RoomActor) {
        roomActor.destroy()
    }

    suspend fun createRoom(sceneName: String, roomName: String): RoomActor {
        val scene = resourceManager.getScene(sceneName) ?: serverError("不存在的Scene")
        if (hasRoom(sceneName, roomName)) serverError("已存在相同名称的room:$roomName")
        val createRoom = scene.createRoom(roomName)
        redisUtils.sSet("$SCENE_HAS_ROOM_$sceneName", roomName)
        redisUtils.hSet(ROOM_ON_GAME_SERVER, "$sceneName/$roomName", resourceManager.serverName)
        roomQueue.sendRoomInfo(RoomQueue.RoomInfo(sceneName, roomName), createRoom.provideRoomConfiguration().maxKeepAliveTime)
        createRoom.addHookOnDestroy {
            val roomActor = it as RoomActor
            log.info("remove room redis info:${roomActor.roomName}")
            redisUtils.sDel("$SCENE_HAS_ROOM_$sceneName", roomActor.roomName)
            redisUtils.hDel(ROOM_ON_GAME_SERVER, "$sceneName/$roomName")
            redisUtils.del("$SCENE_ROOM_HAS_PLAYER_SUB_$sceneName/$roomName")
        }
        return createRoom
    }

    suspend fun hasRoom(sceneName: String, roomName: String): Boolean {
        return redisUtils.sHasKey("$SCENE_HAS_ROOM_$sceneName", roomName)
    }
}