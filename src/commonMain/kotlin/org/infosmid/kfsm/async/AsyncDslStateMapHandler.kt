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
 * This handler will be active inside the top level of the stateMachine definition.
 */
class AsyncDslStateMapHandler<S, E, C, A, R>(private val fsm: AsyncStateMapBuilder<S, E, C, A, R>) {

    /**
     * Defines a section for a specific state.
     * @param currentState The give state
     * @param handler A lambda with definitions for the given state
     */
    fun whenState(currentState: S, handler: AsyncDslStateMapEventHandler<S, E, C, A, R>.() -> Unit):
        AsyncDslStateMapEventHandler<S, E, C, A, R> =
        AsyncDslStateMapEventHandler(currentState, fsm).apply(handler)

    /**
     * Defines a section for default behaviour for the state machine.
     * @param handler A lambda with definition for the default behaviour of the state machine.
     */
    fun default(handler: AsyncDslStateMapDefaultEventHandler<S, E, C, A, R>.() -> Unit):
        AsyncDslStateMapDefaultEventHandler<S, E, C, A, R> =
        AsyncDslStateMapDefaultEventHandler(fsm).apply(handler)
}
