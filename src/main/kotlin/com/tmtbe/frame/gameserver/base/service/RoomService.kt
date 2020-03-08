package com.tmtbe.frame.gameserver.base.service

import com.tmtbe.frame.gameserver.base.scene.ResourceManager
import com.tmtbe.frame.gameserver.base.actor.RoomActor
import com.tmtbe.frame.gameserver.base.utils.RedisUtils
import kotlinx.coroutines.InternalCoroutinesApi
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
@InternalCoroutinesApi
class RoomService(
        val resourceManager: ResourceManager,
        val redisUtils: RedisUtils,
        val emqService: EMQService
) {
    @Value("\${spring.application.name}")
    private lateinit var serverName: String

    suspend fun playerInterRoom(playerName: String, sceneName: String, roomName: String) {
        if (!hasRoom(sceneName, roomName)) {
            createRoom(sceneName, roomName)
        }
        val scene = resourceManager.getScene(sceneName)!!
        val roomActor = scene.getRoomActor("$sceneName/$roomName") ?: error("room 不存在")
        var playerActor = roomActor.getPlayerActor("$sceneName/$roomName/$playerName")
        if (playerActor == null) {
            playerActor = scene.createPlayer(roomName, playerName)
        }
        emqService.addAcl(playerName, roomActor.name, EMQAcl.SUBSCRIBE)
        emqService.addAcl(playerName, "SERVER/${playerActor.name}", EMQAcl.PUBLISH)
        emqService.subscribe(roomActor.name, 1, playerName)
    }

    suspend fun playerOuterRoom(playerName: String, sceneName: String, roomName: String) {
        resourceManager.getActor("$sceneName/$roomName/$playerName")?.destroy()
        emqService.removeAcl(playerName, "$sceneName/$roomName")
        emqService.removeAcl(playerName, "SERVER/$sceneName/$roomName/$playerName")
        emqService.unSubscribe("$sceneName/$roomName", playerName)
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
        scene.createRoom(roomName)
        redisUtils.sSet("Scene", roomName)
        redisUtils.hSet("RoomOnGameServer", "$sceneName/$roomName", serverName)
    }

    suspend fun hasRoom(sceneName: String, roomName: String): Boolean {
        return redisUtils.sHasKey("Scene", roomName)
    }
}