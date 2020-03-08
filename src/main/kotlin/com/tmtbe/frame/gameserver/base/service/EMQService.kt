package com.tmtbe.frame.gameserver.base.service

import com.tmtbe.frame.gameserver.base.client.EMQManagerClient
import com.tmtbe.frame.gameserver.base.client.SubscribeRequest
import com.tmtbe.frame.gameserver.base.client.UnSubscribeRequest
import com.tmtbe.frame.gameserver.base.utils.RedisUtils
import kotlinx.coroutines.reactive.awaitFirst
import org.springframework.stereotype.Service

@Service
class EMQService(
        private val redisUtils: RedisUtils,
        private val emqManagerClient: EMQManagerClient
) {
    suspend fun removeAllAcl(userName: String): Long {
        return redisUtils.del("mqtt_acl:$userName")
    }

    suspend fun removeAcl(userName: String, topic: String): Long {
        return redisUtils.hDel("mqtt_acl:$userName", topic)
    }

    suspend fun addAcl(userName: String, topic: String, acl: EMQAcl): Boolean {
        return redisUtils.hSet("mqtt_acl:$userName", topic, acl.code.toString())
    }

    suspend fun addUser(userName: String, password: String, isSuperuser: Boolean = false) {
        redisUtils.hSet("mqtt_user:$userName", password, password)
        if (isSuperuser) {
            redisUtils.hSet("mqtt_user:$userName", "is_superuser", "1")
        }
    }

    suspend fun subscribe(userName: String, topic: String,
                          qos: Int = 1) {
        val addAcl = addAcl(userName, topic, EMQAcl.SUBSCRIBE)
        val subscribe = emqManagerClient.subscribe(SubscribeRequest(topic, qos, userName)).awaitFirst()
        if (subscribe.code != 0) {
            error("调用订阅API错误")
        }
    }

    suspend fun unSubscribe(userName: String, topic: String) {
        removeAcl(userName, topic)
        val unSubscribe = emqManagerClient.unSubscribe(UnSubscribeRequest(topic, userName)).awaitFirst()
        if (unSubscribe.code != 0) {
            error("调用取消订阅API错误")
        }
    }
}

enum class EMQAcl(var code: Int) {
    SUBSCRIBE(1), PUBLISH(2), PUBSUB(3)
}