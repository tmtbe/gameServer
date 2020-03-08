package com.tmtbe.frame.gameserver.base.service

import com.tmtbe.frame.gameserver.base.client.EMQManagerClient
import com.tmtbe.frame.gameserver.base.client.SubscribeRequest
import com.tmtbe.frame.gameserver.base.client.UnSubscribeRequest
import com.tmtbe.frame.gameserver.base.utils.RedisUtils
import org.springframework.stereotype.Service

@Service
class EMQService(
        private val redisUtils: RedisUtils,
        private val emqManagerClient: EMQManagerClient
) {
    suspend fun removeAllAcl(userName: String) {
        redisUtils.del("mqtt_acl:$userName")
    }

    suspend fun removeAcl(userName: String, topic: String) {
        redisUtils.hDel("mqtt_acl:$userName", topic)
    }

    suspend fun addAcl(userName: String, topic: String, acl: EMQAcl) {
        redisUtils.hSet("mqtt_acl:$userName", topic, acl.code)
    }

    suspend fun addUser(userName: String, password: String, isSuperuser: Boolean = false) {
        redisUtils.hSet("mqtt_user:$userName", password, password)
        if (isSuperuser) {
            redisUtils.hSet("mqtt_user:$userName", "is_superuser", 1)
        }
    }

    suspend fun subscribe(topic: String,
                          qos: Int,
                          userName: String) {
        emqManagerClient.subscribe(SubscribeRequest(topic, qos, userName))
    }

    suspend fun unSubscribe(topic: String, userName: String) {
        emqManagerClient.unSubscribe(UnSubscribeRequest(topic, userName))
    }
}

enum class EMQAcl(var code: Int) {
    SUBSCRIBE(1), PUBLISH(2), PUBSUB(3)
}