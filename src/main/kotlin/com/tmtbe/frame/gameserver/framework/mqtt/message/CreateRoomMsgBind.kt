package com.tmtbe.frame.gameserver.framework.mqtt.message

import com.tmtbe.frame.gameserver.framework.mqtt.MqttMessage
import com.tmtbe.frame.gameserver.framework.mqtt.MqttMessageBinding
import com.tmtbe.frame.gameserver.framework.mqtt.TopicTemplate
import com.tmtbe.frame.gameserver.framework.service.RoomService
import com.tmtbe.frame.gameserver.framework.stereotype.GameMqttMessageBinding

@GameMqttMessageBinding
class CreateRoomMsgBind(
        val roomService: RoomService
) : MqttMessageBinding<CreateRoomMsgBind.CreateRoomMsg>() {

    override fun getClassName(): Class<CreateRoomMsg> = CreateRoomMsg::class.java

    override suspend fun handleMessage(mqttMessage: MqttMessage<CreateRoomMsg>) {
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