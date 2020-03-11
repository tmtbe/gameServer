package com.tmtbe.frame.gameserver.framework.actor

import java.time.Duration

class RoomConfiguration(
        val maxPlayerNumber: Int,
        val maxKeepAliveTime: Duration
)