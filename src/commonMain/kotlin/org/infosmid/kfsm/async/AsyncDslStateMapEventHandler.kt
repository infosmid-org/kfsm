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

import org.infosmid.kfsm.AsyncStateAction
import org.infosmid.kfsm.DefaultAsyncStateAction
import org.infosmid.kfsm.DefaultEntryExitAction
import org.infosmid.kfsm.EventState
import org.infosmid.kfsm.StateGuard

/**
 * The DSL receiver inside a `whenState` block of an asynchronous state machine.
 * It declares the transitions and actions for one state. The actions are `suspend` functions.
 */
class AsyncDslStateMapEventHandler<S, E, C, A, R>(
    private val currentState: S,
    private val fsm: AsyncStateMapBuilder<S, E, C, A, R>
) {
    /**
     * Declares the default action for the state.
     * The state machine runs it when no other transition matches the event.
     * @param action The lambda to run.
     */
    fun default(action: DefaultAsyncStateAction<C, S, E, A, R>) {
        fsm.default(currentState, action)
    }

    /**
     * Declares the entry action for the state.
     * The state machine runs it for an external transition that has this state as the target state.
     * The state has not changed when the action runs.
     * @param action The lambda to run.
     */
    fun onEntry(action: DefaultEntryExitAction<C, S, A>) {
        fsm.entry(currentState, action)
    }

    /**
     * Declares the exit action for the state.
     * The state machine runs it for an external transition that starts in this state.
     * The state has not changed when the action runs.
     * @param action The lambda to run.
     */
    fun onExit(action: DefaultEntryExitAction<C, S, A>) {
        fsm.exit(currentState, action)
    }

    /**
     * Declares an internal transition. The state does not change.
     * @param event A pair of the event and the target state. Write it as `EVENT to STATE`.
     * @param action An optional `suspend` lambda to run during the transition.
     */
    fun onEvent(
        event: EventState<E, S>,
        action: AsyncStateAction<C, A, R>?
    ): AsyncDslStateMapEventHandler<S, E, C, A, R> {
        fsm.transition(currentState, event.first, event.second, null, action)
        return this
    }

    /**
     * Declares an internal transition with a guard. The state does not change.
     * The state machine uses the transition only when the guard returns `true`.
     * @param event A pair of the event and the target state. Write it as `EVENT to STATE`.
     * @param guard The guard expression. It must return `true` to use the transition.
     * @param action An optional `suspend` lambda to run during the transition.
     */
    fun onEvent(
        event: EventState<E, S>,
        guard: StateGuard<C, A>,
        action: AsyncStateAction<C, A, R>?
    ): AsyncDslStateMapEventHandler<S, E, C, A, R> {
        fsm.transition(currentState, event.first, event.second, guard, action)
        return this
    }

    /**
     * Declares an internal transition. The state does not change.
     * @param event The event that triggers the transition.
     * @param action An optional `suspend` lambda to run during the transition.
     */
    fun onEvent(event: E, action: AsyncStateAction<C, A, R>?): AsyncDslStateMapEventHandler<S, E, C, A, R> {
        fsm.transition(currentState, event, null, null, action)
        return this
    }

    /**
     * Declares an internal transition with a guard. The state does not change.
     * The state machine uses the transition only when the guard returns `true`.
     * @param event The event that triggers the transition.
     * @param guard The guard expression. It must return `true` to use the transition.
     * @param action An optional `suspend` lambda to run during the transition.
     */
    fun onEvent(
        event: E,
        guard: StateGuard<C, A>,
        action: AsyncStateAction<C, A, R>?
    ): AsyncDslStateMapEventHandler<S, E, C, A, R> {
        fsm.transition(currentState, event, null, guard, action)
        return this
    }

    /**
     * Declares a push transition.
     * The current state map goes on the stack. The target map becomes the current state map and the state changes to the target state.
     * @param event The event that triggers the transition.
     * @param targetMap The name of the target state map.
     * @param targetState The state in the target state map.
     * @param action An optional `suspend` lambda to run during the transition.
     */
    fun onEventPush(
        event: E,
        targetMap: String,
        targetState: S,
        action: AsyncStateAction<C, A, R>?
    ): AsyncDslStateMapEventHandler<S, E, C, A, R> {
        fsm.pushTransition(currentState, event, targetMap, targetState, null, action)
        return this
    }

    /**
     * Declares a push transition with a guard.
     * The current state map goes on the stack. The target map becomes the current state map and the state changes to the target state.
     * The state machine uses the transition only when the guard returns `true`.
     * @param event The event that triggers the transition.
     * @param targetMap The name of the target state map.
     * @param targetState The state in the target state map.
     * @param guard The guard expression. It must return `true` to use the transition.
     * @param action An optional `suspend` lambda to run during the transition.
     */
    fun onEventPush(
        event: E,
        targetMap: String,
        targetState: S,
        guard: StateGuard<C, A>,
        action: AsyncStateAction<C, A, R>?
    ): AsyncDslStateMapEventHandler<S, E, C, A, R> {
        fsm.pushTransition(currentState, event, targetMap, targetState, guard, action)
        return this
    }

    /**
     * Declares a pop transition without a target state.
     * The state machine removes the current state map from the stack. The previous state map becomes the current state map.
     * @param event The event that triggers the transition.
     * @param action An optional `suspend` lambda to run during the transition.
     */
    fun onEventPop(event: E, action: AsyncStateAction<C, A, R>?): AsyncDslStateMapEventHandler<S, E, C, A, R> {
        fsm.popTransition(currentState, event, null, null, null, action)
        return this
    }

    /**
     * Declares a pop transition with a target state.
     * The state machine removes the current state map from the stack. The previous state map becomes the current state map.
     * @param event A pair of the event and the target state. Write it as `EVENT to STATE`.
     * @param action An optional `suspend` lambda to run during the transition.
     */
    fun onEventPop(event: Pair<E, S>, action: AsyncStateAction<C, A, R>?): AsyncDslStateMapEventHandler<S, E, C, A, R> {
        fsm.popTransition(currentState, event.first, event.second, null, null, action)
        return this
    }

    /**
     * Declares a pop transition with a guard and a target state.
     * The state machine removes the current state map from the stack. The previous state map becomes the current state map.
     * The state machine uses the transition only when the guard returns `true`.
     * @param event A pair of the event and the target state. Write it as `EVENT to STATE`.
     * @param guard The guard expression. It must return `true` to use the transition.
     * @param action An optional `suspend` lambda to run during the transition.
     */
    fun onEventPop(
        event: Pair<E, S>,
        guard: StateGuard<C, A>,
        action: AsyncStateAction<C, A, R>?
    ): AsyncDslStateMapEventHandler<S, E, C, A, R> {
        fsm.popTransition(currentState, event.first, event.second, null, guard, action)
        return this
    }

    /**
     * Declares a pop transition that is followed by a push transition.
     * The state machine removes the current state map from the stack. The previous state map becomes the current state map.
     * @param event The event that triggers the transition.
     * @param targetMap The name of the target state map.
     * @param targetState The state in the target state map.
     * @param action An optional `suspend` lambda to run during the transition.
     */
    fun onEventPop(
        event: E,
        targetMap: String,
        targetState: S,
        action: AsyncStateAction<C, A, R>?
    ): AsyncDslStateMapEventHandler<S, E, C, A, R> {
        fsm.popTransition(currentState, event, targetState, targetMap, null, action)
        return this
    }

    /**
     * Declares a pop transition with a guard without a target state.
     * The state machine removes the current state map from the stack. The previous state map becomes the current state map.
     * The state machine uses the transition only when the guard returns `true`.
     * @param event The event that triggers the transition.
     * @param guard The guard expression. It must return `true` to use the transition.
     * @param action An optional `suspend` lambda to run during the transition.
     */
    fun onEventPop(
        event: E,
        guard: StateGuard<C, A>,
        action: AsyncStateAction<C, A, R>?
    ): AsyncDslStateMapEventHandler<S, E, C, A, R> {
        fsm.popTransition(currentState, event, null, null, guard, action)
        return this
    }

    /**
     * Declares a pop transition that is followed by a push transition with a guard.
     * The state machine removes the current state map from the stack. The previous state map becomes the current state map.
     * The state machine uses the transition only when the guard returns `true`.
     * @param event The event that triggers the transition.
     * @param targetMap The name of the target state map.
     * @param targetState The state in the target state map.
     * @param guard The guard expression. It must return `true` to use the transition.
     * @param action An optional `suspend` lambda to run during the transition.
     */
    fun onEventPop(
        event: E,
        targetMap: String,
        targetState: S,
        guard: StateGuard<C, A>,
        action: AsyncStateAction<C, A, R>?
    ): AsyncDslStateMapEventHandler<S, E, C, A, R> {
        fsm.popTransition(currentState, event, targetState, targetMap, guard, action)
        return this
    }

    /**
     * Declares an automatic transition. It has no event.
     * The state machine runs it after a transition that ends in this state.
     * @param targetState The state after the transition.
     * @param action An optional `suspend` lambda to run during the transition.
     */
    fun automatic(targetState: S, action: AsyncStateAction<C, A, R>?) {
        fsm.automatic(currentState, targetState, null, action)
    }

    /**
     * Declares an automatic transition with a guard. It has no event.
     * The state machine runs it after a transition that ends in this state, only when the guard returns `true`.
     * @param targetState The state after the transition.
     * @param guard The guard expression. It must return `true` to use the transition.
     * @param action An optional `suspend` lambda to run during the transition.
     */
    fun automatic(targetState: S, guard: StateGuard<C, A>, action: AsyncStateAction<C, A, R>?) {
        fsm.automatic(currentState, targetState, guard, action)
    }

    /**
     * Declares a timeout transition.
     * The state machine starts a timer when it enters this state. When the timer expires, the transition occurs.
     * The state machine cancels the timer when it leaves this state.
     * @param targetState The state after the transition.
     * @param timeout The time to wait in milliseconds.
     * @param action An optional `suspend` lambda to run during the transition.
     */
    fun timeout(targetState: S, timeout: Long, action: AsyncStateAction<C, A, R>?) {
        fsm.timeout(currentState, targetState, { timeout }, null, action)
    }

    /**
     * Declares a timeout transition.
     * The state machine starts a timer when it enters this state. When the timer expires, the transition occurs.
     * The state machine cancels the timer when it leaves this state.
     * @param targetState The state after the transition.
     * @param timeout A lambda that has the context as `this` and returns the time to wait in milliseconds.
     * @param action An optional `suspend` lambda to run during the transition.
     */
    fun timeout(targetState: S, timeout: C.() -> Long, action: AsyncStateAction<C, A, R>?) {
        fsm.timeout(currentState, targetState, timeout, null, action)
    }

    /**
     * Declares a timeout transition with a guard.
     * The state machine starts a timer when it enters this state. When the timer expires, the transition occurs.
     * The state machine cancels the timer when it leaves this state.
     * The state machine uses the transition only when the guard returns `true`.
     * @param targetState The state after the transition.
     * @param timeout A lambda that has the context as `this` and returns the time to wait in milliseconds.
     * @param guard The guard expression. It must return `true` to use the transition.
     * @param action An optional `suspend` lambda to run during the transition.
     */
    fun timeout(targetState: S, timeout: C.() -> Long, guard: StateGuard<C, A>, action: AsyncStateAction<C, A, R>?) {
        fsm.timeout(currentState, targetState, timeout, guard, action)
    }

    /**
     * Declares a timeout transition with a guard.
     * The state machine starts a timer when it enters this state. When the timer expires, the transition occurs.
     * The state machine cancels the timer when it leaves this state.
     * The state machine uses the transition only when the guard returns `true`.
     * @param targetState The state after the transition.
     * @param timeout The time to wait in milliseconds.
     * @param guard The guard expression. It must return `true` to use the transition.
     * @param action An optional `suspend` lambda to run during the transition.
     */
    fun timeout(targetState: S, timeout: Long, guard: StateGuard<C, A>, action: AsyncStateAction<C, A, R>?) {
        fsm.timeout(currentState, targetState, { timeout }, guard, action)
    }

    /**
     * Declares an automatic pop transition with a target state.
     * The state machine runs it after a transition that ends in this state.
     * @param targetState The state of the previous state map after the transition.
     * @param action An optional `suspend` lambda to run during the transition.
     */
    fun automaticPop(targetState: S, action: AsyncStateAction<C, A, R>?) {
        fsm.automaticPop(currentState, null, targetState, null, action)
    }

    /**
     * Declares an automatic pop transition without a target state.
     * The state machine runs it after a transition that ends in this state.
     * @param action An optional `suspend` lambda to run during the transition.
     */
    fun automaticPop(action: AsyncStateAction<C, A, R>?) {
        fsm.automaticPop(currentState, null, null, null, action)
    }

    /**
     * Declares an automatic pop transition that is followed by a push transition.
     * The state machine runs it after a transition that ends in this state.
     * @param targetMap The name of the target state map.
     * @param targetState The state in the target state map.
     * @param action An optional `suspend` lambda to run during the transition.
     */
    fun automaticPop(targetMap: String, targetState: S, action: AsyncStateAction<C, A, R>?) {
        fsm.automaticPop(currentState, targetMap, targetState, null, action)
    }

    /**
     * Declares an automatic pop transition with a target state, and with a guard.
     * The state machine runs it after a transition that ends in this state, only when the guard returns `true`.
     * @param targetState The state of the previous state map after the transition.
     * @param guard The guard expression. It must return `true` to use the transition.
     * @param action An optional `suspend` lambda to run during the transition.
     */
    fun automaticPop(targetState: S, guard: StateGuard<C, A>, action: AsyncStateAction<C, A, R>?) {
        fsm.automaticPop(currentState, null, targetState, guard, action)
    }

    /**
     * Declares an automatic pop transition without a target state, and with a guard.
     * The state machine runs it after a transition that ends in this state, only when the guard returns `true`.
     * @param guard The guard expression. It must return `true` to use the transition.
     * @param action An optional `suspend` lambda to run during the transition.
     */
    fun automaticPop(guard: StateGuard<C, A>, action: AsyncStateAction<C, A, R>?) {
        fsm.automaticPop(currentState, null, null, guard, action)
    }

    /**
     * Declares an automatic pop transition that is followed by a push transition, and with a guard.
     * The state machine runs it after a transition that ends in this state, only when the guard returns `true`.
     * @param targetMap The name of the target state map.
     * @param targetState The state in the target state map.
     * @param guard The guard expression. It must return `true` to use the transition.
     * @param action An optional `suspend` lambda to run during the transition.
     */
    fun automaticPop(targetMap: String, targetState: S, guard: StateGuard<C, A>, action: AsyncStateAction<C, A, R>?) {
        fsm.automaticPop(currentState, targetMap, targetState, guard, action)
    }

    /**
     * Declares a timeout pop transition with a target state.
     * The state machine starts a timer when it enters this state. When the timer expires, it removes the current state map from the stack.
     * The state machine cancels the timer when it leaves this state.
     * @param targetState The state of the previous state map after the transition.
     * @param timeout The time to wait in milliseconds.
     * @param action An optional `suspend` lambda to run during the transition.
     */
    fun timeoutPop(targetState: S, timeout: Long, action: AsyncStateAction<C, A, R>?) {
        fsm.timeoutPop(currentState, null, targetState, { timeout }, null, action)
    }

    /**
     * Declares a timeout pop transition with a target state.
     * The state machine starts a timer when it enters this state. When the timer expires, it removes the current state map from the stack.
     * The state machine cancels the timer when it leaves this state.
     * @param targetState The state of the previous state map after the transition.
     * @param timeout A lambda that has the context as `this` and returns the time to wait in milliseconds.
     * @param action An optional `suspend` lambda to run during the transition.
     */
    fun timeoutPop(targetState: S, timeout: C.() -> Long, action: AsyncStateAction<C, A, R>?) {
        fsm.timeoutPop(currentState, null, targetState, timeout, null, action)
    }

    /**
     * Declares a timeout pop transition without a target state.
     * The state machine starts a timer when it enters this state. When the timer expires, it removes the current state map from the stack.
     * The state machine cancels the timer when it leaves this state.
     * @param timeout The time to wait in milliseconds.
     * @param action An optional `suspend` lambda to run during the transition.
     */
    fun timeoutPop(timeout: Long, action: AsyncStateAction<C, A, R>?) {
        fsm.timeoutPop(currentState, null, null, { timeout }, null, action)
    }

    /**
     * Declares a timeout pop transition without a target state.
     * The state machine starts a timer when it enters this state. When the timer expires, it removes the current state map from the stack.
     * The state machine cancels the timer when it leaves this state.
     * @param timeout A lambda that has the context as `this` and returns the time to wait in milliseconds.
     * @param action An optional `suspend` lambda to run during the transition.
     */
    fun timeoutPop(timeout: C.() -> Long, action: AsyncStateAction<C, A, R>?) {
        fsm.timeoutPop(currentState, null, null, timeout, null, action)
    }

    /**
     * Declares a timeout pop transition that is followed by a push transition.
     * The state machine starts a timer when it enters this state. When the timer expires, it removes the current state map from the stack.
     * The state machine cancels the timer when it leaves this state.
     * @param targetMap The name of the target state map.
     * @param targetState The state in the target state map.
     * @param timeout The time to wait in milliseconds.
     * @param action An optional `suspend` lambda to run during the transition.
     */
    fun timeoutPop(targetMap: String, targetState: S, timeout: Long, action: AsyncStateAction<C, A, R>?) {
        fsm.timeoutPop(currentState, targetMap, targetState, { timeout }, null, action)
    }

    /**
     * Declares a timeout pop transition that is followed by a push transition.
     * The state machine starts a timer when it enters this state. When the timer expires, it removes the current state map from the stack.
     * The state machine cancels the timer when it leaves this state.
     * @param targetMap The name of the target state map.
     * @param targetState The state in the target state map.
     * @param timeout A lambda that has the context as `this` and returns the time to wait in milliseconds.
     * @param action An optional `suspend` lambda to run during the transition.
     */
    fun timeoutPop(targetMap: String, targetState: S, timeout: C.() -> Long, action: AsyncStateAction<C, A, R>?) {
        fsm.timeoutPop(currentState, targetMap, targetState, timeout, null, action)
    }

    /**
     * Declares a timeout pop transition with a target state, and with a guard.
     * The state machine starts a timer when it enters this state. When the timer expires, it removes the current state map from the stack.
     * The state machine cancels the timer when it leaves this state.
     * The state machine uses the transition only when the guard returns `true`.
     * @param targetState The state of the previous state map after the transition.
     * @param timeout The time to wait in milliseconds.
     * @param guard The guard expression. It must return `true` to use the transition.
     * @param action An optional `suspend` lambda to run during the transition.
     */
    fun timeoutPop(targetState: S, timeout: Long, guard: StateGuard<C, A>, action: AsyncStateAction<C, A, R>?) {
        fsm.timeoutPop(currentState, null, targetState, { timeout }, guard, action)
    }

    /**
     * Declares a timeout pop transition with a target state, and with a guard.
     * The state machine starts a timer when it enters this state. When the timer expires, it removes the current state map from the stack.
     * The state machine cancels the timer when it leaves this state.
     * The state machine uses the transition only when the guard returns `true`.
     * @param targetState The state of the previous state map after the transition.
     * @param timeout A lambda that has the context as `this` and returns the time to wait in milliseconds.
     * @param guard The guard expression. It must return `true` to use the transition.
     * @param action An optional `suspend` lambda to run during the transition.
     */
    fun timeoutPop(targetState: S, timeout: C.() -> Long, guard: StateGuard<C, A>, action: AsyncStateAction<C, A, R>?) {
        fsm.timeoutPop(currentState, null, targetState, timeout, guard, action)
    }

    /**
     * Declares a timeout pop transition without a target state, and with a guard.
     * The state machine starts a timer when it enters this state. When the timer expires, it removes the current state map from the stack.
     * The state machine cancels the timer when it leaves this state.
     * The state machine uses the transition only when the guard returns `true`.
     * @param timeout The time to wait in milliseconds.
     * @param guard The guard expression. It must return `true` to use the transition.
     * @param action An optional `suspend` lambda to run during the transition.
     */
    fun timeoutPop(timeout: Long, guard: StateGuard<C, A>, action: AsyncStateAction<C, A, R>?) {
        fsm.timeoutPop(currentState, null, null, { timeout }, guard, action)
    }

    /**
     * Declares a timeout pop transition without a target state, and with a guard.
     * The state machine starts a timer when it enters this state. When the timer expires, it removes the current state map from the stack.
     * The state machine cancels the timer when it leaves this state.
     * The state machine uses the transition only when the guard returns `true`.
     * @param timeout A lambda that has the context as `this` and returns the time to wait in milliseconds.
     * @param guard The guard expression. It must return `true` to use the transition.
     * @param action An optional `suspend` lambda to run during the transition.
     */
    fun timeoutPop(timeout: C.() -> Long, guard: StateGuard<C, A>, action: AsyncStateAction<C, A, R>?) {
        fsm.timeoutPop(currentState, null, null, timeout, guard, action)
    }

    /**
     * Declares a timeout pop transition that is followed by a push transition, and with a guard.
     * The state machine starts a timer when it enters this state. When the timer expires, it removes the current state map from the stack.
     * The state machine cancels the timer when it leaves this state.
     * The state machine uses the transition only when the guard returns `true`.
     * @param targetMap The name of the target state map.
     * @param targetState The state in the target state map.
     * @param timeout A lambda that has the context as `this` and returns the time to wait in milliseconds.
     * @param guard The guard expression. It must return `true` to use the transition.
     * @param action An optional `suspend` lambda to run during the transition.
     */
    fun timeoutPop(
        targetMap: String,
        targetState: S,
        timeout: C.() -> Long,
        guard: StateGuard<C, A>,
        action: AsyncStateAction<C, A, R>?
    ) {
        fsm.timeoutPop(currentState, targetMap, targetState, timeout, guard, action)
    }

    /**
     * Declares a timeout pop transition that is followed by a push transition, and with a guard.
     * The state machine starts a timer when it enters this state. When the timer expires, it removes the current state map from the stack.
     * The state machine cancels the timer when it leaves this state.
     * The state machine uses the transition only when the guard returns `true`.
     * @param targetMap The name of the target state map.
     * @param targetState The state in the target state map.
     * @param timeout The time to wait in milliseconds.
     * @param guard The guard expression. It must return `true` to use the transition.
     * @param action An optional `suspend` lambda to run during the transition.
     */
    fun timeoutPop(
        targetMap: String,
        targetState: S,
        timeout: Long,
        guard: StateGuard<C, A>,
        action: AsyncStateAction<C, A, R>?
    ) {
        fsm.timeoutPop(currentState, targetMap, targetState, { timeout }, guard, action)
    }

    /**
     * Declares an automatic push transition.
     * The state machine runs it after a transition that ends in this state.
     * @param targetMap The name of the target state map.
     * @param targetState The state in the target state map.
     * @param action An optional `suspend` lambda to run during the transition.
     */
    fun automaticPush(targetMap: String, targetState: S, action: AsyncStateAction<C, A, R>?) {
        fsm.automaticPush(currentState, targetMap, targetState, null, action)
    }

    /**
     * Declares an automatic push transition with a guard.
     * The state machine runs it after a transition that ends in this state, only when the guard returns `true`.
     * @param targetMap The name of the target state map.
     * @param targetState The state in the target state map.
     * @param guard The guard expression. It must return `true` to use the transition.
     * @param action An optional `suspend` lambda to run during the transition.
     */
    fun automaticPush(targetMap: String, targetState: S, guard: StateGuard<C, A>, action: AsyncStateAction<C, A, R>?) {
        fsm.automaticPush(currentState, targetMap, targetState, guard, action)
    }

    /**
     * Declares a timeout push transition.
     * The state machine starts a timer when it enters this state. When the timer expires, the current state map goes on the stack.
     * The target map becomes the current state map and the state changes to the target state.
     * The state machine cancels the timer when it leaves this state.
     * @param targetMap The name of the target state map.
     * @param targetState The state in the target state map.
     * @param timeout A lambda that has the context as `this` and returns the time to wait in milliseconds.
     * @param action An optional `suspend` lambda to run during the transition.
     */
    fun timeoutPush(targetMap: String, targetState: S, timeout: C.() -> Long, action: AsyncStateAction<C, A, R>?) {
        fsm.timeoutPush(currentState, targetMap, targetState, timeout, null, action)
    }

    /**
     * Declares a timeout push transition.
     * The state machine starts a timer when it enters this state. When the timer expires, the current state map goes on the stack.
     * The target map becomes the current state map and the state changes to the target state.
     * The state machine cancels the timer when it leaves this state.
     * @param targetMap The name of the target state map.
     * @param targetState The state in the target state map.
     * @param timeout The time to wait in milliseconds.
     * @param action An optional `suspend` lambda to run during the transition.
     */
    fun timeoutPush(targetMap: String, targetState: S, timeout: Long, action: AsyncStateAction<C, A, R>?) {
        fsm.timeoutPush(currentState, targetMap, targetState, { timeout }, null, action)
    }

    /**
     * Declares a timeout push transition with a guard.
     * The state machine starts a timer when it enters this state. When the timer expires, the current state map goes on the stack.
     * The target map becomes the current state map and the state changes to the target state.
     * The state machine cancels the timer when it leaves this state.
     * The state machine uses the transition only when the guard returns `true`.
     * @param targetMap The name of the target state map.
     * @param targetState The state in the target state map.
     * @param timeout The time to wait in milliseconds.
     * @param guard The guard expression. It must return `true` to use the transition.
     * @param action An optional `suspend` lambda to run during the transition.
     */
    fun timeoutPush(
        targetMap: String,
        targetState: S,
        timeout: Long,
        guard: StateGuard<C, A>,
        action: AsyncStateAction<C, A, R>?
    ) {
        fsm.timeoutPush(currentState, targetMap, targetState, { timeout }, guard, action)
    }

    /**
     * Declares a timeout push transition with a guard.
     * The state machine starts a timer when it enters this state. When the timer expires, the current state map goes on the stack.
     * The target map becomes the current state map and the state changes to the target state.
     * The state machine cancels the timer when it leaves this state.
     * The state machine uses the transition only when the guard returns `true`.
     * @param targetMap The name of the target state map.
     * @param targetState The state in the target state map.
     * @param timeout A lambda that has the context as `this` and returns the time to wait in milliseconds.
     * @param guard The guard expression. It must return `true` to use the transition.
     * @param action An optional `suspend` lambda to run during the transition.
     */
    fun timeoutPush(
        targetMap: String,
        targetState: S,
        timeout: C.() -> Long,
        guard: StateGuard<C, A>,
        action: AsyncStateAction<C, A, R>?
    ) {
        fsm.timeoutPush(currentState, targetMap, targetState, timeout, guard, action)
    }
}
