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
 * The base for all transitions.
 * @author Corneil du Plessis
 * @soundtrack Wolfgang Amadeus Mozart
 * @param targetState when optional represents an internal transition
 * @param action optional lambda will be invoked when transition occurs.
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
            require(targetState != null) { "targetState is required for push transition" }
        }
    }

    /**
     * Executed exit, optional and entry actions specific in the transition.
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
     * Executed exit, optional and entry actions specific in the transition.
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
     * This function provides an indicator if a Transition is internal or external.
     * When there is no targetState defined a Transition is considered internal and will not trigger entry or exit actions.
     */
    fun isExternal(): Boolean = targetState != null
}
