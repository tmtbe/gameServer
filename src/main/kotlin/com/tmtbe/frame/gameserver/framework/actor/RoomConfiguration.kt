package com.tmtbe.frame.gameserver.framework.actor

import java.time.Duration

class RoomConfiguration(
        var maxPlayerNumber: Int,
        var maxKeepAliveTime: Duration,
        var roomLevel: String? = null
)