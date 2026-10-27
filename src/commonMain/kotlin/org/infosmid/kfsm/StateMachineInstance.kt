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
 * This class represents an instance of a state machine.
 * It will process events, matching transitions, invoking entry, transition and exit actions.
 * @author Corneil du Plessis
 * @soundtrack Wolfgang Amadeus Mozart
 * @param context The events may trigger actions on the context of class C
 * @param definition The defined state machine that provides all the behaviour
 * @param initialState The initial state of the instance.
 */
class StateMachineInstance<S, E, C, A, R>(
    /**
     * The transition actions are performed by manipulating the context.
     */
    private val context: C,
    /**
     * The Immutable definition of the state machine.
     */
    val definition: StateMachineDefinition<S, E, C, A, R>,
    /**
     * The initialState will be assigned to the currentState
     */
    initialState: S? = null,
    initialExternalState: ExternalState<S>? = null,
) {
    internal val namedInstances: MutableMap<String, StateMapInstance<S, E, C, A, R>> = mutableMapOf()

    internal val mapStack: Stack<StateMapInstance<S, E, C, A, R>> = Stack()

    /**
     * This represents the current state of the state machine.
     * It will be modified during transitions
     */
    internal var currentStateMap: StateMapInstance<S, E, C, A, R>

    val currentState: S
        get() = currentStateMap.currentState

    /**
     * Create a state machine using a specific definition and a previous externalised state.
     * @param context The instane will operate on the context
     * @param definition The definition of the state machine instance.
     * @param initialExternalState The previously externalised state.
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
            precondition(transition.targetState != null) { "Target state cannot be null for pushTransition" }
            precondition(transition.targetMap != null) { "Target map cannot be null for pushTransition" }
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
     * This function will provide the set of allowed events given a specific state. It isn't a guarantee that a
     * subsequent transition will be successful since a guard may prevent a transition. Default state handlers are not considered.
     * @param includeDefault When `true` will include default transitions in the list of allowed events.
     */
    fun allowed(includeDefault: Boolean = false): Set<E> = currentStateMap.allowed(includeDefault)

    /**
     * This function will return an indicator if the given event is allowed given the current state.
     * @param event The given event.
     * @param includeDefault Will consider defined default event handlers.
     */
    fun eventAllowed(event: E, includeDefault: Boolean): Boolean =
        currentStateMap.eventAllowed(event, includeDefault)

    /**
     * This function will process the on and advance the state machine according to the FSM definition.
     * @param event The on received,
     */
    fun sendEvent(event: E, arg: A? = null) = currentStateMap.sendEvent(event, arg)

    /**
     * This function will provide the external state that can be use when creating the instance at a later time.
     * @return The external state a collection of state and map name pairs.
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
