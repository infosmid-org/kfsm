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
 * @author Corneil du Plessis
 * @soundtrack Wolfgang Amadeus Mozart
 * @suppress
 * Represents a transition from a given state and event.
 * @param startState The given state
 * @param event The given event
 * @param targetState when optional represents an internal transition
 * @param action An optional lambda that will be invoked.
 */
open class SimpleSyncTransition<S, E, C, A, R>(
    internal val startState: S,
    internal val event: E?,
    targetState: S?,
    targetMap: String?,
    automatic: Boolean,
    type: TransitionType,
    action: SyncStateAction<C, A, R>?
) : SyncTransition<S, E, C, A, R>(targetState, targetMap, automatic, type, action)
