package com.tmtbe.frame.gameserver.framework.message.bind

import com.tmtbe.frame.gameserver.framework.annotation.GameMqttMessageBinding
import com.tmtbe.frame.gameserver.framework.message.MqttMessage
import com.tmtbe.frame.gameserver.framework.message.MqttMessageBinding
import com.tmtbe.frame.gameserver.framework.scene.Scene
import com.tmtbe.frame.gameserver.framework.service.RoomService
import java.util.UUID

@GameMqttMessageBinding
class MatchSuccessMsgBind(
        private val roomService: RoomService
) : MqttMessageBinding<MatchSuccessMsgBind.MatchSuccessMsg>() {

    override suspend fun handleMessage(mqttMessage: MqttMessage<MatchSuccessMsg>, scene: Scene) {
        val matchSuccessMsg = mqttMessage.body!!
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

    override fun getClassName(type: String): Class<out MatchSuccessMsg>? {
        return if (type == MatchSuccessMsg::class.simpleName) {
            MatchSuccessMsg::class.java
        } else {
            null
        }
    }
}