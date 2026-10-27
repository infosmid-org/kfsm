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
 * @suppress
 * A transition with a guard expression. The state machine uses the transition only when the guard returns `true`.
 * @author Corneil du Plessis
 * @soundtrack Wolfgang Amadeus Mozart
 * @param startState The state where the transition starts.
 * @param event The event that triggers the transition. It is `null` for an automatic transition.
 * @param targetState The state after the transition. When it is `null`, the transition is internal.
 * @param targetMap The name of the state map for a push transition or a pop transition.
 * @param automatic `true` if the transition has no event.
 * @param type The kind of transition.
 * @param guard A lambda that returns a `Boolean`.
 * @param action An optional lambda. The state machine runs it when the transition occurs.
 */
open class SyncGuardedTransition<S, E, C, A, R>(
    startState: S,
    event: E?,
    targetState: S?,
    targetMap: String?,
    automatic: Boolean,
    type: TransitionType,
    val guard: StateGuard<C, A>,
    action: SyncStateAction<C, A, R>?
) : SimpleSyncTransition<S, E, C, A, R>(startState, event, targetState, targetMap, automatic, type, action) {
    /**
     * Runs the guard expression to find out if the transition can be used.
     * @param context The context that the guard checks.
     * @param arg The event argument.
     * @return The result of the guard lambda.
     */
    fun guardMet(context: C, arg: A?): Boolean = guard.invoke(context, arg)
}
