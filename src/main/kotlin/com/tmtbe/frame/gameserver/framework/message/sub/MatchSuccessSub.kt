package com.tmtbe.frame.gameserver.framework.message.sub

import com.tmtbe.frame.gameserver.framework.annotation.GameSubscribeTopic
import com.tmtbe.frame.gameserver.framework.message.sub.SubscribeTopic.MqttSubscribeMessage
import com.tmtbe.frame.gameserver.framework.scene.ResourceManager
import com.tmtbe.frame.gameserver.framework.service.RoomService
import com.tmtbe.frame.gameserver.framework.utils.toJsonObject
import java.util.UUID

@GameSubscribeTopic
class MatchSuccessSub(
        val resourceManager: ResourceManager,
        val roomService: RoomService
) : SubscribeTopic {
    override fun subTopics(): List<String> {
        return resourceManager.getAllSceneName().map { sceneName ->
            arrayListOf("\$queue/MATCHED/${sceneName}")
        }.flatten()
    }

    override fun interrupt(): Boolean = true

    override suspend fun handle(mqttSubscribeMessage: MqttSubscribeMessage) {
        val matchSuccessMsg = mqttSubscribeMessage.payload.toJsonObject(MatchSuccessMsg::class.java)
        val roomName = UUID.randomUUID().toString()
        val createRoom = roomService.createRoom(matchSuccessMsg.sceneName, roomName)
        createRoom.roomConfiguration.roomLevel = matchSuccessMsg.roomLevel
        matchSuccessMsg.usernameList.forEach {
            roomService.playerInterRoom(it, matchSuccessMsg.sceneName, roomName)
        }
    }

    data class MatchSuccessMsg(
            val sceneName: String,
            val usernameList: List<String>,
            val roomLevel: String
    )
}