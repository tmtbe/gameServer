package com.tmtbe.frame.gameserver.test

import com.tmtbe.frame.gameserver.base.actor.RoomActor
import com.tmtbe.frame.gameserver.base.scene.Scene
import org.springframework.stereotype.Component

@Component
class TestScene : Scene(
        "test",
        TestRoomActor::class.java,
        TestPlayerActor::class.java
) {
    override suspend fun onRoomCreate(roomActor: RoomActor) {
        log.info("${roomActor.name}:创建了")
    }

    override suspend fun onRoomDestroy(roomActor: RoomActor) {
        log.info("${roomActor.name}:销毁了")
    }
}