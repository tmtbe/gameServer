package com.tmtbe.frame.gameserver.game.demoB.message

import com.tmtbe.frame.gameserver.framework.annotation.GameActorMessage

@GameActorMessage
data class SendIntMsg(
        val value: Int
)