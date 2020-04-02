package com.tmtbe.frame.gameserver.framework.message.bind

import com.tmtbe.frame.gameserver.framework.annotation.GameMqttMessageBinding
import com.tmtbe.frame.gameserver.framework.message.MqttMessage
import com.tmtbe.frame.gameserver.framework.message.MqttMessageBinding
import com.tmtbe.frame.gameserver.framework.message.TopicTemplate
import com.tmtbe.frame.gameserver.framework.scene.Scene
import com.tmtbe.frame.gameserver.framework.utils.RedisUtils

@GameMqttMessageBinding
class CancelMatchMsgBind(
        private val redisUtils: RedisUtils
) : MqttMessageBinding<CancelMatchMsgBind.CancelMatchMsg>() {

    override suspend fun handleMessage(mqttMessage: MqttMessage<CancelMatchMsg>, scene: Scene) {
        val requestChannel = mqttMessage.topicParse.topicChannel as TopicTemplate.ClientChannel
        val matchName = "MATCH_" + scene.name + "_" + mqttMessage.body!!.roomLevel
        redisUtils.sDel(matchName, requestChannel.getPlayerName())
    }

    class CancelMatchMsg(
            val roomLevel: String
    )

    override fun getClassName(type: String): Class<out CancelMatchMsg>? {
        return if (type == CancelMatchMsg::class.simpleName) {
            CancelMatchMsg::class.java
        } else {
            null
        }
    }
}