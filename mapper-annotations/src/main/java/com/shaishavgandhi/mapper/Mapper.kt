package com.shaishavgandhi.mapper

import kotlin.annotation.AnnotationRetention.SOURCE
import kotlin.annotation.AnnotationTarget.CLASS
import kotlin.annotation.AnnotationTarget.TYPE
import kotlin.reflect.KClass

@Target(TYPE, CLASS)
@Retention(SOURCE)
annotation class Mapper(
  val to: KClass<*> = DefaultMapper::class
)