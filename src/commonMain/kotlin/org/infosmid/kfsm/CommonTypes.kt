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
@file:OptIn(ExperimentalContracts::class)

package org.infosmid.kfsm

import kotlin.Pair
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

/**
 * This represents an action that may be invoked during a transition.
 * @author Corneil du Plessis
 * @soundtrack Wolfgang Amadeus Mozart
 * @param C The context: C will be available to the lambda.
 * @param A The type of the argument to the action
 * @param R The return type of the action
 */
typealias SyncStateAction<C, A, R> = C.(A?) -> R?

typealias AsyncStateAction<C, A, R> = suspend C.(A?) -> R?

/**
 * This represents a guard expression that may be used to select a specific transition.
 * @param C The context: C will be available to the lambda
 * @param A The type of the argument to the action
 */
typealias StateGuard<C, A> = C.(A?) -> Boolean

/**
 * This represents an expression to determine the state of the context. This is useful when creating an instance of a
 * statemachine multiple times during the lifecycle of a context represented by a persisted or serializable entity.
 * @param C The context:C will be available to the lambda.
 * @param S The type of the state that should be returned by the lambda.
 */
typealias StateQuery<C, S> = (C.() -> S)

/**
 * Alias for a pair of S, String
 */
typealias StateMapItem<S> = Pair<S, String>

/**
 * Alias for a list of pairs of S,String
 */
typealias StateMapList<S> = List<StateMapItem<S>>

/**
 * Alias for a lambda that will have the context as this and return a `StateMapList`
 */
typealias StateMapQuery<C, S> = (C.() -> StateMapList<S>)

/**
 * This represents a default action that will be invoked when entering or exiting a state with an external state transitions.
 * @param C The context: C will be available to the lambda
 * @param S currentState: S and targetState: S will be available to the lambda as 1st and 2nd arguments.
 * @param A argumentType: A will be available to the lambda as the 3rd argument.
 */
typealias DefaultEntryExitAction<C, S, A> = C.(S, S, A?) -> Unit
typealias StateChangeAction<C, S> = C.(S, S) -> Unit
typealias AsyncStateChangeAction<C, S> = suspend C.(S, S) -> Unit

/**
 * This represents a default action for a specific event. These action will not cause changes in state.
 * They are internal transitions.
 * @param C The context: C will be available to the lambda
 * @param S The currentState:S will be available to the lambda
 * @param E The event: E will be available to the lambda
 * @param A argumentType: A will be available to the lambda as the 3rd argument.
 */
typealias DefaultStateAction<C, S, E, A, R> = C.(S, E, A?) -> R?

typealias DefaultAsyncStateAction<C, S, E, A, R> = suspend C.(S, E, A?) -> R?

/**
 * This represents an event and targetState pair that can be written as `event to state`
 * @param E The event: E will be the first element of the pair
 * @param S The targetState: S will be the second element of the pair.
 */
typealias EventState<E, S> = Pair<E, S>

/**
 * Represents the state of one or more pushed state maps and can be stored and used to initialise the state machine.
 */
typealias ExternalState<S> = StateMapList<S>

/**
 * Represents the different kinds of transitions.
 */
enum class TransitionType {
    /**
     * Transitions are triggered and may change to a new state or remain at the same state while performing an action.
     */
    NORMAL,

    /**
     * A push transition will place the current state map on a stack and make the named statemap the current map and change to the given state,
     */
    PUSH,

    /**
     * A pop transition will pop the stack and make the transition current. If the pop transition provided a new targetMap or targetState that will result in push or normal transition behaviour.
     */
    POP,

    /**
     * A default transition will take place when no configured state/event pair matches.
     */
    DEFAULT,

    /**
     * A timeout may trigger a transition if the timeout occurs. The timeout will be cancelled when the state exit action is triggered.
     *
     */
    TIMEOUT
}

class PreConditionException(message: String) : RuntimeException(message)
class PostConditionException(message: String) : RuntimeException(message)
class InvariantException(message: String) : RuntimeException(message)

@Throws(InvariantException::class)
inline fun invariant(condition: Boolean, errorMessage: () -> String) {
    contract { returns() implies condition }
    if (!condition) {
        throw InvariantException(errorMessage.invoke())
    }
}

@Throws(InvariantException::class)
inline fun <C> checkInvariant(context: C, condition: Pair<String, Condition<C>>?) {
    val check = condition?.second?.invoke(context) ?: true

    if (!check) throw InvariantException(condition.first)
}

typealias Condition<C> = C.() -> Boolean


@Throws(PreConditionException::class)
inline fun precondition(condition: Boolean, errorMessage: () -> String) {
    contract { returns() implies condition }
    if (!condition) {
        throw PreConditionException(errorMessage.invoke())
    }
}

@Throws(PostConditionException::class)
inline fun postcondition(condition: Boolean, errorMessage: () -> String) {
    contract { returns() implies condition }
    if (condition) {
        throw PostConditionException(errorMessage.invoke())
    }
}

@Throws(PreConditionException::class)
inline fun <C> checkPrecondition(context: C, condition: Pair<String, Condition<C>>?) {
    val check = condition?.second?.invoke(context) ?: true
    if (!check) throw PreConditionException(condition!!.first)
}

@Throws(PostConditionException::class)
inline fun <C> checkPostcondition(context: C, condition: Pair<String, Condition<C>>?) {
    val check = condition?.second?.invoke(context) ?: true
    if (!check) throw PostConditionException(condition!!.first)
}

/**
 * This class implements a simple generic stack.
 */
class Stack<T> {
    private val elements: MutableList<T> = mutableListOf()

    /**
     * Push an element onto the stack.
     * @param value The element will be pushed
     */
    fun push(value: T) = elements.add(value)

    /**
     * Returns and removed the element on the top of the stack.
     * @return The element on the top of the stack
     * @throws PreConditionException if the stack is empty
     */
    fun pop(): T {
        precondition(isNotEmpty()) { "stack is empty" }
        return elements.removeAt(elements.lastIndex)
    }

    /**
     * Indicates that the stack is empty.
     * @return true if the stack is empty
     */
    fun isEmpty() = elements.isEmpty()

    /**
     * Indicates that the stack is not empty.
     * @return true if the stack is not empty
     */
    fun isNotEmpty() = elements.isNotEmpty()

    /**
     * Provides access to the top of the stack.
     * @return the element at the top of the stack without removing or `null` is the stack is empty.
     */
    fun peek(): T? = elements.lastOrNull()

    /**
     * Provides access to all the entries on the stack.
     * @return An immutable list of entries with the top of the stack at the end.
     */
    fun peekContent(): List<T> = elements.toList()
}
