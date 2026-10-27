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

import kotlin.reflect.KClass

/**
 * Starts the DSL declaration of a state machine where the actions are `suspend` functions.
 * @param validStates The set of states that the default state map allows.
 * @param validEvents The set of events of the state machine.
 * @param contextClass The class of the context.
 * @param argumentClass The class of the event argument.
 * @param returnClass The class of the return value of the actions.
 * @param handler The lambda that declares the state machine.
 * @return The DSL handler. Call `build()` on it to create the definition.
 */
inline fun <reified S, reified E, reified C : Any, reified A : Any, reified R : Any> asyncStateMachine(
    validStates: Set<S>,
    validEvents: Set<E>,
    contextClass: KClass<out C>,
    argumentClass: KClass<out A>,
    returnClass: KClass<out R>,
    handler: AsyncDslStateMachineHandler<S, E, C, A, R>.() -> Unit
) = AsyncStateMachineBuilder<S, E, C, A, R>(validStates, validEvents).stateMachine(handler)

/**
 * Starts the DSL declaration of a state machine where the actions are `suspend` functions. The return type of the actions is `Any`.
 * @param validStates The set of states that the default state map allows.
 * @param validEvents The set of events of the state machine.
 * @param contextClass The class of the context.
 * @param argumentClass The class of the event argument.
 * @param handler The lambda that declares the state machine.
 * @sample org.infosmid.kfsm.example.TurnstileFSM.Companion.definition
 */
inline fun <reified S, reified E, reified C : Any, reified A : Any> asyncStateMachine(
    validStates: Set<S>,
    validEvents: Set<E>,
    contextClass: KClass<out C>,
    argumentClass: KClass<out A>,
    handler: AsyncDslStateMachineHandler<S, E, C, A, Any>.() -> Unit
) = asyncStateMachine<S, E, C, A, Any>(validStates, validEvents, contextClass, argumentClass, Any::class, handler)

/**
 * Starts the DSL declaration of a state machine where the actions are `suspend` functions.
 * The type of the event argument and the return type of the actions are `Any`.
 * @param validStates The set of states that the default state map allows.
 * @param validEvents The set of events of the state machine.
 * @param contextClass The class of the context.
 * @param handler The lambda that declares the state machine.
 * @sample org.infosmid.kfsm.example.TurnstileFSM.Companion.definition
 */
inline fun <reified S, reified E, reified C : Any> asyncStateMachine(
    validStates: Set<S>,
    validEvents: Set<E>,
    contextClass: KClass<out C>,
    handler: AsyncDslStateMachineHandler<S, E, C, Any, Any>.() -> Unit
) = asyncStateMachine<S, E, C, Any, Any>(validStates, validEvents, contextClass, Any::class, Any::class, handler)

/**
 * Starts the DSL declaration of a state machine where the actions are `suspend` functions.
 * The context, the event argument and the return value have the same type.
 * @param validStates The set of states that the default state map allows.
 * @param validEvents The set of events of the state machine.
 * @param contextClass The class of the context.
 * @param handler The lambda that declares the state machine.
 */
inline fun <reified S, reified E, reified C : Any> asyncFunctionalStateMachine(
    validStates: Set<S>,
    validEvents: Set<E>,
    contextClass: KClass<out C>,
    handler: AsyncDslStateMachineHandler<S, E, C, C, C>.() -> Unit
) = AsyncStateMachineBuilder<S, E, C, C, C>(validStates, validEvents).stateMachine(handler)

/**
 * Runs `block` on the receiver when the expression is `true`. Otherwise it runs `otherwise` on the receiver.
 * @param expression The condition to check.
 * @param block The lambda to run when the expression is `true`.
 * @param otherwise The lambda to run when the expression is `false`.
 * @return The receiver.
 */
inline fun <T> T.ifApply(expression: Boolean, block: T.() -> Unit, otherwise: T.() -> Unit): T {
    return if (expression) {
        this.apply(block)
    } else {
        this.apply(otherwise)
    }
}
