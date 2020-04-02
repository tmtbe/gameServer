package com.tmtbe.frame.gameserver.framework.message.bind

import com.tmtbe.frame.gameserver.framework.annotation.GameMqttMessageBinding
import com.tmtbe.frame.gameserver.framework.message.MqttMessage
import com.tmtbe.frame.gameserver.framework.message.MqttMessageBinding
import com.tmtbe.frame.gameserver.framework.message.TopicTemplate
import com.tmtbe.frame.gameserver.framework.message.sub.MatchSuccessSub
import com.tmtbe.frame.gameserver.framework.scene.Scene
import com.tmtbe.frame.gameserver.framework.service.RoomService
import com.tmtbe.frame.gameserver.framework.utils.RedisUtils
import com.tmtbe.frame.gameserver.framework.utils.toJson
import kotlinx.coroutines.reactive.awaitFirst
import org.springframework.core.io.ClassPathResource
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.data.redis.core.script.DefaultRedisScript
import org.springframework.scripting.support.ResourceScriptSource
import java.util.List

@GameMqttMessageBinding
class MatchMsgBind(
        private val roomService: RoomService,
        private val redisUtils: RedisUtils,
        private val reactiveRedisTemplate: ReactiveRedisTemplate<String, *>
) : MqttMessageBinding<MatchMsgBind.MatchMsg>() {
    private val redisScript: DefaultRedisScript<List<*>> = DefaultRedisScript<List<*>>()

    init {
        redisScript.setScriptSource(ResourceScriptSource(ClassPathResource("script/Match.lua")))
        redisScript.resultType = java.util.List::class.java
    }

    override suspend fun handleMessage(mqttMessage: MqttMessage<MatchMsg>, scene: Scene) {
        val requestChannel = mqttMessage.topicParse.topicChannel as TopicTemplate.ClientChannel
        val matchName = "MATCH_" + scene.name + "_" + mqttMessage.body!!.roomLevel
        val result = reactiveRedisTemplate.execute(redisScript,
                        listOf(matchName, requestChannel.getPlayerName()),
                        listOf(scene.configuration.matchedNeedPlayerNum.toString()))
                .awaitFirst()
        if (result.isNotEmpty()) {
            resourceManager.getMqttGatWay().sendToMqtt(
                    MatchSuccessSub.MatchSuccessMsg(scene.name, result.map { it.toString() }, mqttMessage.body.roomLevel).toJson(),
                    "MATCHED/${scene.name}")
        }
    }

    class MatchMsg(
            val roomLevel: String
    )

    override fun getClassName(type: String): Class<out MatchMsg>? {
        if (type.equals(MatchMsg::class.simpleName)) {
            return MatchMsg::class.java
        } else {
            return null
        }
    }
}