package com.tmtbe.frame.gameserver.framework.utils

import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair
import org.springframework.data.redis.serializer.RedisSerializer
import org.springframework.stereotype.Component
import java.time.Duration

@Configuration
class RedisConfig {
    @Bean
    @Primary
    fun getReactiveRedisTemplate(connectionFactory: ReactiveRedisConnectionFactory): ReactiveRedisTemplate<String, String> {
        val myRedisSerializationContext = MyRedisSerializationContext<String, String>(
                SerializationPair.fromSerializer(RedisSerializer.string()),
                SerializationPair.fromSerializer(RedisSerializer.string()),
                SerializationPair.fromSerializer(RedisSerializer.string()),
                SerializationPair.fromSerializer(RedisSerializer.string()),
                SerializationPair.fromSerializer(RedisSerializer.string())
        )
        return ReactiveRedisTemplate(connectionFactory, myRedisSerializationContext)
    }
}

@Component
class RedisUtils(reactiveRedisTemplate: ReactiveRedisTemplate<*, *>) {
    private var redisTemplate: ReactiveRedisTemplate<String, String> = reactiveRedisTemplate as ReactiveRedisTemplate<String, String>
    suspend fun expire(key: String, duration: Duration): Boolean {
        return redisTemplate.expire(key, duration).awaitFirst()
    }

    suspend fun del(key: String): Long {
        return redisTemplate.delete(key).awaitFirst()
    }

    suspend fun del(keys: Array<String>): Long {
        return redisTemplate.delete(*keys).awaitFirst()
    }

    suspend fun set(key: String, value: String): Boolean {
        return redisTemplate.opsForValue().set(key, value).awaitFirst()
    }

    suspend fun set(key: String, value: String, duration: Duration): Boolean {
        return redisTemplate.opsForValue().set(key, value, duration).awaitFirst()
    }

    suspend fun get(key: String): String? {
        return redisTemplate.opsForValue()[key].awaitFirstOrNull()
    }

    suspend fun hPut(key: String, hKey: String, value: String): Boolean {
        return redisTemplate.opsForHash<String, String>().put(key, hKey, value).awaitFirst()
    }

    suspend fun hPutAll(key: String, values: Map<String, String>): Boolean {
        return redisTemplate.opsForHash<String, String>().putAll(key, values).awaitFirst()
    }

    suspend fun hSet(key: String, hKey: String, value: String): Boolean {
        return redisTemplate.opsForHash<String, String>().put(key, hKey, value).awaitFirst()
    }

    suspend fun hMSet(key: String, map: Map<String, String>): Boolean {
        return redisTemplate.opsForHash<String, String>().putAll(key, map).awaitFirst()
    }

    suspend fun hGet(key: String, hKey: String): String? {
        return redisTemplate.opsForHash<String, String>()[key, hKey].awaitFirstOrNull()
    }

    suspend fun hMGet(key: String, hKeys: Collection<String>): List<String>? {
        return redisTemplate.opsForHash<String, String>().multiGet(key, hKeys).awaitFirstOrNull()
    }

    suspend fun hDel(key: String, vararg hKey: String): Long {
        return redisTemplate.opsForHash<String, String>().remove(key, *hKey).awaitFirst()
    }

    suspend fun hHasKey(key: String, item: String): Boolean {
        return redisTemplate.opsForHash<String, String>().hasKey(key, item).awaitFirst()
    }

    suspend fun sMembers(key: String): List<String> {
        return redisTemplate.opsForSet().members(key).collectList().awaitFirst()
    }

    suspend fun sSet(key: String, vararg values: String): Long {
        return redisTemplate.opsForSet().add(key, *values).awaitFirst()
    }

    suspend fun sDel(key: String, vararg values: String): Long {
        return redisTemplate.opsForSet().remove(key, *values).awaitFirst()
    }

    suspend fun sHasKey(key: String, value: String): Boolean {
        return redisTemplate.opsForSet().isMember(key, value).awaitFirst()
    }

    suspend fun lPush(key: String, value: String): Long {
        return redisTemplate.opsForList().rightPush(key, value).awaitFirst()
    }

    suspend fun lPushAll(key: String, vararg values: String): Long {
        return redisTemplate.opsForList().rightPushAll(key, *values).awaitFirst()
    }

    suspend fun lGet(key: String, start: Long, end: Long): List<String>? {
        return redisTemplate.opsForList().range(key, start, end).collectList().awaitFirstOrNull()
    }

}