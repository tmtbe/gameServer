package com.tmtbe.frame.gameserver.base.service

import com.tmtbe.frame.gameserver.base.scene.ResourceManager.Companion.PLAYER_ON_SERVER_SCENE_ROOM
import com.tmtbe.frame.gameserver.base.scene.ResourceManager.Companion.ROOM_ON_GAME_SERVER
import com.tmtbe.frame.gameserver.base.scene.ResourceManager.Companion.SCENE_HAS_ROOM_
import com.tmtbe.frame.gameserver.base.scene.ResourceManager.Companion.SCENE_ROOM_HAS_PLAYER_SUB_
import com.tmtbe.frame.gameserver.base.utils.RedisUtils
import com.tmtbe.frame.gameserver.base.utils.toJson
import com.tmtbe.frame.gameserver.base.utils.toJsonObject
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
        val redisUtils: RedisUtils,
        val emqService: EMQService
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
            redisUtils.sDel("${SCENE_HAS_ROOM_}$sceneName", roomName)
            redisUtils.hDel(ROOM_ON_GAME_SERVER, "$sceneName/$roomName")
            val topicList = redisUtils.sMembers("$SCENE_ROOM_HAS_PLAYER_SUB_$sceneName/$roomName")
            topicList.map {
                        it.toJsonObject(RoomService.PlayerRoomTopic::class.java)
                    }
                    .forEach { playerRoomTopic ->
                        val hGet = redisUtils.hGet(PLAYER_ON_SERVER_SCENE_ROOM, playerRoomTopic.playerName)
                        val playerServerRoom = hGet?.toJsonObject(RoomService.PlayerServerRoom::class.java)
                        if (playerServerRoom?.roomName == roomName && playerServerRoom.sceneName == sceneName) {
                            redisUtils.hDel(PLAYER_ON_SERVER_SCENE_ROOM, playerRoomTopic.playerName)
                        }
                        emqService.unsubscribe(playerRoomTopic.playerName, playerRoomTopic.topic)
                    }
            redisUtils.del("$SCENE_ROOM_HAS_PLAYER_SUB_$sceneName/$roomName")
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