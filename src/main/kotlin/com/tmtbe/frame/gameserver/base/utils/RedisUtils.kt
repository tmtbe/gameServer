package com.tmtbe.frame.gameserver.base.utils

import kotlinx.coroutines.reactive.awaitFirst
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.stereotype.Component
import java.time.Duration

@Component
class RedisUtils(val reactiveRedisTemplate: ReactiveRedisTemplate<*, *>) {
    private var redisTemplate: ReactiveRedisTemplate<String, Any> = reactiveRedisTemplate as ReactiveRedisTemplate<String, Any>
    suspend fun expire(key: String, duration: Duration): Boolean {
        return redisTemplate.expire(key, duration).awaitFirst()
    }

    suspend fun del(key: String): Long {
        return redisTemplate.delete(key).awaitFirst()
    }

    suspend fun del(keys: Array<String>): Long {
        return redisTemplate.delete(*keys).awaitFirst()
    }

    suspend fun set(key: String, value: Any): Boolean {
        return redisTemplate.opsForValue().set(key, value).awaitFirst()
    }

    suspend fun set(key: String, value: Any, duration: Duration): Boolean {
        return redisTemplate.opsForValue().set(key, value, duration).awaitFirst()
    }

    suspend fun get(key: String): Any? {
        return redisTemplate.opsForValue()[key].awaitFirst()
    }

    suspend fun hPut(key: String, hKey: String, value: Any): Boolean {
        return redisTemplate.opsForHash<Any, Any>().put(key, hKey, value).awaitFirst()
    }

    suspend fun hPutAll(key: String, values: Map<String, Any>): Boolean {
        return redisTemplate.opsForHash<Any, Any>().putAll(key, values).awaitFirst()
    }

    suspend fun hSet(key: String, hKey: String, value: Any): Boolean {
        return redisTemplate.opsForHash<Any, Any>().put(key, hKey, value).awaitFirst()
    }

    suspend fun hMSet(key: String, map: Map<String, Any>): Boolean {
        return redisTemplate.opsForHash<Any, Any>().putAll(key, map).awaitFirst()
    }

    suspend fun hGet(key: String, hKey: String): Any? {
        return redisTemplate.opsForHash<Any, Any>()[key, hKey].awaitFirst()
    }

    suspend fun hMGet(key: String, hKeys: Collection<Any>): List<Any> {
        return redisTemplate.opsForHash<Any, Any>().multiGet(key, hKeys).awaitFirst()
    }

    suspend fun hDel(key: String, vararg hKey: String) {
        redisTemplate.opsForHash<Any, Any>().remove(key, *hKey).awaitFirst()
    }

    suspend fun hHasKey(key: String, item: String): Boolean {
        return redisTemplate.opsForHash<Any, Any>().hasKey(key, item).awaitFirst()
    }

    suspend fun sSet(key: String, vararg values: Any): Long {
        return redisTemplate.opsForSet().add(key, *values).awaitFirst()
    }

    suspend fun sDel(key: String, vararg values: Any): Long {
        return redisTemplate.opsForSet().remove(key, *values).awaitFirst()
    }

    suspend fun sHasKey(key: String, value: Any): Boolean {
        return redisTemplate.opsForSet().isMember(key, value).awaitFirst()
    }

    suspend fun lPush(key: String, value: Any): Long {
        return redisTemplate.opsForList().rightPush(key, value).awaitFirst()
    }

    suspend fun lPushAll(key: String, vararg values: Any): Long {
        return redisTemplate.opsForList().rightPushAll(key, *values).awaitFirst()
    }

    suspend fun lGet(key: String, start: Long, end: Long): Any? {
        return redisTemplate.opsForList().range(key, start, end).collectList().awaitFirst()
    }

}