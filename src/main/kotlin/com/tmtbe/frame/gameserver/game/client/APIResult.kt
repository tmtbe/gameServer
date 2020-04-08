package com.tmtbe.frame.gameserver.game.client

data class APIResult<T>(
        val code: Int,
        val msg: String?,
        val data: T?
)