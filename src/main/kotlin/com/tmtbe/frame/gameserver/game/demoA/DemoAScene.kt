package com.tmtbe.frame.gameserver.game.demoA

import com.tmtbe.frame.gameserver.framework.actor.RoomActor
import com.tmtbe.frame.gameserver.framework.scene.Scene
import com.tmtbe.frame.gameserver.framework.annotation.GameScene

@GameScene
class DemoAScene : Scene(
        "demoA",
        DemoARoomActor::class.java,
        TestPlayerActor::class.java
) {
    override suspend fun onRoomCreate(roomActor: RoomActor) {
        log.info("${roomActor.name}:创建了")
    }

    override suspend fun onRoomDestroy(roomActor: RoomActor) {
        log.info("${roomActor.name}:销毁了")
    }
}