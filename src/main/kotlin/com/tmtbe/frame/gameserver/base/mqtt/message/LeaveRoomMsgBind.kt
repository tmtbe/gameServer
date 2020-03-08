package com.tmtbe.frame.gameserver.base.mqtt.message

import com.tmtbe.frame.gameserver.base.mqtt.MqttMessage
import com.tmtbe.frame.gameserver.base.mqtt.MqttMessageBinding
import com.tmtbe.frame.gameserver.base.mqtt.TopicTemplate
import com.tmtbe.frame.gameserver.base.service.RoomService
import kotlinx.coroutines.InternalCoroutinesApi
import org.springframework.stereotype.Component

@InternalCoroutinesApi
@Component
class LeaveRoomMsgBind(
        val roomService: RoomService
) : MqttMessageBinding<LeaveRoomMsgBind.LeaveRoomMsg>() {

    override fun getType() = "LEAVE_ROOM"

    override fun getClassName(): Class<LeaveRoomMsg> = LeaveRoomMsg::class.java

    override suspend fun handleMessage(mqttMessage: MqttMessage<LeaveRoomMsg>) {
        val requestChannel = mqttMessage.topicParse.topicChannel as TopicTemplate.RequestChannel
        roomService.playerOuterRoom(requestChannel.getName(), mqttMessage.topicParse.scene, mqttMessage.body!!.roomName!!)
    }

    data class LeaveRoomMsg(val roomName: String)
}