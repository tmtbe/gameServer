package com.tmtbe.frame.gameserver.base.utils

import org.springframework.data.redis.serializer.RedisSerializationContext
import org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair


class MyRedisSerializationContext<K, V>(
        private val keyTuple: SerializationPair<K>,
        private val valueTuple: SerializationPair<V>,
        private val hashKeyTuple: SerializationPair<*>,
        private val hashValueTuple: SerializationPair<*>,
        private val stringTuple: SerializationPair<String>
) : RedisSerializationContext<K, V> {
    override fun getKeySerializationPair(): SerializationPair<K> {
        return keyTuple
    }

    override fun getValueSerializationPair(): SerializationPair<V> {
        return valueTuple
    }

    override fun <HK> getHashKeySerializationPair(): SerializationPair<HK> {
        return hashKeyTuple as SerializationPair<HK>
    }

    override fun <HV> getHashValueSerializationPair(): SerializationPair<HV> {
        return hashValueTuple as SerializationPair<HV>
    }

    override fun getStringSerializationPair(): SerializationPair<String> {
        return stringTuple
    }
}
