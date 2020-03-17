package com.tmtbe.frame.gameserver.framework.actor

import com.tmtbe.frame.gameserver.framework.message.MqttMessage
import com.tmtbe.frame.gameserver.framework.message.TopicTemplate
import com.tmtbe.frame.gameserver.framework.message.serverError
import com.tmtbe.frame.gameserver.framework.scene.ResourceManager
import com.tmtbe.frame.gameserver.framework.scene.Scene
import com.tmtbe.frame.gameserver.framework.utils.SpringUtils
import com.tmtbe.frame.gameserver.framework.utils.log
import com.tmtbe.frame.gameserver.framework.utils.receiveTimeOut
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.actor
import java.time.Duration
import java.util.LinkedList
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.coroutineContext

abstract class Actor(
        var name: String,
        val scene: Scene
) {
    protected val log = log()
    protected var resourceManager: ResourceManager = SpringUtils.getBean(ResourceManager::class.java)
    protected var topicTemplate: TopicTemplate = SpringUtils.getBean(TopicTemplate::class.java)
    private var requestMsgList: LinkedList<RequestMsg<Any, Any>> = LinkedList()
    var parent: Actor? = null
    val children: ConcurrentHashMap<String, Actor> = ConcurrentHashMap()
    private lateinit var sendChannel: SendChannel<ActorMsg>
    private val startTime: Long = System.currentTimeMillis()
    private val onDestroyHook: ArrayList<(suspend (Actor) -> Unit)> = ArrayList()

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
            // 由于是父类的init先执行，协程运行时机不确定，有可能会出现空指针现象，所以这里需要delay一下再执行onStart
            delay(10)
            onStart(isActive)
        }
    }

    /**
     * 添加销毁的回调
     */
    fun addHookOnDestroy(hook: suspend (Actor) -> Unit) {
        onDestroyHook.add(hook)
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
            if (getRunTime() > getMaxKeepAliveTime().toMillis()) destroy()
            try {
                onEventTime()
            } catch (e: Throwable) {
                log.error("onEventTime发生错误", e)
            }
            delay(100)
        }
    }

    protected fun getRunTime() = System.currentTimeMillis() - startTime

    private suspend fun onReceive(msg: ActorMsg) {
        when (msg) {
            is AddActorMsg<*> -> {
                try {
                    addChild(msg.actor)
                    msg.response.complete(AddActorMsg.AddActorResult(true, null))
                } catch (e: Throwable) {
                    msg.response.complete(AddActorMsg.AddActorResult(false, e.message))
                }
            }
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

    protected abstract suspend fun onRemoved(parent: Actor)

    protected abstract suspend fun onAdded(parent: Actor)

    protected abstract suspend fun onAddedChild(child: Actor)

    protected abstract suspend fun onRemovedChild(child: Actor)

    protected open suspend fun addChild(child: Actor) {
        if (child.parent != null) serverError("child有parent，不允许加入")
        if (child.isStartDestroy) serverError("child处于销毁状态，不允许加入")
        if (this.isStartDestroy) serverError("自身处于销毁状态，不允许加入")
        children[child.name] = child
        child.parent = this
        child.onAdded(this)
        this.onAddedChild(child)
        child.addHookOnDestroy {
            log.info("remove child: ${it.name}")
            this@Actor.children.remove(it.name)
            this@Actor.onRemovedChild(it)
            it.onRemoved(this@Actor)
        }
    }

    protected open suspend fun removeChild(child: Actor) {
        child.destroy()
    }

    protected open suspend fun removeChild(name: String) {
        children[name]?.destroy()
    }

    fun send(actorMsg: ActorMsg) {
        GlobalScope.launch {
            sendChannel.send(actorMsg)
        }
    }

    open suspend fun destroy() {
        if (isStartDestroy) return
        isStartDestroy = true
        val keys = children.keys.toList()
        for (element in keys) {
            val child = children[element]
            child?.destroy()
        }
        children.clear()
        job?.cancel()
        onDestroyHook.forEach {
            it(this@Actor)
        }
        onDestroyHook.clear()
        this.parent = null
        this.requestMsgList.forEach {
            it.destroy()
        }
        this.requestMsgList.clear()
        if (::sendChannel.isInitialized) {
            this.sendChannel.close()
        }
    }

    suspend fun <E, T> requestAwait(requestPayLoad: E): T {
        return request<E, T>(requestPayLoad).await()
    }

    suspend fun <E, T> request(requestPayLoad: E): CompletableDeferred<T> {
        if (isStartDestroy) error("is destroy")
        val response = CompletableDeferred<T>()
        val requestMsg = RequestMsg<T, E>(requestPayLoad)
        requestMsg.response = response
        sendChannel.send(requestMsg)
        return response
    }

    suspend fun <E, T> groupRequest(requestPayLoad: E, group: AwaitGroupRequest<Actor, T> = AwaitGroupRequest()): AwaitGroupRequest<Actor, T> {
        group.addRequestResult(this, request(requestPayLoad))
        return group
    }
}

sealed class ActorMsg
class AddActorMsg<T>(val actor: Actor) : ActorMsg() {
    var response: CompletableDeferred<AddActorResult> = CompletableDeferred()

    data class AddActorResult(
            val success: Boolean,
            val error: String?
    )
}

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
}

suspend fun <E, T> List<Actor>.groupRequest(requestPayLoad: E): Map<Actor, T> {
    val awaitGroupRequest = AwaitGroupRequest<Actor, T>()
    this.forEach {
        awaitGroupRequest.addRequestResult(it, it.request(requestPayLoad))
    }
    return awaitGroupRequest.getGroupResult()
}

class AwaitGroupRequest<M, T> {
    private val awaitResultMap: HashMap<M, CompletableDeferred<T>> = HashMap()
    fun addRequestResult(from: M, result: CompletableDeferred<T>) {
        awaitResultMap[from] = result
    }

    suspend fun getGroupResult(): Map<M, T> {
        val groupResult: HashMap<M, T> = HashMap()
        for ((from, requestResult) in awaitResultMap) {
            groupResult[from] = requestResult.await()
        }
        return groupResult
    }
}
