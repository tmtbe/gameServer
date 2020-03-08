package com.tmtbe.frame.gameserver.base.mqtt.message

import com.tmtbe.frame.gameserver.base.mqtt.MqttMessage
import com.tmtbe.frame.gameserver.base.mqtt.MqttMessageBinding
import com.tmtbe.frame.gameserver.base.mqtt.TopicTemplate
import com.tmtbe.frame.gameserver.base.service.RoomService
import kotlinx.coroutines.InternalCoroutinesApi
import org.springframework.stereotype.Component

@InternalCoroutinesApi
@Component
class CreateRoomMsgBing(
        val roomService: RoomService
) : MqttMessageBinding<CreateRoomMsgBing.CreateRoomMsg>() {

    override fun getType() = "CREATE_ROOM"

    override fun getClassName(): Class<CreateRoomMsg> = CreateRoomMsg::class.java

    override suspend fun handleMessage(mqttMessage: MqttMessage<CreateRoomMsg>) {
        val requestChannel = mqttMessage.topicParse.topicChannel as TopicTemplate.RequestChannel
        roomService.createRoom(mqttMessage.topicParse.scene, "room11")
        roomService.playerInterRoom(requestChannel.getName(), mqttMessage.topicParse.scene, "room11")
    }

    class CreateRoomMsg {

    }
}