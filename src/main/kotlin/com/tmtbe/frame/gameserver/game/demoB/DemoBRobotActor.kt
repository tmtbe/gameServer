package com.tmtbe.frame.gameserver.game.demoB

import com.tmtbe.frame.gameserver.framework.actor.RequestMsg
import com.tmtbe.frame.gameserver.framework.scene.Scene

class DemoBRobotActor(name: String,
                      scene: Scene
) : DemoBPlayerActor(name, scene) {
    override suspend fun handleRequestMsg(msg: RequestMsg<Any, Any>): Boolean {
        msg.registerEverySecondHandle {
            getRoomActor().sendMqttToRoom("${name}: 倒计时 ${4 - it / 1000} 秒")
        }
        msg.timeOut(time = (0..4000L).random(), default = {
            (0..100).random()
        })
        return true
    }
}