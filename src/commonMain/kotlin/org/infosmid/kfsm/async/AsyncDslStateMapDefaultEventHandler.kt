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

import org.infosmid.kfsm.AsyncStateAction
import org.infosmid.kfsm.DefaultAsyncStateAction
import org.infosmid.kfsm.DefaultEntryExitAction
import org.infosmid.kfsm.EventState

/**
 * The DSL receiver inside a `default` block of an asynchronous state machine.
 */
class AsyncDslStateMapDefaultEventHandler<S, E, C, A, R>(private val fsm: AsyncStateMapBuilder<S, E, C, A, R>) {
    /**
     * Declares the default action. The state machine runs it when no other transition matches.
     * @param action The `suspend` lambda to run when no transition matches.
     */
    fun action(action: DefaultAsyncStateAction<C, S, E, A, R>) {
        fsm.defaultAction(action)
    }

    /**
     * Declares the default entry action.
     * The state machine runs it for an external transition, after the entry action of the target state.
     * The state has not changed when the action runs.
     * @param action The lambda to run when the state machine enters a state.
     */
    fun onEntry(action: DefaultEntryExitAction<C, S, A>) {
        fsm.defaultEntry(action)
    }

    /**
     * Declares the default exit action.
     * The state machine runs it for an external transition, after the exit action of the current state.
     * The state has not changed when the action runs.
     * @param action The lambda to run when the state machine leaves a state.
     */
    fun onExit(action: DefaultEntryExitAction<C, S, A>) {
        fsm.defaultExit(action)
    }

    /**
     * Declares a default external transition for an event. It applies to every state that has no transition for the event.
     * @param event A pair of the event and the target state. Write it as `EVENT to STATE`.
     * @param action The `suspend` lambda to run during the transition.
     */
    fun onEvent(
        event: EventState<E, S>,
        action: AsyncStateAction<C, A, R>?
    ): AsyncDslStateMapDefaultEventHandler<S, E, C, A, R> {
        fsm.default(event, action)
        return this
    }

    /**
     * Declares a default internal transition for an event. The state does not change.
     * It applies to every state that has no transition for the event.
     * @param event The event that triggers the transition.
     * @param action The `suspend` lambda to run during the transition.
     */
    fun onEvent(event: E, action: AsyncStateAction<C, A, R>?): AsyncDslStateMapDefaultEventHandler<S, E, C, A, R> {
        fsm.default(event, action)
        return this
    }
}
