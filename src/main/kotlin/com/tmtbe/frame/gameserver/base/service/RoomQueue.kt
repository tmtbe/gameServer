package com.tmtbe.frame.gameserver.base.service

import com.alibaba.fastjson.JSON
import com.tmtbe.frame.gameserver.base.scene.ResourceManager.Companion.ROOM_ON_GAME_SERVER
import com.tmtbe.frame.gameserver.base.scene.ResourceManager.Companion.ROOM_ON_SCENE_
import com.tmtbe.frame.gameserver.base.utils.RedisUtils
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.launch
import org.springframework.amqp.core.Message
import org.springframework.amqp.core.MessageProperties
import org.springframework.amqp.rabbit.annotation.Exchange
import org.springframework.amqp.rabbit.annotation.Exchange.TRUE
import org.springframework.amqp.rabbit.annotation.Queue
import org.springframework.amqp.rabbit.annotation.QueueBinding
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.stereotype.Component
import java.time.Duration

@Component
class RoomQueue(
        val rabbitTemplate: RabbitTemplate,
        val redisUtils: RedisUtils
) {
    @InternalCoroutinesApi
    @RabbitListener(bindings = [
        QueueBinding(
                value = Queue("room"),
                exchange = Exchange("exchange-room", delayed = TRUE),
                key = ["room"]
        )
    ])
    fun closeRoom(roomInfoJson: String) {
        GlobalScope.launch {
            val roomInfo = roomInfoJson.toJsonObject(RoomInfo::class.java)
            val sceneName = roomInfo.sceneName
            val roomName = roomInfo.roomName
            redisUtils.sDel("${ROOM_ON_SCENE_}$sceneName", roomName)
            redisUtils.hDel(ROOM_ON_GAME_SERVER, "$sceneName/$roomName")
        }
    }

    fun sendRoomInfo(roomInfo: RoomInfo, maxKeepAliveTime: Duration) {
        val exchange = "exchange-room"
        val routingKey = "room"
        val properties = MessageProperties()
        properties.delay = maxKeepAliveTime.toMillis().toInt()
        val message = Message(roomInfo.toJson().toByteArray(), properties)
        rabbitTemplate.send(exchange, routingKey, message)
    }

    data class RoomInfo(
            val sceneName: String,
            val roomName: String
    )
}

fun Any.toJson(): String = JSON.toJSONString(this)
fun <T> String.toJsonObject(clazz: Class<T>) = JSON.parseObject(this, clazz)