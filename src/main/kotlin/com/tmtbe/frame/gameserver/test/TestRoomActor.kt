package com.tmtbe.frame.gameserver.test

import com.tmtbe.frame.gameserver.base.*
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.delay

@InternalCoroutinesApi
class TestRoomActor(name: String,
                    scene: Scene,
                    resourceManager: ResourceManager
) : RoomActor(name, scene, resourceManager) {
    private val needPlayerCount = 8
    private var status = ""
    private var maxCount = (20..30).random()
    private var nowCount = 0
    override fun onAddedPlayer(playerActor: PlayerActor) {
        sendToRoom("$name: ${playerActor.name}加入了房间")
        if (getPlayerActorList().size == needPlayerCount) {
            status = "START"
            sendToRoom("$name: 开始")
        }
    }

    override fun onRemovingPlayer(playerActor: PlayerActor) {
        sendToRoom("$name: ${playerActor.name}离开了房间")
        if (getPlayerActorList().size == 1) {
            destroy()
        }
    }

    override suspend fun onCreate() {

    }

    override suspend fun onEventTime() {
        when (status) {
            "START" -> {
                delay(1000)
                getPlayerActorList().forEach { player ->
                    sendToRoom("${name}: need actor ${player.name} msg")
                    val result = player.request<CMDPayload, String>(CMDPayload("GET"))
                    if (result != null) {
                        sendToRoom(result)
                    }
                }
                nowCount++
                if (nowCount > maxCount) status = "GAME_OVER"
            }
            "GAME_OVER" -> {
                sendToRoom("$name: 结束")
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