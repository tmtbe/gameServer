package com.tmtbe.frame.gameserver.framework.message.bind

import com.tmtbe.frame.gameserver.framework.message.MqttMessage
import com.tmtbe.frame.gameserver.framework.message.MqttMessageBinding
import com.tmtbe.frame.gameserver.framework.message.TopicTemplate
import com.tmtbe.frame.gameserver.framework.scene.Scene
import com.tmtbe.frame.gameserver.framework.service.RoomService
import com.tmtbe.frame.gameserver.framework.annotation.GameMqttMessageBinding

@GameMqttMessageBinding
class CreateRoomMsgBind(
        private val roomService: RoomService
) : MqttMessageBinding<CreateRoomMsgBind.CreateRoomMsg>() {

    override fun getClassName(type: String) = CreateRoomMsg::class.java

    override suspend fun handleMessage(mqttMessage: MqttMessage<CreateRoomMsg>, scene: Scene) {
        val requestChannel = mqttMessage.topicParse.topicChannel as TopicTemplate.ClientChannel
        val roomName = System.currentTimeMillis().toString()
        val playerRoom = roomService.getPlayerRoom(requestChannel.getName())
        if (playerRoom != null) responseError(mqttMessage, "玩家已经在游戏房间", playerRoom)
        roomService.createRoom(mqttMessage.topicParse.scene, roomName)
        roomService.playerInterRoom(requestChannel.getName(), mqttMessage.topicParse.scene, roomName)
        responseMessage(mqttMessage, RoomMsgResp(resourceManager.serverName, mqttMessage.topicParse.scene, roomName))
    }

    class CreateRoomMsg

    class RoomMsgResp(
            val serverName: String,
            val sceneName: String,
            val roomName: String
    )
}