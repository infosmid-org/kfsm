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

import org.infosmid.dbc.precondition

/**
 * An instance of a state machine.
 * The instance processes events. It finds the matching transition and runs the exit action, the transition action and the entry action.
 * @author Corneil du Plessis
 * @soundtrack Wolfgang Amadeus Mozart
 * @param context The context that the actions operate on.
 * @param definition The definition of the state machine. The definition has all the behaviour.
 * @param initialState The state at the start. When it is `null`, the definition derives the state from the context.
 * @param initialExternalState A state that was externalised earlier. Use it to restore the state map stack.
 */
class StateMachineInstance<S, E, C, A, R>(
    /**
     * The actions operate on the context.
     */
    private val context: C,
    /**
     * The immutable definition of the state machine.
     */
    val definition: StateMachineDefinition<S, E, C, A, R>,
    /**
     * The state at the start. The instance assigns it to `currentState`.
     */
    initialState: S? = null,
    initialExternalState: ExternalState<S>? = null,
) {
    internal val namedInstances: MutableMap<String, StateMapInstance<S, E, C, A, R>> = mutableMapOf()

    internal val mapStack: Stack<StateMapInstance<S, E, C, A, R>> = Stack()

    /**
     * The current state map. Transitions change it.
     */
    internal var currentStateMap: StateMapInstance<S, E, C, A, R>

    /**
     * The current state of the current state map.
     */
    val currentState: S
        get() = currentStateMap.currentState

    /**
     * Creates a state machine instance from a definition and a state that was externalised earlier.
     * @param context The context that the actions operate on.
     * @param definition The definition of the state machine.
     * @param initialExternalState The state that was externalised earlier.
     */
    constructor(context: C, definition: StateMachineDefinition<S, E, C, A, R>, initialExternalState: ExternalState<S>) :
        this(context, definition, null, initialExternalState)

    init {
        currentStateMap = definition.create(context, this, initialState, initialExternalState)
    }

    internal fun pushMap(
        defaultInstance: StateMapInstance<S, E, C, A, R>,
        initial: S,
        name: String,
        stateMap: StateMapDefinition<S, E, C, A, R>
    ): StateMapInstance<S, E, C, A, R> {
        mapStack.push(defaultInstance)
        val pushedMap = StateMapInstance(context, initial, name, this, stateMap)
        currentStateMap = pushedMap
        return pushedMap
    }

    internal fun pushMap(stateMap: StateMapInstance<S, E, C, A, R>): StateMapInstance<S, E, C, A, R> {
        mapStack.push(stateMap)
        return stateMap
    }

    internal fun execute(transition: SyncTransition<S, E, C, A, R>, arg: A?): R? {
        val result = if (transition.type == TransitionType.PUSH) {
            precondition(transition.targetState != null) { "A push transition requires a target state" }
            precondition(transition.targetMap != null) { "A push transition requires a target map" }
            if (transition.targetMap != currentStateMap.name) {
                executePush(transition, arg)
            } else {
                currentStateMap.execute(transition, arg)
            }
        } else if (transition.type == TransitionType.POP) {
            executePop(transition, arg)
        } else {
            currentStateMap.execute(transition, arg)
        }
        currentStateMap.executeAutomatic(transition, currentStateMap.currentState, arg)
        return result
    }

    private fun executePop(transition: SyncTransition<S, E, C, A, R>, arg: A?): R? {
        val sourceMap = currentStateMap
        val targetMap = mapStack.pop()
        currentStateMap = targetMap
        return if (transition.targetMap == null) {
            if (transition.targetState == null) {
                transition.execute(context, sourceMap, targetMap, arg)
            } else {
                val interim = transition.execute(context, sourceMap, null, arg)
                targetMap.executeEntry(context, transition.targetState, arg)
                currentStateMap.changeState(transition.targetState)
                interim
            }
        } else {
            val interim = executePush(transition, arg)
            if (transition.targetState != null) {
                currentStateMap.changeState(transition.targetState)
            }
            interim
        }
    }

    private fun executePush(transition: SyncTransition<S, E, C, A, R>, arg: A?): R? {
        val targetStateMap = namedInstances.getOrElse(transition.targetMap!!) {
            definition.createStateMap(transition.targetMap, context, this, transition.targetState!!).apply {
                namedInstances[transition.targetMap] = this
            }
        }
        mapStack.push(currentStateMap)
        val result = transition.execute(context, currentStateMap, targetStateMap, arg)
        currentStateMap = targetStateMap
        if (transition.targetState != null) {
            currentStateMap.changeState(transition.targetState)
        }
        return result
    }

    /**
     * Gets the events that are allowed in the current state.
     * An allowed event does not guarantee a transition, because a guard can prevent the transition.
     * Default actions are not included.
     * @param includeDefault When `true`, the result includes the events of default transitions.
     * @return The set of allowed events.
     */
    fun allowed(includeDefault: Boolean = false): Set<E> = currentStateMap.allowed(includeDefault)

    /**
     * Checks if an event is allowed in the current state.
     * @param event The event to check.
     * @param includeDefault When `true`, the check includes default transitions.
     * @return `true` if the event is allowed.
     */
    fun eventAllowed(event: E, includeDefault: Boolean): Boolean =
        currentStateMap.eventAllowed(event, includeDefault)

    /**
     * Sends an event to the current state map.
     * The state machine runs the matching transition as the definition describes.
     * @param event The event to process.
     * @param arg The argument of the event.
     * @return The result of the transition action, or `null` if the action returns no value.
     * @throws IllegalStateException if no transition and no default action match the current state and event.
     */
    fun sendEvent(event: E, arg: A? = null) = currentStateMap.sendEvent(event, arg)

    /**
     * Gets the state that you can store and use to create an instance later.
     * @return A list of pairs of a state and a state map name. The first pair is the bottom of the stack and the last pair is the current state map.
     * @see StateMachineDefinition.create
     */
    fun externalState(): ExternalState<S> {
        return mapStack.peekContent()
            .map { Pair(it.currentState, it.name ?: "default") }
            .toMutableList()
            .apply {
                add(Pair(currentStateMap.currentState, currentStateMap.name ?: "default"))
            }.toList()
    }
}

typealias AnyStateMachineInstance<S, E, C> = StateMachineInstance<S, E, C, Any, Any>
