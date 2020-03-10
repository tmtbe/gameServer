package com.tmtbe.frame.gameserver.base.utils

import com.alibaba.fastjson.JSON
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select
import kotlin.coroutines.coroutineContext

suspend fun <T> Channel<T>.receiveTimeOut(
        time: Long,
        autoClose: Boolean,
        default: () -> T,
        onTimeOut: (() -> Unit)? = null,
        everySecondHandle: ((time: Long) -> Unit)? = null
): T {
    val source = this
    val default = Channel<String>()
    var nowTime = 0L
    GlobalScope.launch(coroutineContext) {
        while (nowTime < time) {
            delay(java.lang.Long.min(time, 1000))
            nowTime += java.lang.Long.min(time, 1000)
            if (default.isClosedForSend) break
            if (everySecondHandle != null) everySecondHandle(nowTime)
        }
        default.send("ok")
    }
    return select {
        source.onReceive {
            if (autoClose) source.close()
            default.close()
            it
        }
        default.onReceive {
            if (autoClose) source.close()
            if (onTimeOut != null) onTimeOut()
            default.close()
            default()
        }
    }
}

fun Any.toJson(): String = JSON.toJSONString(this)
fun <T> String.toJsonObject(clazz: Class<T>) = JSON.parseObject(this, clazz)