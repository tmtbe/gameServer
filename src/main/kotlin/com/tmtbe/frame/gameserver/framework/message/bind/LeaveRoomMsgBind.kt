package com.tmtbe.frame.gameserver.framework.message.bind

import com.tmtbe.frame.gameserver.framework.message.MqttMessage
import com.tmtbe.frame.gameserver.framework.message.MqttMessageBinding
import com.tmtbe.frame.gameserver.framework.message.TopicTemplate
import com.tmtbe.frame.gameserver.framework.scene.Scene
import com.tmtbe.frame.gameserver.framework.service.RoomService
import com.tmtbe.frame.gameserver.framework.annotation.GameMqttMessageBinding

@GameMqttMessageBinding
class LeaveRoomMsgBind(
        private val roomService: RoomService
) : MqttMessageBinding<LeaveRoomMsgBind.LeaveRoomMsg>() {

    override fun getClassName(type: String): Class<out LeaveRoomMsg>? {
        return if (type == LeaveRoomMsg::class.simpleName) {
            LeaveRoomMsg::class.java
        } else {
            null
        }
    }

    override suspend fun handleMessage(mqttMessage: MqttMessage<LeaveRoomMsg>, scene: Scene) {
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