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
 * A transition for an event that no other transition handles in the state map.
 * @param event The event that identifies the transition.
 * @param targetState The state after the transition. When it is `null`, the transition is internal.
 * @param targetMap The name of the state map for a push transition or a pop transition.
 * @param automatic `true` if the transition has no event.
 * @param type The kind of transition.
 * @param action An optional lambda. The state machine runs it when the transition occurs.
 */
class DefaultSyncTransition<S, E, C, A, R>(
    internal val event: E,
    targetState: S?,
    targetMap: String?,
    automatic: Boolean,
    type: TransitionType,
    action: SyncStateAction<C, A, R>?
) : SyncTransition<S, E, C, A, R>(targetState, targetMap, automatic, type, action)
