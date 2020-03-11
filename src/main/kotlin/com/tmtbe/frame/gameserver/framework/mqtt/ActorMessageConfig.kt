package com.tmtbe.frame.gameserver.framework.mqtt

import com.tmtbe.frame.gameserver.framework.stereotype.GameActorMessage
import org.springframework.beans.factory.config.BeanDefinitionHolder
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory
import org.springframework.beans.factory.support.*
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner
import org.springframework.core.Ordered
import org.springframework.core.PriorityOrdered
import org.springframework.core.type.filter.TypeFilter
import org.springframework.stereotype.Component

@Component
class GameActorMessageRegistryPostProcessor : PriorityOrdered, BeanDefinitionRegistryPostProcessor {
    private var annotation: String = GameActorMessage::class.java.name
    private var basePackage: Array<String> = arrayOf("com.tmtbe.frame.gameserver")
    override fun getOrder(): Int {
        return Ordered.LOWEST_PRECEDENCE
    }

    private fun getBeanDefinitionHolderSet(vararg basePackages: String): Set<BeanDefinitionHolder> {
        val registry: BeanDefinitionRegistry = SimpleBeanDefinitionRegistry()
        val scanner = CustomerClassPathBeanDefinitionScanner(registry, false)
        val annotationType = annotation
        //配置过滤条件
        val includeFilter = TypeFilter { arg0, _ ->
            var result = false
            if (arg0.classMetadata.isConcrete && arg0.annotationMetadata.hasAnnotation(annotationType)) {
                result = true
            }
            result
        }
        scanner.addIncludeFilter(includeFilter)
        return scanner.doScan(*basePackages)
    }

    override fun postProcessBeanFactory(beanFactory: ConfigurableListableBeanFactory) {
        val dlbf = beanFactory as DefaultListableBeanFactory
        val beans = getBeanDefinitionHolderSet(*basePackage)
        beanFactory.registerAlias("findActorMessageClass", FindActorMessageClass::class.java.toString())
        val rbd = RootBeanDefinition(FindActorMessageClass::class.java)
        dlbf.registerBeanDefinition("findActorMessageClass", rbd)
        val findActorMessageClass = beanFactory.getBean("findActorMessageClass") as FindActorMessageClass
        for (bdh in beans) {
            val cls = Class.forName(bdh.beanDefinition.beanClassName)
            if (cls.annotations != null && cls.annotations.isNotEmpty()) {
                for (annotation in cls.annotations) {
                    if (annotation is GameActorMessage) {
                        findActorMessageClass.addActorMessageClass(cls)
                    }
                }
            }
        }
    }

    override fun postProcessBeanDefinitionRegistry(p0: BeanDefinitionRegistry) {

    }

    private fun registerAlias(factory: ConfigurableListableBeanFactory, beanId: String, value: String) {
        //防止别名覆盖bean的ID
        if (factory.containsBeanDefinition(value)) {
            return
        } else {
            factory.registerAlias(beanId, value)
        }
    }
}

class CustomerClassPathBeanDefinitionScanner(registry: BeanDefinitionRegistry?, useDefaultFilters: Boolean) : ClassPathBeanDefinitionScanner(registry!!, useDefaultFilters) {
    public override fun doScan(vararg basePackages: String): Set<BeanDefinitionHolder> {
        return super.doScan(*basePackages)
    }
}

class FindActorMessageClass {
    val classList: ArrayList<Class<*>> = ArrayList()
    fun addActorMessageClass(className: Class<*>) {
        classList.add(className)
    }
}