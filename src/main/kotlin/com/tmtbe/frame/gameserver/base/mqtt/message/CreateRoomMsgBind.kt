package com.tmtbe.frame.gameserver.base.mqtt.message

import com.tmtbe.frame.gameserver.base.mqtt.MqttMessage
import com.tmtbe.frame.gameserver.base.mqtt.MqttMessageBinding
import com.tmtbe.frame.gameserver.base.mqtt.TopicTemplate
import com.tmtbe.frame.gameserver.base.service.RoomService
import kotlinx.coroutines.InternalCoroutinesApi
import org.springframework.stereotype.Component

@InternalCoroutinesApi
@Component
class CreateRoomMsgBind(
        val roomService: RoomService
) : MqttMessageBinding<CreateRoomMsgBind.CreateRoomMsg>() {

    override fun getClassName(): Class<CreateRoomMsg> = CreateRoomMsg::class.java

    override suspend fun handleMessage(mqttMessage: MqttMessage<CreateRoomMsg>) {
        val requestChannel = mqttMessage.topicParse.topicChannel as TopicTemplate.RequestChannel
        val roomName = System.currentTimeMillis().toString()
        val playerRoom = roomService.getPlayerRoom(requestChannel.getName())
        if (playerRoom != null) responseError(mqttMessage, "玩家已经在游戏房间", playerRoom)
        roomService.createRoom(mqttMessage.topicParse.scene, roomName)
        roomService.playerInterRoom(requestChannel.getName(), mqttMessage.topicParse.scene, roomName)
        responseMessage(mqttMessage, "加入房间成功")
    }

    class CreateRoomMsg
}