package com.tmtbe.frame.gameserver.game.demoB

import com.tmtbe.frame.gameserver.framework.actor.*
import com.tmtbe.frame.gameserver.framework.scene.Scene
import kotlinx.coroutines.delay
import java.time.Duration

class DemoBRoomActor(name: String,
                     scene: Scene
) : RoomActor(name, scene) {
    private var status = "INIT"
    private var maxCount = (20..30).random()
    private var nowCount = 0
    override fun provideRoomConfiguration(): RoomConfiguration {
        return RoomConfiguration(4, Duration.ofSeconds(20))
    }

    override suspend fun onAddedPlayer(playerActor: PlayerActor) {
        sendMqttToRoom("$name: ${playerActor.name}加入了房间")
        if (getPlayerActorList().size == provideRoomConfiguration().maxPlayerNumber) {
            status = "START"
            sendMqttToRoom("$name: 开始")
        }
    }

    override suspend fun onRemovedPlayer(playerActor: PlayerActor) {
        sendMqttToRoom("$name: ${playerActor.name}离开了房间")
    }

    override suspend fun onCreate() {

    }

    override suspend fun onEventTime() {
        when (status) {
            "INIT" -> {
                if (getRunTime() > 3000) status = "INIT_TIMEOUT"
            }
            "INIT_TIMEOUT" -> {
                if (!isFull()) {
                    scene.createPlayer(roomName, "robot${children.size}", DemoBRobotActor::class.java)
                }
            }
            "START" -> {
                delay(1000)
                getPlayerActorList().forEach { player ->
                    sendMqttToRoom("${name}: need actor ${player.name} msg")
                }
                val groupRequest = getPlayerActorList().groupRequest<CMDPayload, Int>(CMDPayload("GET"))
                val maxBy = groupRequest.maxBy { it.value }!!
                sendMqttToRoom("最大的值是：${maxBy.value},来自${maxBy.key.name}")
                nowCount++
                if (nowCount > maxCount) status = "GAME_OVER"
            }
            "GAME_OVER" -> {
                sendMqttToRoom("$name: 结束")
                getPlayerActorList().forEach { player ->
                    player.send(NoticeMsg(CMDPayload("GAME_OVER")))
                }
            }
        }
    }

    override fun handleNoticeMsg(noticeMsg: NoticeMsg<Any>) {

    }

    override suspend fun handleRequestMsg(msg: RequestMsg<Any, Any>): Boolean {
        return false
    }

    override suspend fun handleMqttMsg(msg: MqttMsg) {

    }
}