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
    ): Mono<APIResult<UserInfo>>
}

data class UserInfo(
        val ssc: Int? = null,
        val title: String? = null,
        val experience: Int? = null,
        val pc28: Int? = null,
        val syx5: Int? = null,
        val isSafePwd: Int? = null,
        val nickname: String? = null,
        val id: Int? = null,
        val isMobile: Int? = null,
        val fc3d: Int? = null,
        val chess: Int? = null,
        val level: String? = null,
        val lhc: Int? = null,
        val mobile: String? = null,
        val k3: Int? = null,
        val isagent: String? = null,
        val lastLoginArea: String? = null,
        val avatar: String? = null,
        val pk10: Int? = null,
        val isSafeQuestion: Int? = null,
        val lasttime: String? = null,
        val money: Double? = null,
        val safeLevel: Int? = null,
        val pl3: Int? = null,
        val username: String? = null
)