package com.tmtbe.frame.gameserver.base.mqtt

import org.springframework.stereotype.Component

@Component
class TopicTemplate {
    fun createTopic(
            topicChannel: TopicChannel,
            scene: String,
            server: String
    ): String {
        return "${topicChannel.getType()}/${topicChannel.getName()}/$scene/$server"
    }

    fun parseTopic(topic: String): TopicParse {
        val split = topic.split("/")
        if (split.size < 4) {
            serverError("topic格式不正确:$topic")
        } else {
            val (topicChannelType, topicChannelName, scene, server) = split
            return when (TopicChannelType.valueOf(topicChannelType)) {
                TopicChannelType.REQUEST -> {
                    TopicParse(RequestChannel(topicChannelName), scene, server)
                }
                TopicChannelType.RESPONSE -> {
                    TopicParse(ResponseChannel(topicChannelName), scene, server)
                }
                TopicChannelType.ROOM -> {
                    TopicParse(RoomChannel(topicChannelName), scene, server)
                }
            }
        }
    }

    data class TopicParse(
            var topicChannel: TopicChannel,
            val scene: String,
            val server: String
    ) {
        fun toTopicString(): String {
            return "${this.topicChannel.getType().name}/${this.topicChannel.getName()}/$scene/$server"
        }
    }


    class RequestChannel(
            private val userName: String
    ) : TopicChannel {
        override fun getType() = TopicChannelType.REQUEST
        override fun getName() = userName
    }

    class ResponseChannel(
            private val userName: String
    ) : TopicChannel {
        override fun getType() = TopicChannelType.RESPONSE
        override fun getName() = userName
    }

    class RoomChannel(
            private val roomName: String
    ) : TopicChannel {
        override fun getType() = TopicChannelType.ROOM
        override fun getName() = roomName
    }

    interface TopicChannel {
        fun getType(): TopicChannelType
        fun getName(): String
    }

    enum class TopicChannelType {
        REQUEST, RESPONSE, ROOM
    }

}