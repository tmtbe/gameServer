package com.tmtbe.frame.gameserver.base.utils

import org.slf4j.LoggerFactory
import org.springframework.beans.BeansException
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import org.springframework.stereotype.Component

@Component
class SpringUtils : ApplicationContextAware {
    @Throws(BeansException::class)
    override fun setApplicationContext(applicationContext: ApplicationContext) {
        Companion.applicationContext = applicationContext
    }

    companion object {
        private var applicationContext: ApplicationContext? = null
        fun <T> getBean(tClass: Class<T>): T {
            return applicationContext!!.getBean(tClass)
        }

        fun <T> getBean(name: String?, type: Class<T>): T {
            return applicationContext!!.getBean(name!!, type)
        }

        fun getActiveProfile(): String? {
            return applicationContext!!.environment.activeProfiles[0]
        }
    }
}

fun <T : Any> T.getProxy(): T {
    return SpringUtils.getBean(this.javaClass)
}

fun Any.log() = LoggerFactory.getLogger(this.javaClass)