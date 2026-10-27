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
 * The build will be created to assist with create the top level or named state maps.
 * All transitions are assigned to a state map.
 * @author Corneil du Plessis
 * @soundtrack Wolfgang Amadeus Mozart
 */
class StateMapBuilder<S, E, C, A, R>(
    /**
     * The set of states the state map supports
     */
    val validStates: Set<S>,
    /**
     * The name of the statemap.
     */
    val name: String? = null,
    /**
     * The parent state machine builder
     */
    val parentBuilder: StateMachineBuilder<S, E, C, A, R>
) {
    private val transitionRules: MutableMap<Pair<S, E>, SyncTransitionRules<S, E, C, A, R>> = mutableMapOf()
    private val defaultTransitions: MutableMap<E, DefaultSyncTransition<S, E, C, A, R>> = mutableMapOf()
    private val entryActions: MutableMap<S, DefaultEntryExitAction<C, S, A>> = mutableMapOf()
    private val exitActions: MutableMap<S, DefaultEntryExitAction<C, S, A>> = mutableMapOf()
    private val defaultActions: MutableMap<S, DefaultStateAction<C, S, E, A, R>> = mutableMapOf()
    private val automaticTransitions: MutableMap<S, SyncTransitionRules<S, E, C, A, R>> = mutableMapOf()
    private var globalDefault: DefaultStateAction<C, S, E, A, R>? = null
    private var defaultEntryAction: DefaultEntryExitAction<C, S, A>? = null
    private var defaultExitAction: DefaultEntryExitAction<C, S, A>? = null
    private val invariants: MutableSet<Pair<String, Condition<C>>> = mutableSetOf()

    /**
     * This function defines an invariant condition that will hold true before and after all transitions.
     * @param message The message added to exception
     * @param condition A boolean expression applied to the context
     */
    fun invariant(message: String, condition: Condition<C>) {
        invariants.add(Pair(message, condition))
    }

    /**
     * This function defines a transition from the currentState equal to startState to the targetState when event is
     * received and the guard expression is met. The action is executed after any exit action and before entry actions.
     * @param startState The transition will be considered when currentState matches stateState
     * @param event The event will trigger the consideration of this transition
     * @param targetState The transition will change currentState to targetState after executing the option action
     * @param guard The guard expression will have to be met to consider the transition
     * @param action The optional action will be executed when the transition occurs.
     */
    fun transition(
        startState: S,
        event: E,
        targetState: S,
        guard: StateGuard<C, A>,
        action: SyncStateAction<C, A, R>?
    ) {
        precondition(parentBuilder.validEvents.contains(event)) { "$event must be one of ${parentBuilder.validEvents}" }
        precondition(validStates.contains(startState)) { "$startState must be one of $validStates" }
        val key = Pair(startState, event)
        val transitionRule = transitionRules[key]
        val transition = SyncGuardedTransition<S, E, C, A, R>(
            startState,
            event,
            targetState,
            null,
            false,
            TransitionType.NORMAL,
            guard,
            action
        )
        if (transitionRule == null) {
            val rule = SyncTransitionRules<S, E, C, A, R>()
            rule.addGuarded(transition)
            transitionRules[key] = rule
        } else {
            transitionRule.addGuarded(transition)
        }
    }

    /**
     * This function defines a transition that doesn't change the state also know as an internal transition.
     * The transition will only occur if the guard expression is met.
     * @param startState The transition will be considered when currentState matches stateState
     * @param event The event will trigger the consideration of this transition
     * @param guard The guard expression will have to be met to consider the transition
     * @param action The optional action will be executed when the transition occurs.
     */
    fun transition(startState: S, event: E, guard: StateGuard<C, A>, action: SyncStateAction<C, A, R>?) {
        precondition(parentBuilder.validEvents.contains(event)) { "$event must be one of ${parentBuilder.validEvents}" }
        precondition(validStates.contains(startState)) { "$startState must be one of $validStates" }
        val key = Pair(startState, event)
        val transitionRule = transitionRules[key]
        val transition = SyncGuardedTransition<S, E, C, A, R>(
            startState,
            event,
            null,
            null,
            false,
            TransitionType.NORMAL,
            guard,
            action
        )
        if (transitionRule == null) {
            val rule = SyncTransitionRules<S, E, C, A, R>()
            rule.addGuarded(transition)
            transitionRules[key] = rule
        } else {
            transitionRule.addGuarded(transition)
        }
    }

    /**
     * This function defines a transition that will be triggered when the currentState is the same as the startState and on is received. The FSM currentState will change to the targetState after the action was executed.
     * Entry and Exit actions will also be performed.
     * @param startState transition applies when FSM currentState is the same as stateState.
     * @param event transition applies when on received.
     * @param targetState FSM will transition to targetState.
     * @param action The actions will be invoked
     */
    fun transition(startState: S, event: E, targetState: S, action: SyncStateAction<C, A, R>?) {
        precondition(parentBuilder.validEvents.contains(event)) { "$event must be one of ${parentBuilder.validEvents}" }
        precondition(validStates.contains(startState)) { "$startState must be one of $validStates" }
        val key = Pair(startState, event)
        val transitionRule = transitionRules[key]
        val transition = SimpleSyncTransition<S, E, C, A, R>(
            startState,
            event,
            targetState,
            null,
            false,
            TransitionType.NORMAL,
            action
        )
        if (transitionRule == null) {
            transitionRules[key] = SyncTransitionRules<S, E, C, A, R>(transition = transition)
        } else {
            precondition(transitionRule.transition == null) { "Unguarded Transition for $startState on $event already defined" }
            transitionRule.transition = transition
        }
    }

    /**
     * This function defines a transition that doesn't change the state of the state machine when the currentState is startState and the on is received and after the action was performed. No entry or exit actions performed.
     * @param startState transition applies when when FSM currentState is the same as stateState
     * @param event transition applies when on received
     * @param action actions will be invoked
     */
    fun transition(startState: S, event: E, action: SyncStateAction<C, A, R>?) {
        precondition(parentBuilder.validEvents.contains(event)) { "$event must be one of ${parentBuilder.validEvents}" }
        precondition(validStates.contains(startState)) { "$startState must be one of $validStates" }
        val key = Pair(startState, event)
        val transitionRule = transitionRules[key]
        val transition = SimpleSyncTransition<S, E, C, A, R>(
            startState,
            event,
            null,
            null,
            false,
            TransitionType.NORMAL,
            action
        )
        if (transitionRule == null) {
            transitionRules[key] = SyncTransitionRules<S, E, C, A, R>(transition = transition)
        } else {
            precondition(transitionRule.transition == null) { "Unguarded Transition for $startState on $event already defined" }
            transitionRule.transition = transition
        }
    }

    /**
     * Creates a pop transition that applies to the startState on a given event. This will result in a new push transition
     * @param startState The pop transition will apply to the specific state.
     * @param event The event that will trigger the transition
     * @param targetState The state after the transition
     * @param targetMap The map after the transition
     * @param action The optional action that will be invoked.
     */
    fun popTransition(startState: S, event: E, targetState: S?, targetMap: String?, action: SyncStateAction<C, A, R>?) {
        precondition(parentBuilder.validEvents.contains(event)) { "$event must be one of ${parentBuilder.validEvents}" }
        val key = Pair(startState, event)
        val transitionRule = transitionRules[key]
        val transition = SimpleSyncTransition<S, E, C, A, R>(
            startState,
            event,
            targetState,
            targetMap,
            false,
            TransitionType.POP,
            action
        )
        if (transitionRule == null) {
            transitionRules[key] = SyncTransitionRules<S, E, C, A, R>(transition = transition)
        } else {
            precondition(transitionRule.transition == null) { "Unguarded Transition for $startState on $event already defined" }
            transitionRule.transition = transition
        }
    }

    /**
     * Creates a pop transition that applies to the startState on a given event. This will result in a new push transition if the guard expression is true
     * @param startState The pop transition will apply to the specific state.
     * @param event The event that will trigger the transition
     * @param targetState The state after the transition
     * @param targetMap The map after the transition
     * @param guard The expression must evaludate to true before the transition will be trigger.
     * @param action The optional action that will be invoked.
     */
    fun popTransition(
        startState: S,
        event: E,
        targetState: S?,
        targetMap: String?,
        guard: StateGuard<C, A>,
        action: SyncStateAction<C, A, R>?
    ) {
        precondition(parentBuilder.validEvents.contains(event)) { "$event must be one of ${parentBuilder.validEvents}" }
        precondition(validStates.contains(startState)) { "$startState must be one of $validStates" }
        val key = Pair(startState, event)
        val transitionRule = transitionRules[key]
        val transition = SyncGuardedTransition<S, E, C, A, R>(
            startState,
            event,
            targetState,
            targetMap,
            false,
            TransitionType.POP,
            guard,
            action
        )
        if (transitionRule == null) {
            val rule = SyncTransitionRules<S, E, C, A, R>()
            rule.addGuarded(transition)
            transitionRules[key] = rule
        } else {
            transitionRule.addGuarded(transition)
        }
    }

    /**
     * Creates a push transition for a startState and triggered by event. The transition will change to targetMap and targetState.
     * @param startState The transition will apply to the specific state.
     * @param event The event that will trigger the transition
     * @param targetState The state after the transition
     * @param targetMap The map after the transition
     * @param action The optional action will be invoked
     */
    fun pushTransition(startState: S, event: E, targetMap: String, targetState: S, action: SyncStateAction<C, A, R>?) {
        precondition(parentBuilder.validEvents.contains(event)) { "$event must be one of ${parentBuilder.validEvents}" }
        precondition(validStates.contains(startState)) { "$startState must be one of $validStates" }
        precondition(parentBuilder.namedStateMaps.containsKey(targetMap)) { "$targetMap map not found in ${parentBuilder.namedStateMaps.keys}" }
        val key = Pair(startState, event)
        val transitionRule = transitionRules[key]
        val transition = SimpleSyncTransition<S, E, C, A, R>(
            startState,
            event,
            targetState,
            targetMap,
            false,
            TransitionType.PUSH,
            action
        )
        if (transitionRule == null) {
            transitionRules[key] = SyncTransitionRules<S, E, C, A, R>(mutableListOf(), transition)
        } else {
            precondition(transitionRule.transition == null) { "Unguarded Transition for $startState on $event already defined" }
            transitionRule.transition = transition
        }
    }

    /**
     * Creates a push transition for a startState and triggered by event and guard expression being `true`. The transition will change to targetMap and targetState.
     * @param startState The transition will apply to the specific state.
     * @param event The event that will trigger the transition
     * @param targetState The state after the transition
     * @param targetMap The map after the transition
     * @param guard The expression must be true to trigger the transition
     * @param action The optional action will be invoked
     */
    fun pushTransition(
        startState: S,
        event: E,
        targetMap: String,
        targetState: S,
        guard: StateGuard<C, A>,
        action: SyncStateAction<C, A, R>?
    ) {
        precondition(parentBuilder.validEvents.contains(event)) { "$event must be one of ${parentBuilder.validEvents}" }
        precondition(validStates.contains(startState)) { "$startState must be one of $validStates" }
        precondition(parentBuilder.namedStateMaps.containsKey(targetMap)) { "$targetMap map not found in ${parentBuilder.namedStateMaps.keys}" }
        val key = Pair(startState, event)
        val transitionRule = transitionRules[key]
        val transition =
            SyncGuardedTransition<S, E, C, A, R>(
                startState,
                event,
                targetState,
                targetMap,
                false,
                TransitionType.PUSH,
                guard,
                action
            )
        if (transitionRule == null) {
            val rule = SyncTransitionRules<S, E, C, A, R>()
            rule.addGuarded(transition)
            transitionRules[key] = rule
        } else {
            transitionRule.addGuarded(transition)
        }
    }

    /**
     * Creates an automatic transition when currentState is the startState. The transition will change to targetState.
     * @param startState The transition will apply to the specific state.
     * @param targetState The state after the transition
     * @param action The optional action will be invoked
     */
    fun automatic(startState: S, targetState: S, action: SyncStateAction<C, A, R>?) {
        precondition(validStates.contains(targetState)) { "$targetState must be one of $validStates" }
        precondition(validStates.contains(startState)) { "$startState must be one of $validStates" }
        val transitionRule = automaticTransitions[startState]
        val transition = SimpleSyncTransition<S, E, C, A, R>(
                startState,
                null,
                targetState,
                null,
                true,
                TransitionType.NORMAL,
                action
        )
        if (transitionRule == null) {
            automaticTransitions[startState] = SyncTransitionRules<S, E, C, A, R>(mutableListOf(), transition)
        } else {
            precondition(transitionRule.transition == null) { "Unguarded Transition for $startState on already defined" }
            transitionRule.transition = transition
        }
    }

    /**
     * @param startState The transition will apply to the specific state.
     * @param targetState The state after the transition
     * @param guard The expression must be true to trigger the transition
     * @param action The optional action will be invoked
     */
    fun automatic(startState: S, targetState: S, guard: StateGuard<C, A>, action: SyncStateAction<C, A, R>?) {
        precondition(validStates.contains(targetState)) { "$targetState must be one of $validStates" }
        precondition(validStates.contains(startState)) { "$startState must be one of $validStates" }
        val transitionRule = automaticTransitions[startState]
        val transition =
            SyncGuardedTransition<S, E, C, A, R>(
                startState,
                null,
                targetState,
                null,
                true,
                TransitionType.NORMAL,
                guard,
                action
            )
        if (transitionRule == null) {
            automaticTransitions[startState] = SyncTransitionRules<S, E, C, A, R>(mutableListOf(transition))
        } else {
            transitionRule.addGuarded(transition)
        }
    }

    /**
     * Creates a pop transition that will pop the last statemap and then change to the targetState.
     * @param startState The transition will apply to the specific state.
     * @param targetState The state after the transition
     * @param action The optional action will be invoked
     */
    fun automaticPop(startState: S, targetState: S, action: SyncStateAction<C, A, R>?) {
        precondition(validStates.contains(startState)) { "$startState must be one of $validStates" }
        val transitionRule = automaticTransitions[startState]
        val transition = SimpleSyncTransition<S, E, C, A, R>(
            startState, null, targetState, null, true, TransitionType.POP, action
        )
        if (transitionRule == null) {
            automaticTransitions[startState] = SyncTransitionRules<S, E, C, A, R>(mutableListOf(), transition)
        } else {
            precondition(transitionRule.transition == null) { "Unguarded Transition for $startState on already defined" }
            transitionRule.transition = transition
        }
    }

    /**
     * Creates a pop transition that will return to orginal state that was pushed after executing the action.
     * @param startState The transition will apply to the specific state.
     * @param action The optional action will be invoked
     */
    fun automaticPop(startState: S, action: SyncStateAction<C, A, R>?) {
        precondition(validStates.contains(startState)) { "$startState must be one of $validStates" }
        val transitionRule = automaticTransitions[startState]
        val transition = SimpleSyncTransition<S, E, C, A, R>(
            startState, null, null, null, true, TransitionType.POP, action
        )
        if (transitionRule == null) {
            automaticTransitions[startState] = SyncTransitionRules<S, E, C, A, R>(transition = transition)
        } else {
            precondition(transitionRule.transition == null) { "Unguarded Transition for $startState on already defined" }
            transitionRule.transition = transition
        }
    }

    /**
     * Creates a pop transition that will apply when currentState is startState. The transition will change to targetMap/targetState
     * @param startState The transition will apply to the specific state.
     * @param targetState The state after the transition
     * @param targetMap The map after the transition
     * @param action The optional action will be invoked
     */
    fun automaticPop(startState: S, targetMap: String, targetState: S, action: SyncStateAction<C, A, R>?) {
        precondition(validStates.contains(startState)) { "$startState must be one of $validStates" }
        val transitionRule = automaticTransitions[startState]
        val transition =
            SimpleSyncTransition<S, E, C, A, R>(
                startState,
                null,
                targetState,
                targetMap,
                true,
                TransitionType.POP,
                action
            )
        if (transitionRule == null) {
            automaticTransitions[startState] = SyncTransitionRules<S, E, C, A, R>(transition = transition)
        } else {
            precondition(transitionRule.transition == null) { "Unguarded Transition for $startState on already defined" }
            transitionRule.transition = transition
        }
    }

    /**
     * Creates a pop transition that will apply when currentState is startState and guard is true. The transition will change to targetMap/targetState
     * @param startState The transition will apply to the specific state.
     * @param targetState The state after the transition
     * @param guard The expression must be true to trigger the transition
     * @param action The optional action will be invoked
     */
    fun automaticPop(startState: S, targetState: S, guard: StateGuard<C, A>, action: SyncStateAction<C, A, R>?) {
        precondition(validStates.contains(startState)) { "$startState must be one of $validStates" }
        val transitionRule = automaticTransitions[startState]
        val transition =
            SyncGuardedTransition<S, E, C, A, R>(
                startState,
                null,
                targetState,
                null,
                true,
                TransitionType.POP,
                guard,
                action
            )
        if (transitionRule == null) {
            automaticTransitions[startState] = SyncTransitionRules<S, E, C, A, R>(mutableListOf(transition))
        } else {
            transitionRule.addGuarded(transition)
        }
    }

    /**
     * Creates an automatic pop transition with will apply when currentState is startState and the guard expression is true.
     * @param startState The transition will apply to the specific state.
     * @param guard The expression must be true to trigger the transition
     * @param action The optional action will be invoked
     */
    fun automaticPop(startState: S, guard: StateGuard<C, A>, action: SyncStateAction<C, A, R>?) {
        precondition(validStates.contains(startState)) { "$startState must be one of $validStates" }
        val transitionRule = automaticTransitions[startState]
        val transition =
            SyncGuardedTransition<S, E, C, A, R>(startState, null, null, null, true, TransitionType.POP, guard, action)
        if (transitionRule == null) {
            automaticTransitions[startState] = SyncTransitionRules<S, E, C, A, R>(mutableListOf(transition))
        } else {
            transitionRule.addGuarded(transition)
        }
    }

    /**
     * Create an automatic pop transition when currentState is startState and when guard is true. The transition will target targetMap and targetState
     * @param startState The transition will apply to the specific state.
     * @param targetState The state after the transition
     * @param targetMap The map after the transition
     * @param guard The expression must be true to trigger the transition
     * @param action The optional action will be invoked
     */
    fun automaticPop(
        startState: S,
        targetMap: String,
        targetState: S,
        guard: StateGuard<C, A>,
        action: SyncStateAction<C, A, R>?
    ) {
        precondition(validStates.contains(startState)) { "$startState must be one of $validStates" }
        val transitionRule = automaticTransitions[startState]
        val transition = SyncGuardedTransition<S, E, C, A, R>(
                startState,
                null,
                targetState,
                targetMap,
                true,
                TransitionType.POP,
                guard,
                action
            )
        if (transitionRule == null) {
            automaticTransitions[startState] = SyncTransitionRules<S, E, C, A, R>(mutableListOf(transition))
        } else {
            transitionRule.addGuarded(transition)
        }
    }

    /**
     * Creates an automatic push transition when current state is startState for a targetMap and targetState.
     * @param startState The transition will apply to the specific state.
     * @param targetMap The map after the transition
     * @param targetState The state after the transition
     * @param action The optional action will be invoked
     */
    fun automaticPush(startState: S, targetMap: String, targetState: S, action: SyncStateAction<C, A, R>?) {
        precondition(validStates.contains(startState)) { "$startState must be one of $validStates" }
        val transitionRule = automaticTransitions[startState]
        val transition =
            SimpleSyncTransition<S, E, C, A, R>(
                startState,
                null,
                targetState,
                targetMap,
                true,
                TransitionType.PUSH,
                action
            )
        if (transitionRule == null) {
            automaticTransitions[startState] = SyncTransitionRules<S, E, C, A, R>(transition = transition)
        } else {
            precondition(transitionRule.transition == null) { "Unguarded Transition for $startState on already defined" }
            transitionRule.transition = transition
        }
    }

    /**
     * Creates an automatic push transition for startState. When the currentState is startState and guard is true the targetMap and targetState will become current after executing the optional action
     * @param startState The transition will apply to the specific state.
     * @param targetMap The map after the transition
     * @param targetState The state after the transition
     * @param guard The expression must be true to trigger the transition
     * @param action The optional action will be invoked
     */
    fun automaticPush(
        startState: S,
        targetMap: String,
        targetState: S,
        guard: StateGuard<C, A>,
        action: SyncStateAction<C, A, R>?
    ) {
        precondition(validStates.contains(startState)) { "$startState must be one of $validStates" }
        val transitionRule = automaticTransitions[startState]
        val transition = SyncGuardedTransition<S, E, C, A, R>(
                startState,
                null,
                targetState,
                targetMap,
                true,
                TransitionType.PUSH,
                guard,
                action
            )
        if (transitionRule == null) {
            automaticTransitions[startState] = SyncTransitionRules<S, E, C, A, R>(mutableListOf(transition))
        } else {
            transitionRule.addGuarded(transition)
        }
    }

    /**
     * This function defines an action to be invoked when no action is found matching the current state and event.
     * This will be an internal transition and will not cause a change in state or invoke entry or exit functions.
     * @param action This action will be performed
     */
    fun defaultAction(action: DefaultStateAction<C, S, E, A, R>) {
        globalDefault = action
    }

    /**
     * This function defines an action to be invoked when no entry action is defined for the current state.
     * @param action This action will be invoked
     */
    fun defaultEntry(action: DefaultEntryExitAction<C, S, A>) {
        defaultEntryAction = action
    }

    /**
     * This function defines an action to be invoked when no exit action is defined for the current state.
     * @param action This action will be invoked
     */
    fun defaultExit(action: DefaultEntryExitAction<C, S, A>) {
        defaultExitAction = action
    }

    /**
     * This function defines an action to be invoked when no transitions are found matching the given state and on.
     * @param currentState The provided state
     * @param action This action will be invoked
     */
    fun default(currentState: S, action: DefaultStateAction<C, S, E, A, R>) {
        precondition(validStates.contains(currentState)) { "$currentState must be one of $validStates" }
        precondition(defaultActions[currentState] == null) { "Default defaultAction already defined for $currentState" }
        defaultActions[currentState] = action
    }

    /**
     * This function defines an action to be invoked when no transitions match the event. The currentState will be change to second parameter of the Pair.
     * @param event The Pair holds the event and targetState and can be written as `event to state`
     * @param action The option action will be executed when this default transition occurs.
     */
    fun default(event: EventState<E, S>, action: SyncStateAction<C, A, R>?) {
        precondition(parentBuilder.validEvents.contains(event.first)) { "$event must be one of ${parentBuilder.validEvents}" }
        precondition(defaultTransitions[event.first] == null) { "Default transition for ${event.first} already defined" }
        defaultTransitions[event.first] = DefaultSyncTransition<S, E, C, A, R>(
            event.first,
            event.second,
            null,
            false,
            TransitionType.NORMAL,
            action
        )
    }

    /**
     * This function defines an action to be invoked when no transitions are found for given event.
     * @param event The event to match this transition.
     * @param action The option action will be executed when this default transition occurs.
     */
    fun default(event: E, action: SyncStateAction<C, A, R>?) {
        precondition(parentBuilder.validEvents.contains(event)) { "$event must be one of ${parentBuilder.validEvents}" }
        precondition(defaultTransitions[event] == null) { "Default transition for $event already defined" }
        defaultTransitions[event] = DefaultSyncTransition<S, E, C, A, R>(
            event,
            null,
            null,
            false,
            TransitionType.NORMAL,
            action
        )
    }

    /**
     * This function defines an action to be invoked when the FSM changes to the provided state
     * @param currentState The provided state
     * @param action This action will be invoked
     */
    fun entry(currentState: S, action: DefaultEntryExitAction<C, S, A>) {
        precondition(validStates.contains(currentState)) { "$currentState must be one of $validStates" }
        precondition(entryActions[currentState] == null) { "Entry defaultAction already defined for $currentState" }
        entryActions[currentState] = action
    }

    /**
     * This function defines an action to be invoke when the FSM changes from the provided state
     * @param currentState The provided state
     * @param action This action will be invoked
     */
    fun exit(currentState: S, action: DefaultEntryExitAction<C, S, A>) {
        precondition(validStates.contains(currentState)) { "$currentState must be one of $validStates" }
        precondition(exitActions[currentState] == null) { "Exit defaultAction already defined for $currentState" }
        exitActions[currentState] = action
    }

    /**
     * Creates a `StateMapDefinition` from the data in this builder
     */
    fun toMap(afterStateChangeAction: StateChangeAction<C, S>?): StateMapDefinition<S, E, C, A, R> = StateMapDefinition(
        this.name,
        this.validStates,
        this.invariants.toSet(),
        this.transitionRules.toMap(),
        this.defaultTransitions.toMap(),
        this.entryActions.toMap(),
        this.exitActions.toMap(),
        this.defaultActions.toMap(),
        this.automaticTransitions.toMap(),
        this.globalDefault,
        this.defaultEntryAction,
        this.defaultExitAction,
        afterStateChangeAction
    )
}
