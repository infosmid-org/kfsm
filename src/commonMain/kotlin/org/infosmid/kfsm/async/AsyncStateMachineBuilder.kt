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
@file:Suppress("RemoveExplicitTypeArguments")

package org.infosmid.kfsm.async

import org.infosmid.dbc.precondition
import org.infosmid.kfsm.AsyncStateAction
import org.infosmid.kfsm.AsyncStateChangeAction
import org.infosmid.kfsm.Condition
import org.infosmid.kfsm.DefaultAsyncStateAction
import org.infosmid.kfsm.DefaultEntryExitAction
import org.infosmid.kfsm.EventState
import org.infosmid.kfsm.StateGuard
import org.infosmid.kfsm.StateMapQuery
import org.infosmid.kfsm.StateQuery

/**
 * The builder of an asynchronous state machine definition.
 * It is the `suspend` version of [org.infosmid.kfsm.StateMachineBuilder]. The actions are `suspend` functions and the state maps can have timeout transitions.
 * The DSL records the declaration in this builder. Then `complete()` creates the immutable definition.
 * @param S The state type. It is often an enum.
 * @param E The event type. It is often an enum.
 * @param C The context type. The actions operate on the context.
 * @param A The type of the event argument.
 * @param R The type of the return value of the actions.
 * @param validMapStates The set of states that the default state map allows.
 * @param validEvents The set of events of the state machine.
 */
class AsyncStateMachineBuilder<S, E, C, A, R>(validMapStates: Set<S>, internal val validEvents: Set<E>) {
    private var completed = false
    var defaultInitialState: S? = null
    private var deriveInitialState: StateQuery<C, S>? = null
    private var deriveInitialStateMap: StateMapQuery<C, S>? = null
    internal val defaultStateMap =
        AsyncStateMapBuilder<S, E, C, A, R>(validMapStates, null, this)
    internal val namedStateMaps: MutableMap<String, AsyncStateMapBuilder<S, E, C, A, R>> = mutableMapOf()
    internal var afterStateChangeAction: AsyncStateChangeAction<C, S>? = null

    /**
     * Sets the expression that derives the initial state from the context.
     * @param init A function that has the context as `this` and returns the state.
     * @throws org.infosmid.dbc.PreConditionException if the builder is complete.
     */
    fun initialState(init: StateQuery<C, S>) {
        precondition(!completed) { "The state machine definition is complete. You cannot change it." }
        deriveInitialState = init
    }

    /**
     * Sets the expression that derives the state map stack from the context.
     * Use it when the state machine has named state maps.
     * The stack is a list of pairs of a state and a state map name.
     * @param init A function that has the context as `this` and returns the list. The state machine pushes the items on the stack in list order. The last item is the current state map.
     * @throws org.infosmid.dbc.PreConditionException if the builder is complete.
     */
    fun initialStates(init: StateMapQuery<C, S>) {
        precondition(!completed) { "The state machine definition is complete. You cannot change it." }
        deriveInitialStateMap = init
    }

    /**
     * Declares an invariant. The condition must be `true` before and after every transition.
     * @param message The message of the `InvariantException` that the state machine throws when the condition is `false`.
     * @param condition A boolean expression that has the context as `this`.
     */
    fun invariant(message: String, condition: Condition<C>) {
        defaultStateMap.invariant(message, condition)
    }

    /**
     * Declares a named state map.
     * @param name The name of the state map. The name must be unique, must not be empty and must not be `default`.
     * @param validStates The set of states that the state map allows. The set must not be empty.
     * @return The builder of the state map.
     */
    fun stateMap(
        name: String,
        validStates: Set<S>
    ): AsyncStateMapBuilder<S, E, C, A, R> {
        precondition(name.trim().isNotEmpty()) { "The name of a state map must not be empty" }
        precondition(name != "default") { "The name 'default' is reserved for the top-level state map" }
        precondition(validStates.isNotEmpty()) { "A state map must have at least one valid state" }
        val stateMapBuilder =
            AsyncStateMapBuilder<S, E, C, A, R>(validStates, name, this)
        namedStateMaps[name] = stateMapBuilder
        return stateMapBuilder
    }

    /**
     * Completes the declaration and creates the definition.
     * After this call, the builder does not accept changes.
     * @return The immutable state machine definition.
     * @throws org.infosmid.dbc.PreConditionException if the state machine has named state maps and an `initialState` expression.
     */
    fun complete(): AsyncStateMachineDefinition<S, E, C, A, R> {
        completed = true
        if (namedStateMaps.isNotEmpty()) {
            precondition(this.deriveInitialState == null) { "Use initialStates, not initialState, when the state machine has named state maps" }
        }
        return AsyncStateMachineDefinition(
            this.defaultInitialState,
            this.deriveInitialState,
            this.deriveInitialStateMap,
            this.defaultStateMap.toMap(this.afterStateChangeAction),
            this.namedStateMaps.map { Pair(it.key, it.value.toMap(this.afterStateChangeAction)) }.toMap()
        )
    }

    /**
     * Declares the state machine with the DSL.
     * @param handler The lambda that declares the state machine.
     * @return The DSL handler.
     */
    inline fun stateMachine(handler: AsyncDslStateMachineHandler<S, E, C, A, R>.() -> Unit): AsyncDslStateMachineHandler<S, E, C, A, R> =
        AsyncDslStateMachineHandler(this).apply(handler)

    /**
     * Sets the action that the state machine runs after the state has changed.
     * The state machine runs it after the exit action, the transition action and the entry action.
     * @param action A `suspend` lambda that receives the previous state and the new state.
     */
    fun afterStateChange(action: AsyncStateChangeAction<C, S>) {
        this.afterStateChangeAction = action
    }

    /**
     * Declares an external transition with a guard.
     * The transition occurs when the current state is `startState`, the state machine receives the event and the guard returns `true`.
     * The state machine runs the action after the exit action and before the entry action.
     * @param startState The state where the transition starts.
     * @param event The event that triggers the transition.
     * @param targetState The state after the transition.
     * @param guard The guard expression. It must return `true` to use the transition.
     * @param action An optional lambda to run during the transition.
     */
    fun transition(
        startState: S,
        event: E,
        targetState: S,
        guard: StateGuard<C, A>,
        action: AsyncStateAction<C, A, R>?
    ) =
        defaultStateMap.transition(startState, event, targetState, guard, action)

    /**
     * Declares an internal transition with a guard. The state does not change.
     * The transition occurs only when the guard returns `true`.
     * @param startState The state where the transition starts.
     * @param event The event that triggers the transition.
     * @param guard The guard expression. It must return `true` to use the transition.
     * @param action An optional lambda to run during the transition.
     */
    fun transition(startState: S, event: E, guard: StateGuard<C, A>, action: AsyncStateAction<C, A, R>?) =
        defaultStateMap.transition(startState, event, null, guard, action)

    /**
     * Declares an external transition.
     * The transition occurs when the current state is `startState` and the state machine receives the event.
     * The state machine runs the exit action, the transition action and the entry action. Then the state changes to the target state.
     * @param startState The state where the transition starts.
     * @param event The event that triggers the transition.
     * @param targetState The state after the transition.
     * @param action An optional lambda to run during the transition.
     */
    fun transition(startState: S, event: E, targetState: S, action: AsyncStateAction<C, A, R>?) =
        defaultStateMap.transition(startState, event, targetState, null, action)

    /**
     * Declares an internal transition. The state does not change.
     * The state machine does not run the exit action or the entry action.
     * @param startState The state where the transition starts.
     * @param event The event that triggers the transition.
     * @param action An optional lambda to run during the transition.
     */
    fun transition(startState: S, event: E, action: AsyncStateAction<C, A, R>?) =
        defaultStateMap.transition(startState, event, null, null, action)

    /**
     * Declares the default action for the state map.
     * The state machine runs it when no transition matches the current state and the event.
     * It is an internal transition. The state does not change, and the state machine does not run the exit action or the entry action.
     * @param action The lambda to run.
     */
    fun defaultAction(action: DefaultAsyncStateAction<C, S, E, A, R>) =
        defaultStateMap.defaultAction(action)

    /**
     * Declares the default entry action.
     * The state machine runs it for an external transition, after the entry action of the target state.
     * @param action The lambda to run.
     */
    fun defaultEntry(action: DefaultEntryExitAction<C, S, A>) =
        defaultStateMap.defaultEntry(action)

    /**
     * Declares the default exit action.
     * The state machine runs it for an external transition, after the exit action of the current state.
     * @param action The lambda to run.
     */
    fun defaultExit(action: DefaultEntryExitAction<C, S, A>) =
        defaultStateMap.defaultExit(action)

    /**
     * Declares the default action for one state.
     * The state machine runs it when no transition matches the state and the event.
     * @param currentState The state that the action applies to.
     * @param action The lambda to run.
     */
    fun default(currentState: S, action: DefaultAsyncStateAction<C, S, E, A, R>) =
        defaultStateMap.default(currentState, action)

    /**
     * Declares a default external transition for an event.
     * The state machine uses it when no transition matches the event. The state changes to the target state.
     * @param event A pair of the event and the target state. Write it as `event to state`.
     * @param action An optional lambda to run during the transition.
     */
    fun default(event: EventState<E, S>, action: AsyncStateAction<C, A, R>?) =
        defaultStateMap.default(event, action)

    /**
     * Declares a default internal transition for an event.
     * The state machine uses it when no transition matches the event. The state does not change.
     * @param event The event that triggers the transition.
     * @param action An optional lambda to run during the transition.
     */
    fun default(event: E, action: AsyncStateAction<C, A, R>?) =
        defaultStateMap.default(event, action)

    /**
     * Declares the entry action for a state.
     * The state machine runs it for an external transition that has this state as the target state.
     * @param currentState The state that the action applies to.
     * @param action The lambda to run.
     */
    fun entry(currentState: S, action: DefaultEntryExitAction<C, S, A>) =
        defaultStateMap.entry(currentState, action)

    /**
     * Declares the exit action for a state.
     * The state machine runs it for an external transition that starts in this state.
     * @param currentState The state that the action applies to.
     * @param action The lambda to run.
     */
    fun exit(currentState: S, action: DefaultEntryExitAction<C, S, A>) =
        defaultStateMap.exit(currentState, action)
}

typealias AsyncAnyStateMachineBuilder<S, E, C> = AsyncStateMachineBuilder<S, E, C, Any, Any>
