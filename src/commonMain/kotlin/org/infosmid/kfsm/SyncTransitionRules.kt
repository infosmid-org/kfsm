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
 * The transition rules for one state and one event. The rules have one transition and a list of guarded transitions.
 * @author Corneil du Plessis
 * @soundtrack Wolfgang Amadeus Mozart
 * @param guardedTransitions The list of guarded transitions. The state machine checks them in list order.
 * @param transition The transition to use when there are no guarded transitions, or when no guard returns `true`.
 */
class SyncTransitionRules<S, E, C, A, R>(
    val guardedTransitions: MutableList<SyncGuardedTransition<S, E, C, A, R>> = mutableListOf(),
    transition: SimpleSyncTransition<S, E, C, A, R>? = null
) {
    var transition: SimpleSyncTransition<S, E, C, A, R>? = transition
        internal set

    /**
     * Adds a guarded transition to the end of the list.
     */
    fun addGuarded(guardedTransition: SyncGuardedTransition<S, E, C, A, R>) {
        guardedTransitions.add(guardedTransition)
    }

    /**
     * Finds the first guarded transition where the guard returns `true`.
     * @param context The context that the guards check.
     * @param arg The event argument.
     * @return The first matching guarded transition, or `null` if no guard returns `true`.
     */
    fun findGuard(context: C, arg: A? = null): SyncGuardedTransition<S, E, C, A, R>? =
        guardedTransitions.firstOrNull { it.guardMet(context, arg) }
}
