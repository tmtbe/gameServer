package com.tmtbe.frame.gameserver.game.demoB

import com.tmtbe.frame.gameserver.framework.actor.RoomActor
import com.tmtbe.frame.gameserver.framework.scene.Scene
import com.tmtbe.frame.gameserver.framework.annotation.GameScene
import com.tmtbe.frame.gameserver.framework.scene.SceneConfiguration

@GameScene
class DemoBScene : Scene(
        "demoB",
        DemoBRoomActor::class.java,
        DemoBPlayerActor::class.java,
        SceneConfiguration(4)
) {
    override suspend fun onRoomCreate(roomActor: RoomActor) {
        log.info("${roomActor.name}:创建了")
    }

    override suspend fun onRoomDestroy(roomActor: RoomActor) {
        log.info("${roomActor.name}:销毁了")
    }
}