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

import kotlin.reflect.KClass

/**
 * Starts the DSL declaration of a state machine.
 * @author Corneil du Plessis
 * @soundtrack Wolfgang Amadeus Mozart
 * @param validStates The set of states that the default state map allows.
 * @param validEvents The set of events of the state machine.
 * @param contextClass The class of the context.
 * @param argumentClass The class of the event argument.
 * @param returnClass The class of the return value of the actions.
 * @param handler The lambda that declares the state machine.
 * @return The DSL handler. Call `build()` on it to create the definition.
 * @sample org.infosmid.kfsm.example.TurnstileFSM.Companion.definition
 */
inline fun <reified S, reified E, reified C : Any, reified A : Any, reified R : Any> stateMachine(
    validStates: Set<S>,
    validEvents: Set<E>,
    contextClass: KClass<out C>,
    argumentClass: KClass<out A>,
    returnClass: KClass<out R>,
    handler: DslStateMachineHandler<S, E, C, A, R>.() -> Unit
) = StateMachineBuilder<S, E, C, A, R>(validStates, validEvents).stateMachine(handler)

/**
 * Starts the DSL declaration of a state machine. The return type of the actions is `Any`.
 * @param validStates The set of states that the default state map allows.
 * @param validEvents The set of events of the state machine.
 * @param contextClass The class of the context.
 * @param argumentClass The class of the event argument.
 * @param handler The lambda that declares the state machine.
 * @sample org.infosmid.kfsm.example.TurnstileFSM.Companion.definition
 */
inline fun <reified S, reified E, reified C : Any, reified A : Any> stateMachine(
    validStates: Set<S>,
    validEvents: Set<E>,
    contextClass: KClass<out C>,
    argumentClass: KClass<out A>,
    handler: DslStateMachineHandler<S, E, C, A, Any>.() -> Unit
) = stateMachine<S, E, C, A, Any>(
    validStates,
    validEvents,
    contextClass,
    argumentClass,
    Any::class,
    handler
)

/**
 * Starts the DSL declaration of a state machine. The type of the event argument and the return type of the actions are `Any`.
 * @param validStates The set of states that the default state map allows.
 * @param validEvents The set of events of the state machine.
 * @param contextClass The class of the context.
 * @param handler The lambda that declares the state machine.
 * @sample org.infosmid.kfsm.example.TurnstileFSM.definition
 */
inline fun <reified S, reified E, reified C : Any> stateMachine(
    validStates: Set<S>,
    validEvents: Set<E>,
    contextClass: KClass<out C>,
    handler: DslStateMachineHandler<S, E, C, Any, Any>.() -> Unit
) = stateMachine<S, E, C, Any, Any>(
    validStates,
    validEvents,
    contextClass,
    Any::class,
    Any::class,
    handler
)

/**
 * Starts the DSL declaration of a state machine where the context, the event argument and the return value have the same type.
 * Use it with an immutable context. An action returns the new context.
 * @param validStates The set of states that the default state map allows.
 * @param validEvents The set of events of the state machine.
 * @param contextClass The class of the context.
 * @param handler The lambda that declares the state machine.
 */
inline fun <reified S, reified E, reified C : Any> functionalStateMachine(
    validStates: Set<S>,
    validEvents: Set<E>,
    contextClass: KClass<out C>,
    handler: DslStateMachineHandler<S, E, C, C, C>.() -> Unit
) = StateMachineBuilder<S, E, C, C, C>(validStates, validEvents).stateMachine(handler)
