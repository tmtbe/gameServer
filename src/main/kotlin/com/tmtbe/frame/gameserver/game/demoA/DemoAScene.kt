package com.tmtbe.frame.gameserver.game.demoA

import com.tmtbe.frame.gameserver.framework.actor.RoomActor
import com.tmtbe.frame.gameserver.framework.annotation.GameScene
import com.tmtbe.frame.gameserver.framework.scene.Scene
import com.tmtbe.frame.gameserver.framework.scene.SceneConfiguration

@GameScene
class DemoAScene : Scene(
        "demoA",
        DemoARoomActor::class.java,
        DemoAPlayerActor::class.java,
        SceneConfiguration(2)
) {
    override suspend fun onRoomCreate(roomActor: RoomActor) {
        log.info("${roomActor.name}:创建了")
    }

    override suspend fun onRoomDestroy(roomActor: RoomActor) {
        log.info("${roomActor.name}:销毁了")
    }
}