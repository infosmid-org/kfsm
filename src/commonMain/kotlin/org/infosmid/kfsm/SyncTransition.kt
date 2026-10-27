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

import org.infosmid.dbc.preconditionNotNull

/**
 * @suppress
 * The base class for all transitions.
 * @author Corneil du Plessis
 * @soundtrack Wolfgang Amadeus Mozart
 * @param targetState The state after the transition. When it is `null`, the transition is internal.
 * @param targetMap The name of the state map for a push transition or a pop transition.
 * @param automatic `true` if the transition has no event.
 * @param type The kind of transition.
 * @param action An optional lambda. The state machine runs it when the transition occurs.
 */
open class SyncTransition<S, E, C, A, R>(
    val targetState: S? = null,
    val targetMap: String? = null,
    val automatic: Boolean = false,
    val type: TransitionType = TransitionType.NORMAL,
    val action: SyncStateAction<C, A, R>? = null
) {
    init {
        if (type == TransitionType.PUSH) {
            preconditionNotNull(targetState) { "A push transition requires a target state" }
        }
    }

    /**
     * Runs the exit action, the transition action and the entry action in the same state map.
     * An internal transition does not run the exit action or the entry action.
     * @return The result of the transition action, or `null` if there is no action.
     */
    open fun execute(context: C, instance: StateMapInstance<S, E, C, A, R>, arg: A?): R? {

        if (isExternal()) {
            instance.executeExit(context, targetState!!, arg)
        }
        val result = action?.invoke(context, arg)
        if (isExternal()) {
            instance.executeEntry(context, targetState!!, arg)
        }
        return result
    }

    /**
     * Runs the exit action, the transition action and the entry action across two state maps.
     * The exit action runs in the source map. The entry action runs in the target map.
     * An internal transition does not run the exit action or the entry action.
     * @return The result of the transition action, or `null` if there is no action.
     */
    open fun execute(
        context: C,
        sourceMap: StateMapInstance<S, E, C, A, R>,
        targetMap: StateMapInstance<S, E, C, A, R>?,
        arg: A?
    ): R? {

        if (isExternal()) {
            sourceMap.executeExit(context, targetState!!, arg)
        }
        val result = action?.invoke(context, arg)
        if (targetMap != null && isExternal()) {
            targetMap.executeEntry(context, targetState!!, arg)
        }
        return result
    }

    /**
     * Checks if the transition is external.
     * A transition without a target state is internal. An internal transition does not run entry actions or exit actions.
     * @return `true` if the transition has a target state.
     */
    fun isExternal(): Boolean = targetState != null
}
