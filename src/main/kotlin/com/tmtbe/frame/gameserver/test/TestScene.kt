package com.tmtbe.frame.gameserver.test

import com.tmtbe.frame.gameserver.base.ResourceManager
import com.tmtbe.frame.gameserver.base.RoomActor
import com.tmtbe.frame.gameserver.base.Scene
import com.tmtbe.frame.gameserver.base.utils.log
import kotlinx.coroutines.InternalCoroutinesApi

@InternalCoroutinesApi
class TestScene(resourceManager: ResourceManager) : Scene(
        "test",
        TestRoomActor::class.java,
        TestPlayerActor::class.java,
        resourceManager
) {
    val log = log()
    override suspend fun onRoomCreate(roomActor: RoomActor) {
        log.info("${roomActor.name}:创建了")
    }

    override suspend fun onRoomDestroy(roomActor: RoomActor) {
        log.info("${roomActor.name}:销毁了")
    }
}