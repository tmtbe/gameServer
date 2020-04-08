package com.tmtbe.frame.gameserver.game.client

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestHeader
import reactivefeign.spring.config.ReactiveFeignClient
import reactor.core.publisher.Mono

@ReactiveFeignClient(name = "accountClient", url = "http://dz8888.com")
interface AccountClient {
    @GetMapping("/v1/User/Info")
    fun getUserInfo(
            @RequestHeader("token") token: String
    ): Mono<UserInfoResult>
}


data class UserInfoResult(
        val code: Int,
        val msg: String
)