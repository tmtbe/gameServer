package com.tmtbe.frame.gameserver.base.client

import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import reactivefeign.spring.config.ReactiveFeignClient
import reactor.core.publisher.Mono

@ReactiveFeignClient(name = "mqttManager", url = "\${spring.mqtt.api}")
interface EMQManagerClient {
    @PostMapping("/api/v4/mqtt/subscribe")
    fun subscribe(
            @RequestBody subscribeRequest: SubscribeRequest,
            @RequestHeader("Authorization") auth: String = "Basic YWRtaW46cHVibGlj"
    ): Mono<APIResult>

    @PostMapping("/api/v4/mqtt/unsubscribe")
    fun unSubscribe(
            @RequestBody unSubscribeRequest: UnSubscribeRequest,
            @RequestHeader("Authorization") auth: String = "Basic YWRtaW46cHVibGlj"
    ): Mono<APIResult>
}

data class SubscribeRequest(
        val topic: String,
        val qos: Int,
        val clientid: String
)

data class UnSubscribeRequest(
        val topic: String,
        val clientid: String
)

data class APIResult(
        val message: String?,
        val code: Int
)