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
 * The DSL receiver at the top level of a state machine declaration.
 * @author Corneil du Plessis
 * @soundtrack Wolfgang Amadeus Mozart
 */
class DslStateMachineHandler<S, E, C, A, R>(private val fsm: StateMachineBuilder<S, E, C, A, R>) {
    /**
     * Declares an expression that derives the initial state from the context.
     * @param deriveInitialState A lambda that has the context as `this` and returns the state.
     */
    fun initialState(deriveInitialState: StateQuery<C, S>): DslStateMachineHandler<S, E, C, A, R> {
        fsm.initialState(deriveInitialState)
        return this
    }

    /**
     * The initial state to use when no `initialState` expression or external state is available.
     */
    var defaultInitialState: S?
        get() = fsm.defaultInitialState
        set(state) {
            fsm.defaultInitialState = state
        }

    /**
     * Declares an expression that derives the state map stack from the context.
     * The expression returns a list of pairs of a state and a state map name.
     * The first pair is the bottom of the stack. The last pair is the current state map.
     * Use it when the state machine has named state maps.
     *
     * ```
     * initialStates {
     *     mutableListOf<StateMapItem<PayingTurnstileStates>>().apply {
     *         if (locked) {
     *             add(PayingTurnstileStates.LOCKED to "default")
     *         } else {
     *             add(PayingTurnstileStates.UNLOCKED to "default")
     *         }
     *         if (coins > 0) {
     *             add(PayingTurnstileStates.COINS to "coins")
     *         }
     *     }
     * }
     * ```
     *
     * @param deriveInitialMap A lambda that has the context as `this` and returns the list.
     */
    fun initialStates(deriveInitialMap: StateMapQuery<C, S>): DslStateMachineHandler<S, E, C, A, R> {
        fsm.initialStates(deriveInitialMap)
        return this
    }

    /**
     * Declares an invariant. The state machine checks it when the state machine is created and when the state changes.
     * @param message The message of the `InvariantException` that the state machine throws when the condition is `false`.
     * @param condition A lambda that has the context as `this` and returns `true` when the invariant holds.
     */
    fun invariant(message: String, condition: Condition<C>): DslStateMachineHandler<S, E, C, A, R> {
        fsm.invariant(message, condition)
        return this
    }

    /**
     * Declares an action that the state machine runs after the state has changed.
     * The state machine does not run the action when the state stays the same.
     * The state machine catches an exception from the action and prints a message. It does not rethrow the exception.
     * @param action A lambda that receives the previous state and the new state.
     */
    fun onStateChange(action: StateChangeAction<C, S>) {
        fsm.afterStateChange(action)
    }

    /**
     * Declares the transitions and actions for one state of the default state map.
     * @param currentState The state that the block describes.
     * @param handler The lambda that declares the transitions and actions.
     */
    fun whenState(currentState: S, handler: DslStateMapEventHandler<S, E, C, A, R>.() -> Unit):
        DslStateMapEventHandler<S, E, C, A, R> =
        DslStateMapEventHandler(currentState, fsm.defaultStateMap).apply(handler)

    /**
     * Declares the default behaviour of the default state map.
     * @param handler The lambda that declares the default actions.
     */
    fun default(handler: DslStateMapDefaultEventHandler<S, E, C, A, R>.() -> Unit):
        DslStateMapDefaultEventHandler<S, E, C, A, R> =
        DslStateMapDefaultEventHandler(fsm.defaultStateMap).apply(handler)

    /**
     * Completes the declaration and creates the definition.
     * @return The immutable state machine definition.
     */
    fun build() = fsm.complete()

    /**
     * Declares a named state map.
     * A push transition can make the state map current.
     */
    fun stateMap(
        /**
         * The name of the state map. The name must be unique and must not be empty.
         */
        name: String,
        /**
         * The set of states that the state map allows.
         */
        validStates: Set<S>,
        /**
         * The lambda that declares the state map.
         */
        handler: DslStateMapHandler<S, E, C, A, R>.() -> Unit
    ): DslStateMapHandler<S, E, C, A, R> {
        return DslStateMapHandler(fsm.stateMap(name.trim(), validStates)).apply(handler)
    }
}
