package com.tmtbe.frame.gameserver.base.actor

import java.time.Duration

class RoomConfiguration(
        val maxPlayerNumber: Int,
        val maxKeepAliveTime: Duration
)