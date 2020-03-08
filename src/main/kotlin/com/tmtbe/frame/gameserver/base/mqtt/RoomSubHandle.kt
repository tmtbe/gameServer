package com.tmtbe.frame.gameserver.base.mqtt

import kotlinx.coroutines.InternalCoroutinesApi
import org.springframework.stereotype.Component


@InternalCoroutinesApi
@Component
class RoomSubHandle(val requestSubWith: RequestSubWith) : SubscribeHandle {
    override fun handle(topic: String, payload: String): Boolean {
        return false
    }

    override fun bindWith(): List<String> {
        return requestSubWith.subWith()
    }
}