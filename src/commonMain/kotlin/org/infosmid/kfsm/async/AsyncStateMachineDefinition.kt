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

import org.infosmid.kfsm.ExternalState
import org.infosmid.kfsm.StateMapQuery
import org.infosmid.kfsm.StateQuery
import kotlinx.coroutines.CoroutineScope

/**
 * The immutable definition of an asynchronous state machine.
 * You can share one definition. Create an instance of the state machine for each context.
 */
class AsyncStateMachineDefinition<S, E, C, A, R>(
    val defaultInitialState: S?,
    private val deriveInitialState: StateQuery<C, S>?,
    private val deriveInitialMap: StateMapQuery<C, S>?,
    /**
     * The top-level state map. Each instance creates it when the instance is created.
     */
    val defaultStateMap: AsyncStateMapDefinition<S, E, C, A, R>,
    /**
     * The named state maps, by name. A push transition makes a named state map current.
     */
    val namedStateMaps: Map<String, AsyncStateMapDefinition<S, E, C, A, R>>

) {
    private fun createMap(
        mapName: String,
        context: C,
        parentFsm: AsyncStateMachineInstance<S, E, C, A, R>,
        initial: S,
        coroutineScope: CoroutineScope
    ) {
        if (mapName == "default") {
            parentFsm.pushMap(
                AsyncStateMapInstance(
                    context,
                    initial,
                    null,
                    parentFsm,
                    defaultStateMap,
                    coroutineScope
                )
            )
        } else {
            val stateMap = namedStateMaps[mapName] ?: error("State map $mapName does not exist")
            parentFsm.pushMap(
                AsyncStateMapInstance(
                    context,
                    initial,
                    mapName,
                    parentFsm,
                    stateMap,
                    coroutineScope
                )
            )
        }
    }

    /**
     * Creates the current state map of an instance.
     * The order of precedence for the initial state is:
     * 1. The external state.
     * 2. The `initialStates` expression.
     * 3. The `initialState` argument.
     * 4. The `initialState` expression.
     * 5. The default initial state.
     * @param context The context that the actions operate on.
     * @param initialState The initial state. When it is `null`, the definition derives the state.
     * @throws IllegalStateException if the definition cannot find an initial state.
     */
    internal fun create(
        context: C,
        parentFsm: AsyncStateMachineInstance<S, E, C, A, R>,
        coroutineScope: CoroutineScope,
        initialState: S? = null,
        intitialExternalState: ExternalState<S>? = null
    ): AsyncStateMapInstance<S, E, C, A, R> {
        when {
            intitialExternalState != null -> {
                intitialExternalState.forEach { (initial, mapName) ->
                    createMap(mapName, context, parentFsm, initial, coroutineScope)
                }
                return parentFsm.mapStack.pop()
            }

            deriveInitialMap != null -> {
                deriveInitialMap.invoke(context).forEach { (initial, mapName) ->
                    createMap(mapName, context, parentFsm, initial, coroutineScope)
                }
                return parentFsm.mapStack.pop()
            }

            else -> {
                val initial = initialState ?: deriveInitialState?.invoke(context) ?: defaultInitialState
                ?: error("The state machine has no initial state. Give an initial state to create(), or define initialState, initialStates or defaultInitialState")
                return AsyncStateMapInstance(
                    context,
                    initial,
                    null,
                    parentFsm,
                    defaultStateMap,
                    coroutineScope
                )
            }
        }
    }

    internal fun createStateMap(
        name: String,
        context: C,
        parentFsm: AsyncStateMachineInstance<S, E, C, A, R>,
        initialState: S,
        coroutineScope: CoroutineScope
    ): AsyncStateMapInstance<S, E, C, A, R> =
        AsyncStateMapInstance(
            context,
            initialState,
            name,
            parentFsm,
            namedStateMaps[name] ?: error("State map $name does not exist"),
            coroutineScope
        )

    /**
     * Creates a state machine instance with a state that was externalised earlier.
     * @param context The context that the actions operate on.
     * @param initialExternalState The state that was externalised earlier. See [AsyncStateMachineInstance.externalState].
     * @return The new instance.
     */
    fun create(
        context: C,
        coroutineScope: CoroutineScope,
        initialExternalState: ExternalState<S>,
    ): AsyncStateMachineInstance<S, E, C, A, R> =
        AsyncStateMachineInstance<S, E, C, A, R>(
            context,
            this,
            coroutineScope,
            null,
            initialExternalState
        )

    /**
     * Creates a state machine instance.
     * @param context The context that the actions operate on.
     * @param initial The initial state. When it is `null`, the definition derives the state from the context.
     * @return The new instance.
     */
    fun create(
        context: C,
        coroutineScope: CoroutineScope,
        initial: S? = null
    ): AsyncStateMachineInstance<S, E, C, A, R> =
        AsyncStateMachineInstance<S, E, C, A, R>(context, this, coroutineScope, initial)

    /**
     * Gets the events that are possible for a state in all the state maps.
     * An event can still fail, because a guard can prevent the transition.
     * The result also depends on which state map is current, because each state map has its own transitions.
     * @param state The state to check.
     * @param includeDefault When `true`, the result includes the events of default transitions.
     * @return The set of possible events.
     */
    fun possibleEvents(state: S, includeDefault: Boolean): Set<E> {
        val result = mutableSetOf<E>()
        result.addAll(defaultStateMap.allowed(state, includeDefault))
        namedStateMaps.values.forEach {
            if (it.validStates.contains(state)) {
                result.addAll(it.allowed(state, includeDefault))
            }
        }
        return result.toSet()
    }
}
