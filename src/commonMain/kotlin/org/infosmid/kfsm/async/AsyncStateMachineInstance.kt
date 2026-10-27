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

import kotlinx.coroutines.CoroutineScope
import org.infosmid.dbc.preconditionNotNull
import org.infosmid.kfsm.ExternalState
import org.infosmid.kfsm.Stack
import org.infosmid.kfsm.TransitionType

/**
 * An instance of an asynchronous state machine.
 * The instance processes events. It finds the matching transition and runs the exit action, the transition action and the entry action.
 * The instance uses a `CoroutineScope` to run timeout transitions.
 * @param context The context that the actions operate on.
 * @param definition The definition of the state machine. The definition has all the behaviour.
 * @param coroutineScope The scope that runs the timeout transitions.
 * @param initialState The state at the start. When it is `null`, the definition derives the state from the context.
 * @param initialExternalState A state that was externalised earlier. Use it to restore the state map stack.
 */
class AsyncStateMachineInstance<S, E, C, A, R>(
    /**
     * The actions operate on the context.
     */
    private val context: C,
    /**
     * The immutable definition of the state machine.
     */
    val definition: AsyncStateMachineDefinition<S, E, C, A, R>,
    /**
     * The scope that runs the timeout transitions.
     */
    val coroutineScope: CoroutineScope,
    initialState: S? = null,
    initialExternalState: ExternalState<S>? = null,
) {
    internal val namedInstances: MutableMap<String, AsyncStateMapInstance<S, E, C, A, R>> = mutableMapOf()

    internal val mapStack: Stack<AsyncStateMapInstance<S, E, C, A, R>> =
        Stack()

    /**
     * The current state map. Transitions change it.
     */
    internal var currentStateMap: AsyncStateMapInstance<S, E, C, A, R>

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
    constructor(
        context: C,
        definition: AsyncStateMachineDefinition<S, E, C, A, R>,
        initialExternalState: ExternalState<S>,
        coroutineScope: CoroutineScope
    ) :
        this(context, definition, coroutineScope, null, initialExternalState)

    init {
        currentStateMap = definition.create(context, this, coroutineScope, initialState, initialExternalState)
    }

    internal fun pushMap(
        defaultInstance: AsyncStateMapInstance<S, E, C, A, R>,
        initial: S,
        name: String,
        stateMap: AsyncStateMapDefinition<S, E, C, A, R>
    ): AsyncStateMapInstance<S, E, C, A, R> {
        mapStack.push(defaultInstance)
        val pushedMap = AsyncStateMapInstance(context, initial, name, this, stateMap, coroutineScope)
        currentStateMap = pushedMap
        return pushedMap
    }

    internal fun pushMap(stateMap: AsyncStateMapInstance<S, E, C, A, R>): AsyncStateMapInstance<S, E, C, A, R> {
        mapStack.push(stateMap)
        return stateMap
    }

    internal suspend fun execute(transition: AsyncTransition<S, E, C, A, R>, arg: A?): R? {
        val result = if (transition.type == TransitionType.PUSH) {
            preconditionNotNull(transition.targetState) { "A push transition requires a target state" }
            preconditionNotNull(transition.targetMap) { "A push transition requires a target map" }
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

    private suspend fun executePop(transition: AsyncTransition<S, E, C, A, R>, arg: A?): R? {
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

    private suspend fun executePush(transition: AsyncTransition<S, E, C, A, R>, arg: A?): R? {
        val targetStateMap = namedInstances.getOrElse(transition.targetMap!!) {
            definition.createStateMap(transition.targetMap, context, this, transition.targetState!!, coroutineScope)
                .apply {
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
    suspend fun sendEvent(event: E, arg: A? = null) = currentStateMap.sendEvent(event, arg)

    /**
     * Gets the state that you can store and use to create an instance later.
     * @return A list of pairs of a state and a state map name. The first pair is the bottom of the stack and the last pair is the current state map.
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

typealias AsyncAnyStateMachineInstance<S, E, C> = AsyncStateMachineInstance<S, E, C, Any, Any>
