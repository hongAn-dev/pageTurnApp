package com.pageturn.core.common.network

import javax.inject.Qualifier

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class Dispatcher(val ptDispatcher: PtDispatchers)

enum class PtDispatchers {
    Default,
    IO,
    Main
}
