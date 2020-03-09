package com.tmtbe.frame.gameserver.test

import com.tmtbe.frame.gameserver.base.actor.*
import com.tmtbe.frame.gameserver.base.scene.Scene
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.delay
import java.time.Duration

@InternalCoroutinesApi
class TestRoomActor(name: String,
                    scene: Scene
) : RoomActor(name, scene) {
    private val needPlayerCount = 8
    private var status = ""
    private var maxCount = (20..30).random()
    private var nowCount = 0
    override fun provideRoomConfiguration(): RoomConfiguration {
        return RoomConfiguration(4, Duration.ofHours(1))
    }

    override suspend fun onAddedPlayer(playerActor: PlayerActor) {
        sendMqttToRoom("$name: ${playerActor.name}加入了房间")
        if (getPlayerActorList().size == needPlayerCount) {
            status = "START"
            sendMqttToRoom("$name: 开始")
        }
    }

    override suspend fun onRemovingPlayer(playerActor: PlayerActor) {
        sendMqttToRoom("$name: ${playerActor.name}离开了房间")
    }

    override suspend fun onCreate() {

    }

    override suspend fun onEventTime() {
        when (status) {
            "START" -> {
                delay(1000)
                getPlayerActorList().forEach { player ->
                    sendMqttToRoom("${name}: need actor ${player.name} msg")
                    val result = player.request<CMDPayload, String>(CMDPayload("GET"))
                    if (result != null) {
                        sendMqttToRoom(result)
                    }
                }
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