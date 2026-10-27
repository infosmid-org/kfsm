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

import org.infosmid.kfsm.AsyncStateChangeAction
import org.infosmid.kfsm.Condition
import org.infosmid.kfsm.DefaultAsyncStateAction
import org.infosmid.kfsm.DefaultEntryExitAction

/**
 * The definition of a state map for an asynchronous state machine.
 * A state map is a group of states with their transitions and actions.
 * A state machine has one top-level state map and can have named state maps.
 */
class AsyncStateMapDefinition<S, E, C, A, R>(
    /**
     * The name of the state map. The name of the top-level state map is `null`.
     */
    val name: String?,
    /**
     * The set of states that this state map allows.
     */
    val validStates: Set<S>,
    /**
     * The invariants. The state machine checks them before and after every transition.
     * It throws an `InvariantException` when a condition is `false`.
     */
    val invariants: Set<Pair<String, Condition<C>>>,
    /**
     * The transition rules, by pair of state and event.
     * Most transitions are in this map.
     */
    val transitionRules: Map<Pair<S, E>, AsyncTransitionRules<S, E, C, A, R>>,
    /**
     * The default transitions, by event. The state machine uses a default transition when no transition matches the state and the event.
     */
    val defaultTransitions: Map<E, DefaultAsyncTransition<S, E, C, A, R>>,
    /**
     * The entry actions, by state. The state machine runs the entry action when it enters the state.
     */
    val entryActions: Map<S, DefaultEntryExitAction<C, S, A>>,
    /**
     * The exit actions, by state. The state machine runs the exit action when it leaves the state.
     */
    val exitActions: Map<S, DefaultEntryExitAction<C, S, A>>,
    /**
     * The default actions, by state. The state machine runs the default action when no transition matches the state and the event.
     */
    val defaultActions: Map<S, DefaultAsyncStateAction<C, S, E, A, R>>,
    /**
     * The transition rules for automatic transitions, by state.
     */
    val automaticTransitions: Map<S, AsyncTransitionRules<S, E, C, A, R>>,
    /**
     * The timer definitions, by state. The state machine starts the timers when it enters the state and cancels them when it leaves the state.
     */
    val timerDefinitions: Map<S, AsyncTimerDefinition<S, E, C, A, R>>,
    /**
     * The default action for the state map. The state machine runs it when no other action matches.
     */
    val globalDefault: DefaultAsyncStateAction<C, S, E, A, R>?,
    /**
     * The default entry action. The state machine runs it when it enters any state, after the entry action of the state.
     */
    val defaultEntryAction: DefaultEntryExitAction<C, S, A>?,
    /**
     * The default exit action. The state machine runs it when it leaves any state, after the exit action of the state.
     */
    val defaultExitAction: DefaultEntryExitAction<C, S, A>?,
    /**
     * The action that the state machine runs after the state has changed.
     * The state machine catches an exception from the action and does not rethrow it.
     */
    val afterStateChangeAction: AsyncStateChangeAction<C, S>?
) {
    /**
     * Gets the events that are allowed in the current state.
     * An allowed event does not guarantee a transition, because a guard can prevent the transition.
     * Default actions are not included.
     * @param includeDefault When `true`, the result includes the events of default transitions.
     * @return The set of allowed events.
     */
    fun allowed(given: S, includeDefault: Boolean = false): Set<E> {
        val result = transitionRules.entries.filter {
            it.key.first == given
        }.map {
            it.key.second
        }.toSet()
        if (includeDefault && defaultTransitions.isNotEmpty()) {
            return result + defaultTransitions.keys
        }
        return result
    }

    /**
     * Checks if an event is allowed in a state.
     * The result is `false` when the state has no transition for the event.
     * The exception is when `includeDefault` is `true` and the state has a default action or a default transition for the event.
     * @param event The event to check.
     * @param given The state to check.
     * @param includeDefault When `true`, the check includes default actions and default transitions.
     */
    fun eventAllowed(event: E, given: S, includeDefault: Boolean): Boolean =
        (includeDefault &&
            hasDefaultStateHandler(given)) ||
            allowed(given, includeDefault).contains(event)

    /**
     * Checks if the state has a default action.
     */
    private fun hasDefaultStateHandler(given: S) = defaultActions.contains(given)
}
