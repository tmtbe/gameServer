package com.tmtbe.frame.gameserver.base.mqtt

interface SubscribeHandle {
    fun handle(topic: String, payload: String): Boolean
    fun bindWith(): List<String>
}

interface SubscribeWith {
    fun subWith(): List<String>
}