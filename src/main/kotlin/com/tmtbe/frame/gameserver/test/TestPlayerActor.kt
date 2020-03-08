package com.tmtbe.frame.gameserver.test

import com.tmtbe.frame.gameserver.base.actor.*
import com.tmtbe.frame.gameserver.base.scene.ResourceManager
import com.tmtbe.frame.gameserver.base.scene.Scene
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.delay

@InternalCoroutinesApi
class TestPlayerActor(name: String,
                      scene: Scene
) : PlayerActor(name, scene) {
    private var status: String = "NORAML"
    override suspend fun handleRequestMsg(msg: RequestMsg<Any, Any>): Boolean {
        msg.registerEverySecondHandle {
            getRoomActor().sendMqttToRoom("${name}: 倒计时 ${4 - it / 1000} 秒")
        }
        msg.timeOut(time = 4000, default = {
            "${name}: 超时后机器人处理"
        })
        return false
    }

    override suspend fun onCreate() {
    }

    override suspend fun onEventTime() {
        when (status) {
            "GAME_OVER" -> {
                delay((1000..3000).random().toLong())
                destroy()
            }
            else -> {

            }
        }
    }

    override fun handleNoticeMsg(noticeMsg: NoticeMsg<Any>) {
        val message = noticeMsg.message
        when (message) {
            is CMDPayload -> {
                when (message.CMD) {
                    "GAME_OVER" -> {
                        status = "GAME_OVER"
                    }
                }
            }
        }
    }

    override suspend fun handleMqttMsg(msg: MqttMsg) {
        if (hasRequestMsg()) {
            val requestMsg = getRequestMsg()
            when (val request = requestMsg.request) {
                is CMDPayload -> {
                    when (request.CMD) {
                        "GET" -> {
                            requestMsg.send(msg.payload)
                        }
                    }
                }
            }
        }
    }

    override suspend fun onRemoving(parent: Actor) {

    }

    override suspend fun onAdded(parent: Actor) {

    }
}

data class CMDPayload(
        val CMD: String
)