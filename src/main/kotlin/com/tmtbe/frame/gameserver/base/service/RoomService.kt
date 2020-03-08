package com.tmtbe.frame.gameserver.base.service

import com.tmtbe.frame.gameserver.base.actor.RoomActor
import com.tmtbe.frame.gameserver.base.mqtt.TopicTemplate
import com.tmtbe.frame.gameserver.base.scene.ResourceManager
import com.tmtbe.frame.gameserver.base.utils.RedisUtils
import kotlinx.coroutines.InternalCoroutinesApi
import org.springframework.stereotype.Service

@Service
@InternalCoroutinesApi
class RoomService(
        val resourceManager: ResourceManager,
        val redisUtils: RedisUtils,
        val emqService: EMQService,
        val topicTemplate: TopicTemplate
) {
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
    }

    suspend fun playerOuterRoom(playerName: String, sceneName: String, roomName: String) {
        resourceManager.getActor("$sceneName/$roomName/$playerName")?.destroy()
        val createTopic = topicTemplate.createTopic(
                TopicTemplate.RoomChannel(roomName), sceneName, resourceManager.serverName
        )
        emqService.unSubscribe(playerName, createTopic)
    }

    suspend fun closeRoom(roomActor: RoomActor) {
        val roomName = roomActor.roomName
        val sceneName = roomActor.sceneName
        roomActor.destroy()
        redisUtils.sDel(roomActor.sceneName, roomActor.roomName)
        redisUtils.hDel("RoomOnGameServer", "$sceneName/$roomName")
    }

    suspend fun createRoom(sceneName: String, roomName: String) {
        val scene = resourceManager.getScene(sceneName) ?: error("不存在的Scene")
        if (hasRoom(sceneName, roomName)) error("已存在相同名称的room:$roomName")
        scene.createRoom(roomName)
        redisUtils.sSet("Scene", roomName)
        redisUtils.hSet("RoomOnGameServer", "$sceneName/$roomName", resourceManager.serverName)
    }

    suspend fun hasRoom(sceneName: String, roomName: String): Boolean {
        return redisUtils.sHasKey("Scene", roomName)
    }
}