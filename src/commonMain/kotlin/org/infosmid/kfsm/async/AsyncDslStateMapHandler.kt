/*
 * Copyright 2019-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.infosmid.kfsm.async

/**
 * The DSL receiver inside a named `stateMap` block of an asynchronous state machine.
 */
class AsyncDslStateMapHandler<S, E, C, A, R>(private val fsm: AsyncStateMapBuilder<S, E, C, A, R>) {

    /**
     * Declares the transitions and actions for one state.
     * @param currentState The state that the block describes.
     * @param handler The lambda that declares the transitions and actions.
     */
    fun whenState(currentState: S, handler: AsyncDslStateMapEventHandler<S, E, C, A, R>.() -> Unit):
        AsyncDslStateMapEventHandler<S, E, C, A, R> =
        AsyncDslStateMapEventHandler(currentState, fsm).apply(handler)

    /**
     * Declares the default behaviour of the state map.
     * @param handler The lambda that declares the default actions.
     */
    fun default(handler: AsyncDslStateMapDefaultEventHandler<S, E, C, A, R>.() -> Unit):
        AsyncDslStateMapDefaultEventHandler<S, E, C, A, R> =
        AsyncDslStateMapDefaultEventHandler(fsm).apply(handler)
}
