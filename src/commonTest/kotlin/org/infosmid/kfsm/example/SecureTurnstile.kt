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

import org.infosmid.kfsm.stateMachine

// tag::states-events[]
enum class SecureTurnstileEvents {
    CARD,
    PASS
}
// end::states-events[]

enum class SecureTurnstileStates {
    LOCKED,
    OVERRIDE,
    UNLOCKED
}

// tag::context[]
class SecureTurnstile {
    var locked: Boolean = true
        private set
    var overrideActive: Boolean = false
        private set

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
class SecureTurnstileFSM(secureTurnstile: SecureTurnstile) {
    companion object {
        val definition = stateMachine(
            SecureTurnstileStates.entries.toSet(),
            SecureTurnstileEvents.entries.toSet(),
            SecureTurnstile::class,
            Int::class
        ) {
            defaultInitialState = SecureTurnstileStates.LOCKED
            initialState { if (locked) SecureTurnstileStates.LOCKED else SecureTurnstileStates.UNLOCKED }
            default {
                action { _, _, _ ->
                    buzzer()
                }
            }
            onStateChange { oldState, newState ->
                println("onStateChange:$oldState -> $newState")
            }
            whenState(SecureTurnstileStates.OVERRIDE) {
                onEvent(
                    SecureTurnstileEvents.CARD to SecureTurnstileStates.LOCKED,
                    guard = { cardId ->
                        requireNotNull(cardId)
                        isOverrideCard(cardId)
                    }
                ) {
                    cancelOverride()
                }
                onEvent(
                    SecureTurnstileEvents.CARD to SecureTurnstileStates.UNLOCKED,
                    guard = { cardId ->
                        requireNotNull(cardId)
                        !isValidCard(cardId)
                    }
                ) { cardId ->
                    println("Allowed invalid card:$cardId")
                    unlock()
                }
                onEvent(
                    SecureTurnstileEvents.CARD to SecureTurnstileStates.UNLOCKED,
                    guard = { cardId ->
                        requireNotNull(cardId)
                        isValidCard(cardId)
                    }
                ) { cardId ->
                    println("Allowing valid card:${cardId}")
                    unlock()
                }
            }
            whenState(SecureTurnstileStates.LOCKED) {
                onEvent(
                    SecureTurnstileEvents.CARD to SecureTurnstileStates.OVERRIDE,
                    guard = { cardId ->
                        requireNotNull(cardId)
                        isOverrideCard(cardId)
                    }
                ) {
                    activateOverride()
                }
                onEvent(
                    SecureTurnstileEvents.CARD to SecureTurnstileStates.UNLOCKED,
                    guard = { cardId ->
                        requireNotNull(cardId)
                        isValidCard(cardId)
                    }
                ) {
                    unlock()
                }
                onEvent(
                    SecureTurnstileEvents.CARD,
                    guard = { cardId ->
                        requireNotNull(cardId) { "cardId is required" }
                        !isValidCard(cardId)
                    }
                ) { cardId ->
                    requireNotNull(cardId)
                    invalidCard(cardId)
                }
            }
            whenState(SecureTurnstileStates.UNLOCKED) {
                onEvent(
                    SecureTurnstileEvents.CARD to SecureTurnstileStates.LOCKED,
                    guard = { cardId ->
                        requireNotNull(cardId)
                        isOverrideCard(cardId)
                    }
                ) {
                    lock()
                }
                onEvent(SecureTurnstileEvents.PASS to SecureTurnstileStates.LOCKED) {
                    lock()
                }
            }
        }.build()
    }

    private val fsm = definition.create(secureTurnstile)
    fun card(cardId: Int) = fsm.sendEvent(SecureTurnstileEvents.CARD, cardId)
    fun pass() = fsm.sendEvent(SecureTurnstileEvents.PASS)
    fun allowEvent(): Set<String> = fsm.allowed().map { it.name.lowercase() }.toSet()
}
// end::fsm[]
