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
 * @suppress
 * An instance of a state map. It holds the current state and runs the transitions of the state map.
 * @author Corneil du Plessis
 * @soundtrack Wolfgang Amadeus Mozart
 */
class StateMapInstance<S, E, C, A, R>(
    val context: C,
    val newState: S,
    val name: String?,
    val parentFsm: StateMachineInstance<S, E, C, A, R>,
    val definition: StateMapDefinition<S, E, C, A, R>
) {
    init {
        precondition(definition.validStates.contains(newState)) { "Initial state $newState is not in the valid states of this state map: ${definition.validStates}" }
        definition.invariants.forEach { checkInvariant(context, it) }
    }

    var currentState: S = newState
        internal set

    internal fun changeState(targetState: S) {
        definition.invariants.forEach { checkInvariant(context, it) }
        if (currentState != targetState) {
            val oldState = currentState
            currentState = targetState
            try {
                definition.afterStateChangeAction?.invoke(context, oldState, currentState)
            } catch (x: Throwable) {
                println("changeState:ignoring:exception:$x")
            }
            definition.invariants.forEach { checkInvariant(context, it) }
        }
    }

    internal fun execute(transition: SyncTransition<S, E, C, A, R>, arg: A?): R? {
        val result = transition.execute(context, this, arg)
        if (transition.isExternal()) {
            changeState(transition.targetState!!)
        }
        return result
    }

    internal fun executeEntry(context: C, targetState: S, arg: A?) {
        val entryAction =
            definition.entryActions[targetState] ?: parentFsm.definition.defaultStateMap.entryActions[targetState]
        entryAction?.invoke(context, currentState, targetState, arg)
        val defaultEntry = definition.defaultEntryAction ?: parentFsm.definition.defaultStateMap.defaultEntryAction
        defaultEntry?.invoke(context, currentState, targetState, arg)
    }

    internal fun executeExit(context: C, targetState: S, arg: A?) {
        val exitAction =
            definition.exitActions[currentState] ?: parentFsm.definition.defaultStateMap.exitActions[targetState]
        exitAction?.invoke(context, currentState, targetState, arg)
        val defaultExitAction = definition.defaultExitAction ?: parentFsm.definition.defaultStateMap.defaultExitAction
        defaultExitAction?.invoke(context, currentState, targetState, arg)
    }

    private fun executeDefaultAction(event: E, arg: A?): R? {
        return with(
            definition.defaultActions[currentState]
                ?: definition.globalDefault
                ?: parentFsm.definition.defaultStateMap.defaultActions[currentState]
                ?: parentFsm.definition.defaultStateMap.globalDefault
                ?: error("No transition and no default action for state $currentState and event $event")
        ) {
            invoke(context, currentState, event, arg)
        }
    }

    /**
     * Processes an event in the current state of this state map.
     * When the state map has transition rules for the current state and the event, the order of selection is:
     * 1. The first guarded transition where the guard returns `true`.
     * 2. The transition without a guard.
     * 3. The default action.
     *
     * When the state map has no transition rules for the current state and the event, it uses the default transition for the event.
     * If there is no default transition, it uses the default action.
     * @param event The event to process.
     * @param arg The argument of the event.
     * @return The result of the transition action or the default action.
     * @throws IllegalStateException if no transition and no default action match.
     */
    fun sendEvent(event: E, arg: A? = null): R? {
        val stateEvent = Pair(currentState, event)
        return definition.transitionRules[stateEvent]?.let { rule ->
            rule.findGuard(context, arg)?.let {
                parentFsm.execute(it, arg)
            } ?: run {
                rule.transition?.let {
                    parentFsm.execute(it, arg)
                } ?: executeDefaultAction(event, arg)
            }
        } ?: definition.defaultTransitions[event]?.let {
            parentFsm.execute(it, arg)
        } ?: executeDefaultAction(event, arg)
    }

    /**
     * Gets the events that are allowed in the current state.
     * @param includeDefaults When `true`, the result includes the events of default transitions.
     */
    fun allowed(includeDefaults: Boolean = false) = definition.allowed(currentState, includeDefaults)

    /**
     * Checks if an event is allowed in the current state.
     * @param event The event to check.
     * @param includeDefault When `true`, the check includes default transitions.
     */
    fun eventAllowed(event: E, includeDefault: Boolean): Boolean =
        definition.eventAllowed(event, currentState, includeDefault)

    internal fun executeAutomatic(currentTransition: SyncTransition<S, E, C, A, R>, state: S, arg: A?): Boolean {
        var result = false
        definition.automaticTransitions[state]?.apply rule@{
            val defaultTransition = this.transition
            findGuard(context, arg)?.apply {
                if (currentTransition !== this) {
                    parentFsm.execute(this, arg)
                    result = true
                }
            } ?: run {
                if (defaultTransition != null) {
                    if (currentTransition !== defaultTransition) {
                        parentFsm.execute(defaultTransition, arg)
                        result = true
                    }
                }
            }
        }
        return result
    }
}
