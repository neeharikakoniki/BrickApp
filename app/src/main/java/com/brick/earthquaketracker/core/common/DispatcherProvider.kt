package com.brick.earthquaketracker.core.common

import javax.inject.Qualifier

/**
 * Qualifier annotations for injected dispatchers.
 * Using injected dispatchers (instead of hardcoded Dispatchers.IO) makes every
 * coroutine-based class testable with StandardTestDispatcher and virtual time.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DefaultDispatcher
