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
 * Contains the definition of a state map. A state machine has at least one top-level state map.
 * @author Corneil du Plessis
 * @soundtrack Wolfgang Amadeus Mozart
 */
class StateMapDefinition<S, E, C, A, R>(
    /**
     * The name of the statemap. The top-level state map name is `null`
     */
    val name: String?,
    /**
     * A set of the valid states for this map.
     */
    val validStates: Set<S>,
    /**
     * Invariant conditions are checked before and after every transition an will throw an InvariantException if false
     */
    val invariants: Set<Pair<String, Condition<C>>>,
    /**
     * transitionRule contains a map of TransitionRules that is keyed by a Pair of state,event
     * This will be the most common transition rule.
     */
    val transitionRules: Map<Pair<S, E>, SyncTransitionRules<S, E, C, A, R>>,
    /**
     * The default transitions will be used if no transition of found matching a given event
     */
    val defaultTransitions: Map<E, DefaultSyncTransition<S, E, C, A, R>>,
    /**
     * This is a map of actions keyed by the state. A specific action will be invoked when a state is entered.
     */
    val entryActions: Map<S, DefaultEntryExitAction<C, S, A>>,
    /**
     * This is a map of actions keyed by the state. A specific action will be invoked when a state is exited.
     */
    val exitActions: Map<S, DefaultEntryExitAction<C, S, A>>,
    /**
     * This is a map of default actions for event on specific startState.
     */
    val defaultActions: Map<S, DefaultStateAction<C, S, E, A, R>>,
    /**
     * This a map of TransitionRules by state for automatic transitions.
     */
    val automaticTransitions: Map<S, SyncTransitionRules<S, E, C, A, R>>,
    /**
     * This is the action that will be invoked of no other has been matched
     */
    val globalDefault: DefaultStateAction<C, S, E, A, R>?,
    /**
     * This is the default action that will be invoked when entering any state when no other action has been matched.
     */
    val defaultEntryAction: DefaultEntryExitAction<C, S, A>?,
    /**
     * This is the default action that will be invoked when exiting any state when no other action has been matched.
     */
    val defaultExitAction: DefaultEntryExitAction<C, S, A>?,
    /**
     * This action will be invoked after a change in the state of the statemachine.
     * This machine will catch and ignore any exceptions thrown by the handler.
     */
    val afterStateChangeAction: StateChangeAction<C, S>?
) {
    /**
     * This function will provide the set of allowed events given a specific state. It isn't a guarantee that a
     * subsequent transition will be successful since a guard may prevent a transition. Default state handlers are not considered.
     * @param given The specific state to consider
     * @param includeDefault When `true` will include default transitions in the list of allowed events.
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
     * This function will provide an indicator if an event is allow for a given state.
     * When no state transition is declared this function will return false unless `includeDefault` is true and
     * there is a default transition of handler for the event.
     */
    fun eventAllowed(event: E, given: S, includeDefault: Boolean): Boolean =
        (includeDefault && hasDefaultStateHandler(given)) || allowed(given, includeDefault).contains(event)

    /**
     * This function will provide an indicator if a default action has been defined for a given state.
     */
    private fun hasDefaultStateHandler(given: S) = defaultActions.contains(given)
}
