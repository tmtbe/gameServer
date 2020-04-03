package com.tmtbe.frame.gameserver.framework.message.bind

import com.tmtbe.frame.gameserver.framework.annotation.GameMqttMessageBinding
import com.tmtbe.frame.gameserver.framework.message.MqttMessage
import com.tmtbe.frame.gameserver.framework.message.MqttMessageBinding
import com.tmtbe.frame.gameserver.framework.message.TopicTemplate
import com.tmtbe.frame.gameserver.framework.message.serverError
import com.tmtbe.frame.gameserver.framework.scene.Scene
import com.tmtbe.frame.gameserver.framework.service.RoomService
import kotlinx.coroutines.reactive.awaitFirst
import org.springframework.core.io.ClassPathResource
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.data.redis.core.script.DefaultRedisScript
import org.springframework.scripting.support.ResourceScriptSource
import java.util.List
import java.util.UUID

@GameMqttMessageBinding
class MatchMsgBind(
        private val reactiveRedisTemplate: ReactiveRedisTemplate<String, *>,
        private val roomService: RoomService
) : MqttMessageBinding<MatchMsgBind.MatchMsg>() {
    private val redisScript: DefaultRedisScript<List<*>> = DefaultRedisScript<List<*>>()

    init {
        redisScript.setScriptSource(ResourceScriptSource(ClassPathResource("script/Match.lua")))
        redisScript.resultType = java.util.List::class.java
    }

    override suspend fun handleMessage(mqttMessage: MqttMessage<MatchMsg>, scene: Scene) {
        val requestChannel = mqttMessage.topicParse.topicChannel as TopicTemplate.ClientChannel
        val playerRoom = roomService.getPlayerRoom(requestChannel.getPlayerName())
        if (playerRoom != null) throw serverError("你已经在房间里，无法匹配")
        val matchName = "MATCH_" + scene.name + "_" + mqttMessage.body!!.roomLevel
        val result = reactiveRedisTemplate.execute(redisScript,
                        listOf(matchName, requestChannel.getPlayerName()),
                        listOf(scene.configuration.matchedNeedPlayerNum.toString()))
                .awaitFirst()
        responseMessage(mqttMessage, MatchMsgResp())
        if (result.isNotEmpty()) {
            resourceManager.sendMqttMessage(MqttMessage(UUID.randomUUID().toString(),
                    MatchSuccessMsgBind.MatchSuccessMsg::class.simpleName!!,
                    MatchSuccessMsgBind.MatchSuccessMsg(scene.name, result.map { it.toString() }, mqttMessage.body.roomLevel),
                    TopicTemplate.TopicParse(TopicTemplate.InternalServerChannel(resourceManager.serverName), scene.name, "all")
            ))
        }
    }

    class MatchMsg(
            val roomLevel: String
    )

    class MatchMsgResp

    override fun getClassName(type: String): Class<out MatchMsg>? {
        return if (type == MatchMsg::class.simpleName) {
            MatchMsg::class.java
        } else {
            null
        }
    }
}