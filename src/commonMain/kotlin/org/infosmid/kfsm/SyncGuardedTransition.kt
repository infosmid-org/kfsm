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
 * Represents a guarded transition. The transition will be considered if the guard expression is true
 * @author Corneil du Plessis
 * @soundtrack Wolfgang Amadeus Mozart
 * @param startState The given state
 * @param event The given event
 * @param targetState when optional represents an internal transition
 * @param guard Expression lambda returning a Boolean
 * @param action An optional lambda that will be invoked.
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
     * This function will invoke the guard expression using the provided context to determine if transition can be considered.
     * @param context The provided context
     * @return result of guard lambda
     */
    fun guardMet(context: C, arg: A?): Boolean = guard.invoke(context, arg)
}
