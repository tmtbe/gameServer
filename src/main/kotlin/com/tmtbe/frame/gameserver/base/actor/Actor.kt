package com.tmtbe.frame.gameserver.base.actor

import com.tmtbe.frame.gameserver.base.mqtt.MqttMessage
import com.tmtbe.frame.gameserver.base.mqtt.TopicTemplate
import com.tmtbe.frame.gameserver.base.mqtt.serverError
import com.tmtbe.frame.gameserver.base.scene.ResourceManager
import com.tmtbe.frame.gameserver.base.scene.Scene
import com.tmtbe.frame.gameserver.base.utils.SpringUtils
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.selects.select
import java.lang.Long.min
import java.time.Duration
import java.util.LinkedList
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.coroutineContext

@InternalCoroutinesApi
abstract class Actor(
        var name: String,
        val scene: Scene
) {
    protected var resourceManager: ResourceManager = SpringUtils.getBean(ResourceManager::class.java)
    protected var topicTemplate: TopicTemplate = SpringUtils.getBean(TopicTemplate::class.java)
    private var requestMsgList: LinkedList<RequestMsg<Any, Any>> = LinkedList()
    var parent: Actor? = null
    val children: ConcurrentHashMap<String, Actor> = ConcurrentHashMap()
    private lateinit var sendChannel: SendChannel<ActorMsg>
    private val startTime: Long = System.currentTimeMillis()

    @Volatile
    private var isStartDestroy: Boolean = false
    private var job: Job? = null

    init {
        job = GlobalScope.launch {
            sendChannel = actor(coroutineContext) {
                for (msg in channel) {
                    onReceive(msg)
                }
            }
            onStart(isActive)
        }
    }

    protected abstract fun getMaxKeepAliveTime(): Duration

    protected fun sendMqttMessage(mqttMessage: MqttMessage<*>) {
        resourceManager.sendMqttMessage(mqttMessage)
    }

    protected fun getRequestMsg(): RequestMsg<Any, Any> {
        return requestMsgList.pop()
    }

    protected fun hasRequestMsg(): Boolean {
        return !requestMsgList.isEmpty()
    }

    protected fun removeFirstRequestMsg() {
        requestMsgList.removeFirst()
    }

    protected abstract suspend fun onCreate()

    protected abstract suspend fun onEventTime()

    private suspend fun onStart(isActive: Boolean) {
        onCreate()
        while (isActive) {
            if (System.currentTimeMillis() - startTime > getMaxKeepAliveTime().toMillis()) destroy()
            onEventTime()
            delay(100)
        }
    }

    private suspend fun onReceive(msg: ActorMsg) {
        when (msg) {
            is MqttMsg -> {
                handleMqttMsg(msg)
            }
            is NoticeMsg<*> -> {
                handleNoticeMsg((msg as NoticeMsg<Any>))
            }
            is RequestMsg<*, *> -> {
                (msg as RequestMsg<Any, Any>).registerTimerOutHandle {
                    requestMsgList.remove(msg)
                }
                if (!handleRequestMsg(msg)) {
                    requestMsgList.add(msg)
                }
            }
        }
    }

    protected abstract fun handleNoticeMsg(noticeMsg: NoticeMsg<Any>)

    /**
     * 没有处理成功的RequestMsg会压到RequestMsgList队列中去
     */
    protected abstract suspend fun handleRequestMsg(msg: RequestMsg<Any, Any>): Boolean

    protected abstract suspend fun handleMqttMsg(msg: MqttMsg)

    protected abstract suspend fun onRemoving(parent: Actor)

    protected abstract suspend fun onAdded(parent: Actor)

    protected abstract suspend fun onAddedChild(child: Actor)

    protected abstract suspend fun onRemovingChild(child: Actor)

    open suspend fun addChild(child: Actor) {
        if (child.parent != null) serverError("错误操作，不允许加入")
        if (child.isStartDestroy) serverError("销毁状态，不允许加入")
        if (this.isStartDestroy) serverError("销毁状态，不允许加入")
        children[child.name] = child
        child.parent = this
        child.onAdded(this)
        this.onAddedChild(child)
    }

    suspend fun removeChild(child: Actor) {
        children.remove(child.name)
        child.destroy()
    }

    suspend fun removeChild(name: String) {
        val actor = children[name]
        if (actor != null) {
            children.remove(name)
            actor.destroy()
        }
    }

    fun send(actorMsg: ActorMsg) {
        GlobalScope.launch {
            sendChannel.send(actorMsg)
        }
    }

    open suspend fun destroy() {
        if (isStartDestroy) return
        job?.cancel()
        isStartDestroy = true
        onRemoving(this)
        if (this.parent != null) {
            this.parent!!.onRemovingChild(this)
            this.parent!!.removeChild(this)
            this.parent = null
        }
        resourceManager.removeActor(this)
        val keys = children.keys.toList()
        for (element in keys) {
            val child = children[element]
            child?.parent?.removeChild(child)
            child?.destroy()
        }
        children.clear()
        this.requestMsgList.forEach {
            it.destroy()
        }
        this.requestMsgList.clear()
        if (::sendChannel.isInitialized) {
            this.sendChannel.close()
        }
    }

    suspend fun <E, T> request(requestPayLoad: E): T? {
        if (isStartDestroy) return null
        val response = CompletableDeferred<T>()
        val requestMsg = RequestMsg<T, E>(requestPayLoad)
        requestMsg.response = response
        sendChannel.send(requestMsg)
        return response.await()
    }
}

sealed class ActorMsg
class NoticeMsg<E>(val message: E) : ActorMsg()
class MqttMsg(val payload: MqttMessage<*>) : ActorMsg()
class RequestMsg<T, E>(val request: E) : ActorMsg() {
    var response: CompletableDeferred<T>? = null
    private var timeOutHandlers: LinkedList<(() -> Unit)> = LinkedList()
    private var everySecondHandles: LinkedList<((time: Long) -> Unit)> = LinkedList()
    private var channel = Channel<T>()

    fun registerTimerOutHandle(onTimeOut: () -> Unit) {
        timeOutHandlers.add(onTimeOut)
    }

    fun registerEverySecondHandle(everySecondHandle: ((time: Long) -> Unit)) {
        everySecondHandles.add(everySecondHandle)
    }

    fun destroy() {
        response = null
        timeOutHandlers.clear()
        everySecondHandles.clear()
    }

    @InternalCoroutinesApi
    suspend fun timeOut(
            time: Long,
            default: () -> T
    ) {
        GlobalScope.launch(coroutineContext) {
            response?.complete(channel.receiveTimeOut(time, true, default, {
                timeOutHandlers.forEach { run ->
                    run()
                }
            }, {
                everySecondHandles.forEach { run ->
                    run(it)
                }
            }))
        }
    }

    suspend fun send(msg: T) {
        GlobalScope.launch(coroutineContext) {
            channel.send(msg)
        }
    }

    suspend fun after(time: Long, callback: () -> Unit) {
        GlobalScope.launch(coroutineContext) {
            delay(time)
            callback()
        }
    }
}

@ExperimentalCoroutinesApi
@InternalCoroutinesApi
suspend fun <T> Channel<T>.receiveTimeOut(
        time: Long,
        autoClose: Boolean,
        default: () -> T,
        onTimeOut: (() -> Unit)? = null,
        everySecondHandle: ((time: Long) -> Unit)? = null
): T {
    val source = this
    val defalut = Channel<T>()
    var nowTime = 0L
    GlobalScope.launch(coroutineContext) {
        while (nowTime < time) {
            delay(min(time, 1000))
            nowTime += min(time, 1000)
            if (defalut.isClosedForSend) break
            if (everySecondHandle != null) everySecondHandle(nowTime)
        }
        defalut.close()
    }
    return select {
        source.onReceive {
            if (autoClose) source.close()
            defalut.close()
            it
        }
        defalut.onReceiveOrClosed {
            if (autoClose) source.close()
            if (onTimeOut != null) onTimeOut()
            default()
        }
    }
}