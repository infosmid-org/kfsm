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

import org.infosmid.dbc.InvariantException
import org.infosmid.dbc.PreConditionException
import org.infosmid.dbc.precondition

/**
 * An action that the state machine runs during a transition.
 * The context is the receiver (`this`) of the lambda.
 * @author Corneil du Plessis
 * @soundtrack Wolfgang Amadeus Mozart
 * @param C The context type. The lambda uses it as `this`.
 * @param A The type of the event argument.
 * @param R The type of the return value.
 */
typealias SyncStateAction<C, A, R> = C.(A?) -> R?

/**
 * The `suspend` version of [SyncStateAction].
 * @param C The context type. The lambda uses it as `this`.
 * @param A The type of the event argument.
 * @param R The type of the return value.
 */
typealias AsyncStateAction<C, A, R> = suspend C.(A?) -> R?

/**
 * A guard expression. The state machine uses it to select a transition.
 * The transition is selected only when the guard returns `true`.
 * @param C The context type. The lambda uses it as `this`.
 * @param A The type of the event argument.
 */
typealias StateGuard<C, A> = C.(A?) -> Boolean

/**
 * An expression that derives the current state from the context.
 * Use it when you create more than one state machine instance for the same context.
 * For example, the context can be a persisted or serializable entity.
 * @param C The context type. The lambda uses it as `this`.
 * @param S The type of the state that the lambda returns.
 */
typealias StateQuery<C, S> = (C.() -> S)

/**
 * A pair of a state and the name of its state map.
 */
typealias StateMapItem<S> = Pair<S, String>

/**
 * A list of [StateMapItem]. The first item is the bottom of the state map stack.
 */
typealias StateMapList<S> = List<StateMapItem<S>>

/**
 * An expression that derives the state map stack from the context.
 * The context is the receiver (`this`) of the lambda. The lambda returns a [StateMapList].
 */
typealias StateMapQuery<C, S> = (C.() -> StateMapList<S>)

/**
 * A default entry action or exit action.
 * The state machine runs it for an external transition.
 * @param C The context type. The lambda uses it as `this`.
 * @param S The lambda receives the current state as the first argument and the target state as the second argument.
 * @param A The lambda receives the event argument as the third argument.
 */
typealias DefaultEntryExitAction<C, S, A> = C.(S, S, A?) -> Unit

/**
 * An action that the state machine runs after a transition when the state has changed.
 * The lambda receives the previous state as the first argument and the new state as the second argument.
 * @param C The context type. The lambda uses it as `this`.
 * @param S The state type.
 */
typealias StateChangeAction<C, S> = C.(S, S) -> Unit

/**
 * The `suspend` version of [StateChangeAction].
 * @param C The context type. The lambda uses it as `this`.
 * @param S The state type.
 */
typealias AsyncStateChangeAction<C, S> = suspend C.(S, S) -> Unit

/**
 * A default action. The state machine runs it when no other transition matches the current state and event.
 * A default action does not change the state. It is an internal transition.
 * @param C The context type. The lambda uses it as `this`.
 * @param S The lambda receives the current state as the first argument.
 * @param E The lambda receives the event as the second argument.
 * @param A The lambda receives the event argument as the third argument.
 * @param R The type of the return value.
 */
typealias DefaultStateAction<C, S, E, A, R> = C.(S, E, A?) -> R?

/**
 * The `suspend` version of [DefaultStateAction].
 */
typealias DefaultAsyncStateAction<C, S, E, A, R> = suspend C.(S, E, A?) -> R?

/**
 * A pair of an event and a target state. Write it as `event to state`.
 * @param E The event type. It is the first element of the pair.
 * @param S The target state type. It is the second element of the pair.
 */
typealias EventState<E, S> = Pair<E, S>

/**
 * The state of the state machine. It has one item for each state map on the stack.
 * You can store it and use it later to create a state machine instance.
 */
typealias ExternalState<S> = StateMapList<S>

/**
 * The kinds of transition.
 */
enum class TransitionType {
    /**
     * An event triggers the transition. The transition can change the state or keep the same state. It can run an action.
     */
    NORMAL,

    /**
     * A push transition puts the current state map on the stack.
     * The named state map becomes the current state map, and the state changes to the target state.
     */
    PUSH,

    /**
     * A pop transition removes the current state map from the stack. The previous state map becomes the current state map.
     * If the pop transition has a target map or a target state, a push transition or a normal transition follows.
     */
    POP,

    /**
     * A default transition runs when no configured state and event pair matches.
     */
    DEFAULT,

    /**
     * A timeout transition runs when the configured time expires after the state is entered.
     * The state machine cancels the timeout when it runs the exit action of the state.
     */
    TIMEOUT
}

typealias Condition<C> = C.() -> Boolean

/**
 * Checks the invariant of the state machine.
 * @param context The context that the condition checks.
 * @param condition A pair of the invariant message and the condition. When it is `null`, the check passes.
 * @throws InvariantException if the condition returns `false`.
 */
@Throws(InvariantException::class)
fun <C> checkInvariant(context: C, condition: Pair<String, Condition<C>>?) {
    val check = condition?.second?.invoke(context) ?: true

    if (!check) throw InvariantException(condition.first)
}

/**
 * A simple generic stack.
 */
class Stack<T> {
    private val elements: MutableList<T> = mutableListOf()

    /**
     * Push an element onto the stack.
     * @param value The element to push.
     */
    fun push(value: T) = elements.add(value)

    /**
     * Removes the element at the top of the stack and returns it.
     * @return The element that was at the top of the stack.
     * @throws PreConditionException if the stack is empty.
     */
    @Throws(PreConditionException::class)
    fun pop(): T {
        precondition(isNotEmpty()) { "The state map stack is empty" }
        return elements.removeAt(elements.lastIndex)
    }

    /**
     * Checks if the stack is empty.
     * @return `true` if the stack is empty.
     */
    fun isEmpty() = elements.isEmpty()

    /**
     * Checks if the stack is not empty.
     * @return `true` if the stack is not empty.
     */
    fun isNotEmpty() = elements.isNotEmpty()

    /**
     * Reads the top of the stack without removing it.
     * @return The element at the top of the stack, or `null` if the stack is empty.
     */
    fun peek(): T? = elements.lastOrNull()

    /**
     * Reads all the elements on the stack.
     * @return A copy of the elements. The top of the stack is the last element.
     */
    fun peekContent(): List<T> = elements.toList()
}
