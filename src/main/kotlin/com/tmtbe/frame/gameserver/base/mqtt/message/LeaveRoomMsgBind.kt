package com.tmtbe.frame.gameserver.base.mqtt.message

import com.tmtbe.frame.gameserver.base.mqtt.MqttMessage
import com.tmtbe.frame.gameserver.base.mqtt.MqttMessageBinding
import com.tmtbe.frame.gameserver.base.mqtt.TopicTemplate
import com.tmtbe.frame.gameserver.base.service.RoomService
import org.springframework.stereotype.Component

@Component
class LeaveRoomMsgBind(
        val roomService: RoomService
) : MqttMessageBinding<LeaveRoomMsgBind.LeaveRoomMsg>() {

    override fun getClassName(): Class<LeaveRoomMsg> = LeaveRoomMsg::class.java

    override suspend fun handleMessage(mqttMessage: MqttMessage<LeaveRoomMsg>) {
        val requestChannel = mqttMessage.topicParse.topicChannel as TopicTemplate.ClientChannel
        mustNotNull(mqttMessage.body)
        roomService.playerOuterRoom(
                requestChannel.getName(),
                mqttMessage.topicParse.scene,
                mqttMessage.body!!.roomName
        )
        responseMessage(mqttMessage, "成功退出房间")
    }

    data class LeaveRoomMsg(val roomName: String)
}