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
 * Defines the start of a state machine DSL declaration
 * @author Corneil du Plessis
 * @soundtrack Wolfgang Amadeus Mozart
 * @param validStates A set of the possible states supported by the top-level state map
 * @param validEvents The class of the possible events
 * @param contextClass The class of the context
 * @param argumentClass The class of the argument to events/actions
 * @param returnClass The class of the return type of events/actions
 * @param handler Statemachine handler
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
 * Defines the start of a state machine DSL declaration with `Any` as the return type
 * @param validStates A set of the possible states supported by the top-level state map
 * @param eventClass The class of the possible events
 * @param contextClass The class of the context
 * @param argumentClass The class of the argument to events/actions
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
 * Defines the start of a state machine DSL declaration with `Any` as the type of arguments and returns types for events/actions
 * @param validStates A set of the possible states supported by the top-level state map
 * @param validEvents The class of the possible events
 * @param contextClass The class of the context
 * @param handler The DSL handler
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

inline fun <reified S, reified E, reified C : Any> functionalStateMachine(
    validStates: Set<S>,
    validEvents: Set<E>,
    contextClass: KClass<out C>,
    handler: DslStateMachineHandler<S, E, C, C, C>.() -> Unit
) = StateMachineBuilder<S, E, C, C, C>(validStates, validEvents).stateMachine(handler)
