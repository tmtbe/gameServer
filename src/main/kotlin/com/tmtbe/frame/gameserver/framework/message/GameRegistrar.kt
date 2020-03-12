package com.tmtbe.frame.gameserver.framework.message

import com.tmtbe.frame.gameserver.framework.annotation.GameApplication
import com.tmtbe.frame.gameserver.framework.annotation.GameActorMessage
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition
import org.springframework.beans.factory.support.BeanDefinitionRegistry
import org.springframework.beans.factory.support.DefaultListableBeanFactory
import org.springframework.beans.factory.support.RootBeanDefinition
import org.springframework.context.EnvironmentAware
import org.springframework.context.ResourceLoaderAware
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar
import org.springframework.core.env.Environment
import org.springframework.core.io.ResourceLoader
import org.springframework.core.type.AnnotationMetadata
import org.springframework.core.type.filter.AnnotationTypeFilter
import org.springframework.util.Assert
import org.springframework.util.ClassUtils
import org.springframework.util.StringUtils


internal open class GameRegistrar : ImportBeanDefinitionRegistrar, ResourceLoaderAware, EnvironmentAware {
    private var resourceLoader: ResourceLoader? = null
    private var environment: Environment? = null
    override fun setResourceLoader(resourceLoader: ResourceLoader) {
        this.resourceLoader = resourceLoader
    }

    override fun setEnvironment(environment: Environment) {
        this.environment = environment
    }

    override fun registerBeanDefinitions(metadata: AnnotationMetadata,
                                         registry: BeanDefinitionRegistry) {
        registerGameActorMessage(metadata, registry)
    }

    private fun registerGameActorMessage(metadata: AnnotationMetadata,
                                         registry: BeanDefinitionRegistry) {
        val scanner = scanner
        scanner.setResourceLoader(resourceLoader)
        val annotationTypeFilter = AnnotationTypeFilter(
                GameActorMessage::class.java)
        scanner.addIncludeFilter(annotationTypeFilter)
        val basePackages: MutableSet<String> = getBasePackages(metadata)
        for (basePackage in basePackages) {
            val candidateComponents = scanner
                    .findCandidateComponents(basePackage)
            for (candidateComponent in candidateComponents) {
                if (candidateComponent is AnnotatedBeanDefinition) {
                    // verify annotated class is an interface
                    val annotationMetadata = candidateComponent.metadata
                    Assert.isTrue(annotationMetadata.isConcrete,
                            "@GameActorMessage can only be specified on an concrete")
                    val attributes = annotationMetadata
                            .getAnnotationAttributes(
                                    GameActorMessage::class.java.canonicalName)
                    val defaultBeanFactory = registry as DefaultListableBeanFactory
                    defaultBeanFactory.registerAlias("findActorMessageClass", FindActorMessageClass::class.java.toString())
                    val rbd = RootBeanDefinition(FindActorMessageClass::class.java)
                    defaultBeanFactory.registerBeanDefinition("findActorMessageClass", rbd)
                    val findActorMessageClass = defaultBeanFactory.getBean("findActorMessageClass") as FindActorMessageClass
                    val cls = Class.forName(candidateComponent.beanClassName)
                    if (cls.annotations != null && cls.annotations.isNotEmpty()) {
                        for (annotation in cls.annotations) {
                            if (annotation is GameActorMessage) {
                                findActorMessageClass.addActorMessageClass(cls)
                                break
                            }
                        }
                    }
                }
            }
        }
    }

    private val scanner: ClassPathScanningCandidateComponentProvider
        get() = object : ClassPathScanningCandidateComponentProvider(false, environment!!) {
            override fun isCandidateComponent(beanDefinition: AnnotatedBeanDefinition): Boolean {
                var isCandidate = false
                if (beanDefinition.metadata.isIndependent) {
                    if (!beanDefinition.metadata.isAnnotation) {
                        isCandidate = true
                    }
                }
                return isCandidate
            }
        }

    private fun getBasePackages(importingClassMetadata: AnnotationMetadata): MutableSet<String> {
        val attributes = importingClassMetadata
                .getAnnotationAttributes(GameApplication::class.java.canonicalName)
        val basePackages: MutableSet<String> = HashSet()
        for (pkg in (attributes!!["value"] as Array<String>?)!!) {
            if (StringUtils.hasText(pkg)) {
                basePackages.add(pkg)
            }
        }
        for (pkg in (attributes["basePackages"] as Array<String>?)!!) {
            if (StringUtils.hasText(pkg)) {
                basePackages.add(pkg)
            }
        }
        for (clazz in (attributes["basePackageClasses"] as Array<Class<*>?>?)!!) {
            basePackages.add(ClassUtils.getPackageName(clazz!!))
        }
        if (basePackages.isEmpty()) {
            basePackages.add(
                    ClassUtils.getPackageName(importingClassMetadata.className))
        }
        return basePackages
    }
}
