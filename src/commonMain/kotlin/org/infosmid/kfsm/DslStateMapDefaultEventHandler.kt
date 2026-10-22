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
package org.infosmid.kfsm

/**
 * This handler will be active inside the default section of the statemachine.
 * @author Corneil du Plessis
 * @soundtrack Wolfgang Amadeus Mozart
 */
class DslStateMapDefaultEventHandler<S, E, C, A, R>(private val fsm: StateMapBuilder<S, E, C, A, R>) {
    /**
     * Define a default action that will be applied when no other transitions are matched.
     * @param action Will be invoked when no transitions match
     */
    fun action(action: DefaultStateAction<C, S, E, A, R>) {
        fsm.defaultAction(action)
    }

    /**
     * Defines an action to perform before a change in the currentState of the FSM
     * @param action This action will be performed when entering a new state.
     */
    fun onEntry(action: DefaultEntryExitAction<C, S, A>) {
        fsm.defaultEntry(action)
    }

    /**
     * Defines an action to be performed after the currentState was changed.
     * @param action The action will be performed when leaving any state.
     */
    fun onExit(action: DefaultEntryExitAction<C, S, A>) {
        fsm.defaultExit(action)
    }

    /**
     * Defines a default transition when an on is received to a specific state.
     * @param event Pair representing an on and targetState for transition. Can be written as EVENT to STATE
     * @param action The action will be performed before transition is completed
     */
    fun onEvent(
        event: EventState<E, S>,
        action: SyncStateAction<C, A, R>?
    ): DslStateMapDefaultEventHandler<S, E, C, A, R> {
        fsm.default(event, action)
        return this
    }

    /**
     * Defines a default internal transition for a specific event with no change in state.
     * @param event The event that triggers this transition
     * @param action The action will be invoked for this transition
     */
    fun onEvent(event: E, action: SyncStateAction<C, A, R>?): DslStateMapDefaultEventHandler<S, E, C, A, R> {
        fsm.default(event, action)
        return this
    }
}
