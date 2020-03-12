package com.tmtbe.frame.gameserver.framework.message

class FindActorMessageClass {
    val classList: ArrayList<Class<*>> = ArrayList()
    fun addActorMessageClass(className: Class<*>) {
        classList.add(className)
    }
}