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

class AsyncTransitionRules<S, E, C, A, R>(
    val guardedTransitions: MutableList<AsyncGuardedTransition<S, E, C, A, R>> = mutableListOf(),
    transition: SimpleAsyncTransition<S, E, C, A, R>? = null
) {
    var transition: SimpleAsyncTransition<S, E, C, A, R>? = transition
        internal set

    /**
     * Add a guarded transition to the end of the list
     */
    fun addGuarded(guardedTransition: AsyncGuardedTransition<S, E, C, A, R>) {
        guardedTransitions.add(guardedTransition)
    }

    /**
     * Find the first entry in the list of guarded transitions that match/
     * @param context The given context.
     */
    fun findGuard(context: C, arg: A? = null): AsyncGuardedTransition<S, E, C, A, R>? =
        guardedTransitions.firstOrNull { it.guardMet(context, arg) }
}
