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
package org.infosmid.kfsm.example

import org.infosmid.kfsm.async.asyncStateMachine
import kotlinx.coroutines.CoroutineScope

// tag::states-events[]
enum class TimeoutSecureTurnstileEvents {
    CARD,
    PASS
}
// end::states-events[]

enum class TimeoutSecureTurnstileStates {
    LOCKED,
    UNLOCKED
}

// tag::context[]
class TimerSecureTurnstile {
    var locked: Boolean = true
        private set
    var overrideActive: Boolean = false
        private set
    val timeout = 500L
    fun activateOverride() {
        overrideActive = true
        println("override activated")
    }

    fun cancelOverride() {
        overrideActive = false
        println("override canceled")
    }

    fun lock() {
        println("lock")
        locked = true
        overrideActive = false
    }

    fun unlock() {
        println("unlock")
        locked = false
        overrideActive = false
    }

    fun buzzer() {
        println("BUZZER")
    }

    fun invalidCard(cardId: Int) {
        println("Invalid card $cardId")
    }

    fun isOverrideCard(cardId: Int): Boolean {
        return cardId == 42
    }

    fun isValidCard(cardId: Int): Boolean {
        return cardId % 2 == 1
    }
}
// end::context[]

// tag::fsm[]
class TimerSecureTurnstileFSM(secureTurnstile: TimerSecureTurnstile, coroutineScope: CoroutineScope) {
    companion object {
        val definition = asyncStateMachine(
            TimeoutSecureTurnstileStates.entries.toSet(),
            TimeoutSecureTurnstileEvents.entries.toSet(),
            TimerSecureTurnstile::class,
            Int::class
        ) {
            defaultInitialState = TimeoutSecureTurnstileStates.LOCKED
            initialState { if (locked) TimeoutSecureTurnstileStates.LOCKED else TimeoutSecureTurnstileStates.UNLOCKED }
            default {
                action { _, _, _ ->
                    buzzer()
                }
            }
            onStateChange { oldState, newState ->
                println("onStateChange:$oldState -> $newState")
            }
            whenState(TimeoutSecureTurnstileStates.LOCKED) {
                onEvent(
                    TimeoutSecureTurnstileEvents.CARD,
                    guard = { cardId ->
                        requireNotNull(cardId)
                        isOverrideCard(cardId) && overrideActive
                    }
                ) {
                    cancelOverride()
                }
                onEvent(
                    TimeoutSecureTurnstileEvents.CARD,
                    guard = { cardId ->
                        requireNotNull(cardId)
                        isOverrideCard(cardId)
                    }
                ) {
                    activateOverride()
                }
                onEvent(
                    TimeoutSecureTurnstileEvents.CARD to TimeoutSecureTurnstileStates.UNLOCKED,
                    guard = { cardId ->
                        requireNotNull(cardId)
                        overrideActive || isValidCard(cardId)
                    }
                ) {
                    unlock()
                }
                onEvent(
                    TimeoutSecureTurnstileEvents.CARD,
                    guard = { cardId ->
                        requireNotNull(cardId) { "cardId is required" }
                        !isValidCard(cardId)
                    }
                ) { cardId ->
                    requireNotNull(cardId)
                    invalidCard(cardId)
                }
            }
            whenState(TimeoutSecureTurnstileStates.UNLOCKED) {
                timeout(TimeoutSecureTurnstileStates.LOCKED, { timeout }) {
                    println("Timeout. Locking")
                    lock()
                }
                onEvent(
                    TimeoutSecureTurnstileEvents.CARD to TimeoutSecureTurnstileStates.LOCKED,
                    guard = { cardId ->
                        requireNotNull(cardId)
                        isOverrideCard(cardId)
                    }
                ) {
                    lock()
                }
                onEvent(TimeoutSecureTurnstileEvents.PASS to TimeoutSecureTurnstileStates.LOCKED) {
                    lock()
                }
            }
        }.build()
    }

    private val fsm = definition.create(secureTurnstile, coroutineScope)

    suspend fun card(cardId: Int) = fsm.sendEvent(TimeoutSecureTurnstileEvents.CARD, cardId)
    suspend fun pass() = fsm.sendEvent(TimeoutSecureTurnstileEvents.PASS)
    fun allowEvent(): Set<String> = fsm.allowed().map { it.name.lowercase() }.toSet()
}
// end::fsm[]
