package com.tmtbe.frame.gameserver.test.api

import com.tmtbe.frame.gameserver.base.client.APIResult
import com.tmtbe.frame.gameserver.base.client.EMQManagerClient
import com.tmtbe.frame.gameserver.base.client.SubscribeRequest
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

@RestController
class ManagerApi(
        val EMQManagerClient: EMQManagerClient
) {
    @PostMapping("/api/subscribe")
    fun subscribe(
            @RequestBody subscribeRequest: SubscribeRequest): Mono<APIResult> {
        return EMQManagerClient.subscribe(subscribeRequest)
    }
}