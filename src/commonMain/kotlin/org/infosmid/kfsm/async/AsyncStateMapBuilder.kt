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

import org.infosmid.dbc.precondition
import org.infosmid.kfsm.AsyncStateAction
import org.infosmid.kfsm.AsyncStateChangeAction
import org.infosmid.kfsm.Condition
import org.infosmid.kfsm.DefaultAsyncStateAction
import org.infosmid.kfsm.DefaultEntryExitAction
import org.infosmid.kfsm.EventState
import org.infosmid.kfsm.StateGuard
import org.infosmid.kfsm.TransitionType

/**
 * The builder of one state map for an asynchronous state machine.
 * It is the `suspend` version of [org.infosmid.kfsm.StateMapBuilder]. It also records timeout transitions.
 * The builder records all the transitions and actions of the state map. `toMap()` creates the immutable definition.
 * @param validStates The set of states that the state map allows.
 * @param name The name of the state map. The name of the top-level state map is `null`.
 * @param parentBuilder The builder of the state machine that owns this state map.
 */
class AsyncStateMapBuilder<S, E, C, A, R>(
    /**
     * The set of states that the state map allows.
     */
    val validStates: Set<S>,
    /**
     * The name of the state map. The name of the top-level state map is `null`.
     */
    val name: String? = null,
    /**
     * The builder of the state machine that owns this state map.
     */
    val parentBuilder: AsyncStateMachineBuilder<S, E, C, A, R>
) {
    private val transitionRules: MutableMap<Pair<S, E>, AsyncTransitionRules<S, E, C, A, R>> = mutableMapOf()
    private val defaultTransitions: MutableMap<E, DefaultAsyncTransition<S, E, C, A, R>> = mutableMapOf()
    private val entryActions: MutableMap<S, DefaultEntryExitAction<C, S, A>> = mutableMapOf()
    private val exitActions: MutableMap<S, DefaultEntryExitAction<C, S, A>> = mutableMapOf()
    private val defaultActions: MutableMap<S, DefaultAsyncStateAction<C, S, E, A, R>> = mutableMapOf()
    private val automaticTransitions: MutableMap<S, AsyncTransitionRules<S, E, C, A, R>> = mutableMapOf()
    private var globalDefault: DefaultAsyncStateAction<C, S, E, A, R>? = null
    private var defaultEntryAction: DefaultEntryExitAction<C, S, A>? = null
    private var defaultExitAction: DefaultEntryExitAction<C, S, A>? = null
    private val timerActions: MutableMap<S, AsyncTimerDefinition<S, E, C, A, R>> = mutableMapOf()
    private val invariants: MutableSet<Pair<String, Condition<C>>> = mutableSetOf()

    /**
     * Declares an invariant. The condition must be `true` before and after every transition.
     * @param message The message of the `InvariantException` that the state machine throws when the condition is `false`.
     * @param condition A boolean expression that has the context as `this`.
     */
    fun invariant(message: String, condition: Condition<C>) {
        invariants.add(Pair(message, condition))
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
        targetState: S?,
        guard: StateGuard<C, A>?,
        action: AsyncStateAction<C, A, R>?
    ) {
        precondition(parentBuilder.validEvents.contains(event)) { "Event $event is not in the valid events of the state machine: ${parentBuilder.validEvents}" }
        precondition(validStates.contains(startState)) { "State $startState is not in the valid states of this state map: $validStates" }
        val key = Pair(startState, event)
        val transitionRule = transitionRules[key]
        if (guard != null) {
            val transition = AsyncGuardedTransition<S, E, C, A, R>(
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
                val rule = AsyncTransitionRules<S, E, C, A, R>()
                rule.addGuarded(transition)
                transitionRules[key] = rule
            } else {
                transitionRule.addGuarded(transition)
            }
        } else {
            val transition = SimpleAsyncTransition<S, E, C, A, R>(
                startState,
                event,
                targetState,
                null,
                false,
                TransitionType.NORMAL,
                action
            )
            if (transitionRule == null) {
                transitionRules[key] =
                    AsyncTransitionRules<S, E, C, A, R>(transition = transition)
            } else {
                precondition(transitionRule.transition == null) { "State $startState already has a transition without a guard for event $event" }
                transitionRule.transition = transition
            }
        }
    }

    /**
     * Declares a pop transition with a guard.
     * The current state map is removed from the stack. When the transition has a target map, a push transition follows.
     * @param startState The state where the transition starts.
     * @param event The event that triggers the transition.
     * @param targetState The state after the transition. It can be `null`.
     * @param targetMap The name of the state map for the push transition that follows. It can be `null`.
     * @param guard The guard expression. It must return `true` to use the transition.
     * @param action An optional lambda to run during the transition.
     */
    fun popTransition(
        startState: S,
        event: E,
        targetState: S?,
        targetMap: String?,
        guard: StateGuard<C, A>?,
        action: AsyncStateAction<C, A, R>?
    ) {
        precondition(parentBuilder.validEvents.contains(event)) { "Event $event is not in the valid events of the state machine: ${parentBuilder.validEvents}" }
        precondition(validStates.contains(startState)) { "State $startState is not in the valid states of this state map: $validStates" }
        val key = Pair(startState, event)
        val transitionRule = transitionRules[key]
        if (guard != null) {
            val transition = AsyncGuardedTransition<S, E, C, A, R>(
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
                val rule = AsyncTransitionRules<S, E, C, A, R>()
                rule.addGuarded(transition)
                transitionRules[key] = rule
            } else {
                transitionRule.addGuarded(transition)
            }
        } else {
            val transition = SimpleAsyncTransition<S, E, C, A, R>(
                startState,
                event,
                targetState,
                targetMap,
                false,
                TransitionType.POP,
                action
            )
            if (transitionRule == null) {
                transitionRules[key] =
                    AsyncTransitionRules<S, E, C, A, R>(transition = transition)
            } else {
                precondition(transitionRule.transition == null) { "State $startState already has a transition without a guard for event $event" }
                transitionRule.transition = transition
            }
        }
    }

    /**
     * Declares a push transition with a guard.
     * The state machine uses the transition only when the guard returns `true`.
     * The current state map goes on the stack. The target map becomes the current state map and the state changes to the target state.
     * @param startState The state where the transition starts.
     * @param event The event that triggers the transition.
     * @param targetMap The name of the target state map.
     * @param targetState The state in the target state map.
     * @param guard The guard expression. It must return `true` to use the transition.
     * @param action An optional lambda to run during the transition.
     */
    fun pushTransition(
        startState: S,
        event: E,
        targetMap: String,
        targetState: S,
        guard: StateGuard<C, A>?,
        action: AsyncStateAction<C, A, R>?
    ) {
        precondition(parentBuilder.validEvents.contains(event)) { "Event $event is not in the valid events of the state machine: ${parentBuilder.validEvents}" }
        precondition(validStates.contains(startState)) { "State $startState is not in the valid states of this state map: $validStates" }
        precondition(parentBuilder.namedStateMaps.containsKey(targetMap)) { "State map $targetMap does not exist. Defined state maps: ${parentBuilder.namedStateMaps.keys}" }
        val key = Pair(startState, event)
        val transitionRule = transitionRules[key]
        if (guard != null) {
            val transition =
                AsyncGuardedTransition<S, E, C, A, R>(
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
                val rule = AsyncTransitionRules<S, E, C, A, R>()
                rule.addGuarded(transition)
                transitionRules[key] = rule
            } else {
                transitionRule.addGuarded(transition)
            }
        } else {
            val transition = SimpleAsyncTransition<S, E, C, A, R>(
                startState,
                event,
                targetState,
                targetMap,
                false,
                TransitionType.PUSH,
                action
            )
            if (transitionRule == null) {
                transitionRules[key] =
                    AsyncTransitionRules<S, E, C, A, R>(mutableListOf(), transition)
            } else {
                precondition(transitionRule.transition == null) { "State $startState already has a transition without a guard for event $event" }
                transitionRule.transition = transition
            }
        }
    }

    /**
     * Declares an automatic transition with a guard. It has no event.
     * The state machine runs it after a transition that ends in `startState`, only when the guard returns `true`.
     * @param startState The state where the transition starts.
     * @param targetState The state after the transition.
     * @param guard The guard expression. It must return `true` to use the transition. When it is `null`, the transition has no guard.
     * @param action An optional lambda to run during the transition.
     */
    fun automatic(startState: S, targetState: S, guard: StateGuard<C, A>?, action: AsyncStateAction<C, A, R>?) {
        precondition(validStates.contains(targetState)) { "State $targetState is not in the valid states of this state map: $validStates" }
        precondition(validStates.contains(startState)) { "State $startState is not in the valid states of this state map: $validStates" }
        val transitionRule = automaticTransitions[startState]
        if (guard != null) {
            val transition = AsyncGuardedTransition<S, E, C, A, R>(
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
                automaticTransitions[startState] = AsyncTransitionRules<S, E, C, A, R>(
                    mutableListOf(transition)
                )
            } else {
                transitionRule.addGuarded(transition)
            }
        } else {
            val transition = SimpleAsyncTransition<S, E, C, A, R>(
                    startState,
                    null,
                    targetState,
                    null,
                    true,
                    TransitionType.NORMAL,
                    action
                )
            if (transitionRule == null) {
                automaticTransitions[startState] = AsyncTransitionRules<S, E, C, A, R>(
                    mutableListOf(),
                    transition
                )
            } else {
                precondition(transitionRule.transition == null) { "State $startState already has a transition without a guard" }
                transitionRule.transition = transition
            }
        }
    }

    /**
     * Declares a timeout transition.
     * The state machine starts a timer when it enters `startState`. When the timer expires, the transition occurs.
     * The state machine cancels the timer when it leaves `startState`.
     * @param startState The state where the timer starts.
     * @param targetState The state after the transition.
     * @param timeout A lambda that has the context as `this` and returns the time to wait in milliseconds.
     * @param guard The guard expression. It must return `true` to use the transition. When it is `null`, the transition has no guard.
     * @param action An optional lambda to run during the transition.
     */
    fun timeout(
        startState: S,
        targetState: S,
        timeout: C.() -> Long,
        guard: StateGuard<C, A>?,
        action: AsyncStateAction<C, A, R>?
    ) {
        precondition(validStates.contains(targetState)) { "State $targetState is not in the valid states of this state map: $validStates" }
        precondition(validStates.contains(startState)) { "State $startState is not in the valid states of this state map: $validStates" }
        val timerDefinition = timerActions[startState]
        if (guard != null) {
            val transition =
                AsyncGuardedTransition<S, E, C, A, R>(
                    startState,
                    null,
                    targetState,
                    null,
                    false,
                    TransitionType.NORMAL,
                    guard,
                    action
                )
            if (timerDefinition == null) {
                timerActions[startState] =
                    AsyncTimerDefinition(timeout, AsyncTransitionRules<S, E, C, A, R>(mutableListOf(transition)))
            } else {
                timerDefinition.rule.addGuarded(transition)
            }
        } else {
            val transition =
                SimpleAsyncTransition<S, E, C, A, R>(
                    startState,
                    null,
                    targetState,
                    null,
                    true,
                    TransitionType.TIMEOUT,
                    action
                )
            if (timerDefinition == null) {
                timerActions[startState] = AsyncTimerDefinition(
                    timeout,
                    AsyncTransitionRules<S, E, C, A, R>(mutableListOf(), transition)
                )
            } else {
                precondition(timerDefinition.rule.transition == null) { "State $startState already has a transition without a guard" }
                timerDefinition.rule.transition = transition
            }
        }
    }

    /**
     * Declares an automatic pop transition that is followed by a push transition, with a guard.
     * @param startState The state where the transition starts.
     * @param targetMap The name of the target state map.
     * @param targetState The state in the target state map.
     * @param guard The guard expression. It must return `true` to use the transition.
     * @param action An optional lambda to run during the transition.
     */
    fun automaticPop(
        startState: S,
        targetMap: String?,
        targetState: S?,
        guard: StateGuard<C, A>?,
        action: AsyncStateAction<C, A, R>?
    ) {
        precondition(validStates.contains(startState)) { "State $startState is not in the valid states of this state map: $validStates" }
        val transitionRule = automaticTransitions[startState]
        if (guard != null) {
            val transition =
                AsyncGuardedTransition<S, E, C, A, R>(
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
                automaticTransitions[startState] =
                    AsyncTransitionRules<S, E, C, A, R>(mutableListOf(transition))
            } else {
                transitionRule.addGuarded(transition)
            }
        } else {
            val transition = SimpleAsyncTransition<S, E, C, A, R>(
                startState,
                null,
                targetState,
                targetMap,
                true,
                TransitionType.POP,
                action
            )
            if (transitionRule == null) {
                automaticTransitions[startState] =
                    AsyncTransitionRules<S, E, C, A, R>(transition = transition)
            } else {
                precondition(transitionRule.transition == null) { "State $startState already has a transition without a guard" }
                transitionRule.transition = transition
            }
        }
    }

    /**
     * Declares a timeout pop transition.
     * The state machine starts a timer when it enters `startState`. When the timer expires, the current state map is removed from the stack.
     * When the transition has a target map, a push transition follows. The state machine cancels the timer when it leaves `startState`.
     * @param startState The state where the timer starts.
     * @param targetMap The name of the state map for the push transition that follows. It can be `null`.
     * @param targetState The state after the transition. It can be `null`.
     * @param timeout A lambda that has the context as `this` and returns the time to wait in milliseconds.
     * @param guard The guard expression. It must return `true` to use the transition. When it is `null`, the transition has no guard.
     * @param action An optional lambda to run during the transition.
     */
    fun timeoutPop(
        startState: S,
        targetMap: String?,
        targetState: S?,
        timeout: C.() -> Long,
        guard: StateGuard<C, A>?,
        action: AsyncStateAction<C, A, R>?
    ) {
        precondition(validStates.contains(startState)) { "State $startState is not in the valid states of this state map: $validStates" }
        val timerDefinition = timerActions[startState]
        if (guard != null) {
            val transition =
                AsyncGuardedTransition<S, E, C, A, R>(
                    startState,
                    null,
                    targetState,
                    targetMap,
                    false,
                    TransitionType.POP,
                    guard,
                    action
                )
            if (timerDefinition == null) {
                timerActions[startState] =
                    AsyncTimerDefinition(timeout, AsyncTransitionRules<S, E, C, A, R>(mutableListOf(transition)))
            } else {
                timerDefinition.rule.addGuarded(transition)
            }
        } else {
            val transition =
                SimpleAsyncTransition<S, E, C, A, R>(
                    startState,
                    null,
                    targetState,
                    targetMap,
                    false,
                    TransitionType.POP,
                    action
                )
            if (timerDefinition == null) {
                timerActions[startState] = AsyncTimerDefinition(
                    timeout,
                    AsyncTransitionRules<S, E, C, A, R>(mutableListOf(), transition)
                )
            } else {
                precondition(timerDefinition.rule.transition == null) { "State $startState already has a transition without a guard" }
                timerDefinition.rule.transition = transition
            }
        }
    }

    /**
     * Declares an automatic push transition with a guard.
     * @param startState The state where the transition starts.
     * @param targetMap The name of the target state map.
     * @param targetState The state in the target state map.
     * @param guard The guard expression. It must return `true` to use the transition.
     * @param action An optional lambda to run during the transition.
     */
    fun automaticPush(
        startState: S,
        targetMap: String,
        targetState: S,
        guard: StateGuard<C, A>?,
        action: AsyncStateAction<C, A, R>?
    ) {
        precondition(validStates.contains(startState)) { "State $startState is not in the valid states of this state map: $validStates" }
        val transitionRule = automaticTransitions[startState]
        if (guard != null) {
            val transition = AsyncGuardedTransition<S, E, C, A, R>(
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
                automaticTransitions[startState] =
                    AsyncTransitionRules<S, E, C, A, R>(mutableListOf(transition))
            } else {
                transitionRule.addGuarded(transition)
            }
        } else {
            val transition = SimpleAsyncTransition<S, E, C, A, R>(
                    startState,
                    null,
                    targetState,
                    targetMap,
                    true,
                    TransitionType.PUSH,
                    action
                )
            if (transitionRule == null) {
                automaticTransitions[startState] = AsyncTransitionRules<S, E, C, A, R>(transition = transition)
            } else {
                precondition(transitionRule.transition == null) { "State $startState already has a transition without a guard" }
                transitionRule.transition = transition
            }
        }
    }

    /**
     * Declares a timeout push transition.
     * The state machine starts a timer when it enters `startState`. When the timer expires, the current state map goes on the stack.
     * The target map becomes the current state map and the state changes to the target state.
     * The state machine cancels the timer when it leaves `startState`.
     * @param startState The state where the timer starts.
     * @param targetMap The name of the target state map.
     * @param targetState The state in the target state map.
     * @param timeout A lambda that has the context as `this` and returns the time to wait in milliseconds.
     * @param guard The guard expression. It must return `true` to use the transition. When it is `null`, the transition has no guard.
     * @param action An optional lambda to run during the transition.
     */
    fun timeoutPush(
        startState: S,
        targetMap: String,
        targetState: S,
        timeout: C.() -> Long,
        guard: StateGuard<C, A>?,
        action: AsyncStateAction<C, A, R>?
    ) {
        precondition(validStates.contains(startState)) { "State $startState is not in the valid states of this state map: $validStates" }
        val timerDefinition = timerActions[startState]
        if (guard != null) {
            val transition = AsyncGuardedTransition<S, E, C, A, R>(
                    startState,
                    null,
                    targetState,
                    targetMap,
                    false,
                    TransitionType.PUSH,
                    guard,
                    action
                )
            if (timerDefinition == null) {
                timerActions[startState] =  AsyncTimerDefinition(
                    timeout,
                    AsyncTransitionRules<S, E, C, A, R>(mutableListOf(transition))
                )
            } else {
                timerDefinition.rule.addGuarded(transition)
            }
        } else {
            val transition = SimpleAsyncTransition<S, E, C, A, R>(
                    startState,
                    null,
                    targetState,
                    targetMap,
                    true,
                    TransitionType.PUSH,
                    action
                )
            if (timerDefinition == null) {
                timerActions[startState] = AsyncTimerDefinition(
                    timeout,
                    AsyncTransitionRules<S, E, C, A, R>(mutableListOf(), transition)
                )
            } else {
                precondition(timerDefinition.rule.transition == null) { "State $startState already has a transition without a guard" }
                timerDefinition.rule.transition = transition
            }
        }
    }

    /**
     * Declares the default action for the state map.
     * The state machine runs it when no transition matches the current state and the event.
     * It is an internal transition. The state does not change, and the state machine does not run the exit action or the entry action.
     * @param action The lambda to run.
     */
    fun defaultAction(action: DefaultAsyncStateAction<C, S, E, A, R>) {
        globalDefault = action
    }

    /**
     * Declares the default entry action.
     * The state machine runs it for an external transition, after the entry action of the target state.
     * @param action The lambda to run.
     */
    fun defaultEntry(action: DefaultEntryExitAction<C, S, A>) {
        defaultEntryAction = action
    }

    /**
     * Declares the default exit action.
     * The state machine runs it for an external transition, after the exit action of the current state.
     * @param action The lambda to run.
     */
    fun defaultExit(action: DefaultEntryExitAction<C, S, A>) {
        defaultExitAction = action
    }

    /**
     * Declares the default action for one state.
     * The state machine runs it when no transition matches the state and the event.
     * @param currentState The state that the action applies to.
     * @param action The lambda to run.
     */
    fun default(currentState: S, action: DefaultAsyncStateAction<C, S, E, A, R>) {
        precondition(validStates.contains(currentState)) { "State $currentState is not in the valid states of this state map: $validStates" }
        precondition(defaultActions[currentState] == null) { "State $currentState already has a default action" }
        defaultActions[currentState] = action
    }

    /**
     * Declares a default external transition for an event.
     * The state machine uses it when no transition matches the event. The state changes to the target state.
     * @param event A pair of the event and the target state. Write it as `event to state`.
     * @param action An optional lambda to run during the transition.
     */
    fun default(event: EventState<E, S>, action: AsyncStateAction<C, A, R>?) {
        precondition(parentBuilder.validEvents.contains(event.first)) { "Event $event is not in the valid events of the state machine: ${parentBuilder.validEvents}" }
        precondition(defaultTransitions[event.first] == null) { "Event ${event.first} already has a default transition" }
        defaultTransitions[event.first] =
            DefaultAsyncTransition<S, E, C, A, R>(
                event.first,
                event.second,
                null,
                false,
                TransitionType.NORMAL,
                action
            )
    }

    /**
     * Declares a default internal transition for an event.
     * The state machine uses it when no transition matches the event. The state does not change.
     * @param event The event that triggers the transition.
     * @param action An optional lambda to run during the transition.
     */
    fun default(event: E, action: AsyncStateAction<C, A, R>?) {
        precondition(parentBuilder.validEvents.contains(event)) { "Event $event is not in the valid events of the state machine: ${parentBuilder.validEvents}" }
        precondition(defaultTransitions[event] == null) { "Event $event already has a default transition" }
        defaultTransitions[event] = DefaultAsyncTransition<S, E, C, A, R>(
            event,
            null,
            null,
            false,
            TransitionType.NORMAL,
            action
        )
    }

    /**
     * Declares the entry action for a state.
     * The state machine runs it for an external transition that has this state as the target state.
     * @param currentState The state that the action applies to.
     * @param action The lambda to run.
     */
    fun entry(currentState: S, action: DefaultEntryExitAction<C, S, A>) {
        precondition(validStates.contains(currentState)) { "State $currentState is not in the valid states of this state map: $validStates" }
        precondition(entryActions[currentState] == null) { "State $currentState already has an entry action" }
        entryActions[currentState] = action
    }

    /**
     * Declares the exit action for a state.
     * The state machine runs it for an external transition that starts in this state.
     * @param currentState The state that the action applies to.
     * @param action The lambda to run.
     */
    fun exit(currentState: S, action: DefaultEntryExitAction<C, S, A>) {
        precondition(validStates.contains(currentState)) { "State $currentState is not in the valid states of this state map: $validStates" }
        precondition(exitActions[currentState] == null) { "State $currentState already has an exit action" }
        exitActions[currentState] = action
    }

    /**
     * Creates the immutable `StateMapDefinition` from the declarations in this builder.
     * @param afterStateChangeAction The action that the state machine runs after the state has changed. It can be `null`.
     */
    fun toMap(afterStateChangeAction: AsyncStateChangeAction<C, S>?): AsyncStateMapDefinition<S, E, C, A, R> =
        AsyncStateMapDefinition(
            this.name,
            this.validStates,
            this.invariants.toSet(),
            this.transitionRules.toMap(),
            this.defaultTransitions.toMap(),
            this.entryActions.toMap(),
            this.exitActions.toMap(),
            this.defaultActions.toMap(),
            this.automaticTransitions.toMap(),
            this.timerActions.toMap(),
            this.globalDefault,
            this.defaultEntryAction,
            this.defaultExitAction,
            afterStateChangeAction
        )
}
