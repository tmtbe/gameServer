package com.tmtbe.frame.gameserver.framework.message.bind

import com.tmtbe.frame.gameserver.framework.message.FindActorMessageClass
import com.tmtbe.frame.gameserver.framework.message.MqttMessage
import com.tmtbe.frame.gameserver.framework.message.MqttMessageBinding
import com.tmtbe.frame.gameserver.framework.scene.Scene
import com.tmtbe.frame.gameserver.framework.service.RoomService
import com.tmtbe.frame.gameserver.framework.annotation.GameMqttMessageBinding

@GameMqttMessageBinding
class ActorMsgBind(
        private val roomService: RoomService,
        private val findActorMessageClass: FindActorMessageClass
) : MqttMessageBinding<Any>() {
    init {
        findActorMessageClass
    }

    override suspend fun handleMessage(mqttMessage: MqttMessage<Any>, scene: Scene) {
        forwardToPlayerActor(mqttMessage)
    }

    override fun getClassName(type: String): Class<out Any>? {
        return findActorMessageClass.classList.firstOrNull {
            it.simpleName == type
        }
    }
}