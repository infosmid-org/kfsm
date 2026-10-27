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

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.ExperimentalAtomicApi

/**
 * @suppress
 * A timer for a timeout transition.
 * The timer starts when it is created. After the delay, it runs the transition if the timer is still active.
 * The timer uses the `CoroutineScope` to run the delay.
 * @param parentFsm The state map instance that owns the timer.
 * @param context The context that the guards and actions operate on.
 * @param arg The event argument of the transition that entered the state.
 * @param definition The definition of the timer. It has the delay and the transition rules.
 * @param coroutineScope The scope that runs the delay.
 */
@OptIn(ExperimentalAtomicApi::class)
class AsyncTimer<S, E, C, A, R> constructor(
    private val parentFsm: AsyncStateMapInstance<S, E, C, A, R>,
    val context: C,
    val arg: A?,
    val definition: AsyncTimerDefinition<S, E, C, A, R>,
    coroutineScope: CoroutineScope
) {
    private val active = AtomicBoolean(false)
    private val timer: Job

    init {
        active.store(true)
        timer = coroutineScope.async {
            delay(context.(definition.timeout)())
            trigger()
        }
    }

    /**
     * Cancels the timer. The timer does not run the transition after it is cancelled.
     */
    fun cancel() {
        active.store(false)
    }

    /**
     * Runs the transition of the timeout when the timer is active.
     * It uses the first guarded transition where the guard returns `true`. Otherwise it uses the transition without a guard.
     */
    suspend fun trigger() {
        if (active.load()) {
            val defaultTransition = definition.rule.transition
            definition.rule.findGuard(context, arg)?.apply {
                parentFsm.execute(this, arg)
            } ?: run {
                if (defaultTransition != null) {
                    parentFsm.execute(defaultTransition, arg)
                }
            }
        }
    }
}
